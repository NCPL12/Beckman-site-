package ncpl.bms.reports.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayInputStream;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.model.dto.AlarmRecordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AlarmReportService {

    // Global reviewer constant (can be overridden by method parameter)
    private static final String DEFAULT_REVIEWER = "System Administrator";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AuditReportService auditReportService;

    public List<AlarmRecordDTO> fetchAlarmLogs(long startMillis, long endMillis) {
        String sql = "SELECT " +
                "r.[timestamp],"+
                "r.[ackState], " +
                "r.[alarmClass], " +
                "r.[normalTime], " +
                "r.[ackTime], " +
                "s.[source], " +
                "c.[timeOfLastAlarm] " +
                "FROM [dbo].[alarmOrion_OrionAlarmRecord] r " +
                "LEFT JOIN [dbo].[alarmOrion_OrionAlarmSourceOrder] o ON r.[id] = o.[id] " +
                "LEFT JOIN [dbo].[alarmOrion_OrionAlarmSource] s ON o.[alarmSource] = s.[id] " +
                "LEFT JOIN [dbo].[alarmOrion_OrionAlarmClass] c ON r.[alarmClass] = c.[id] " +
                "WHERE r.[timestamp] BETWEEN ? AND ? " +
                "ORDER BY r.[timestamp] ASC";

        return jdbcTemplate.query(sql, new Object[]{startMillis, endMillis}, (ResultSet rs, int rowNum) -> {
            AlarmRecordDTO dto = new AlarmRecordDTO();
            dto.setTimestamp(rs.getLong("timestamp"));
            dto.setSource(extractSourceName(rs.getString("source")));
            dto.setTimeOfLastAlarm(rs.getLong("timeOfLastAlarm"));
            dto.setAckState(rs.getLong("ackState"));
            dto.setAlarmClass(rs.getLong("alarmClass"));
            dto.setNormalTime(rs.getLong("normalTime"));
            dto.setAckTime(rs.getLong("ackTime"));
            dto.setMessageText(extractMessageText(rs.getString("source")));
            return dto;
        });
    }

    private String extractSourceName(String fullSource) {
        if (fullSource == null) return "";
        Pattern pattern = Pattern.compile("points/([^/]+)");
        Matcher matcher = pattern.matcher(fullSource);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractMessageText(String fullSource)    {
        if (fullSource == null) return "";

        int index = fullSource.indexOf("points/");
        if (index == -1) return "";

        String afterPoints = fullSource.substring(index + "points/".length());
        String[] segments = afterPoints.split("/");

        if (segments.length == 0) {
            return "";
        }

        // If second last is HUMIDITY → return HUMIDITY + lastSegment
        if (segments.length >= 2 && "HUMIDITY".equalsIgnoreCase(segments[segments.length - 2])) {
            return segments[segments.length - 2] + segments[segments.length - 1];
        }

        // Else return only last segment
        return segments[segments.length - 1];
    }

    private String formatEpoch(Long millis) {
        try {
            return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date(millis));
        } catch (Exception e) {
            return String.valueOf(millis);
        }
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setMinimumHeight(30f);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    private PdfPCell createLeftAlignedCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setMinimumHeight(30f);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    private PdfPCell createRightAlignedCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setMinimumHeight(30f);
        cell.setBorder(Rectangle.BOX);
        return cell;
    }

    private String getAlarmClassLabel(Long alarmClass) {
        if (alarmClass == null) return "Unknown";
        switch (alarmClass.intValue()) {
            case 0: return "Normal";
            case 1: return "Critical";
            case 2: return "Default";
            default: return "Unknown";
        }
    }

    private String getAckClassLabel(Long ackClass) {
        if (ackClass == null) return "Unknown";
        switch (ackClass.intValue()) {
            case 0: return "Ack";
            case 1: return "Unacked";
            default: return "Unknown";
        }
    }
    public void reviewAlarmReport(int reportId, String reviewer) throws Exception {
        String effectiveReviewer = (reviewer != null && !reviewer.trim().isEmpty()) ? reviewer : "Supervisor";

        byte[] originalPdf = getStoredAlarmReportById(reportId);
        if (originalPdf == null) throw new Exception("Report not found");

        PdfReader reader = new PdfReader(new ByteArrayInputStream(originalPdf));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, outputStream);

        String formattedDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
        String reviewText = "Reviewed By: " + effectiveReviewer;
        String generatedText = "Generated on: " + formattedDate;

        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        Font font = new Font(baseFont, 9);

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            PdfContentByte canvas = stamper.getOverContent(i);
            Rectangle pageSize = reader.getPageSize(i);

            float x = pageSize.getRight() + 200f; // Right margin (approx 36pt)
            float y = pageSize.getBottom() + 40f; // A bit above the bottom

            // "Reviewed By" line
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(reviewText, font), x, y, 0);

            // "Generated on" line just below the "Reviewed By"
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(generatedText, font), x, y - 12f, 0);
        }

        stamper.close();
        reader.close();

        byte[] updatedPdf = outputStream.toByteArray();
        long reviewTime = System.currentTimeMillis();

        String sql = "UPDATE StoredAlarmReport SET report_data = ?, reviewed_by = ?, review_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, updatedPdf, effectiveReviewer, reviewTime, reportId);

        log.info("Alarm report reviewed by {} at {}", effectiveReviewer, formattedDate);
    }

    private PdfPTable createAlarmTable(Font headerFont) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5f, 5f, 3f, 5f, 3f, 5f});
        table.setHeaderRows(1);

        String[] headers = {"Timestamp","Source Name", "Ack State", "Message text", "Alarm Class", "Alarm Time"};
        Font whiteFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE); // Text color: white

        for (String h : headers) {
            PdfPCell header = new PdfPCell(new Phrase(h, whiteFont));
            header.setBackgroundColor(new Color(0, 123, 128)); // Teal background
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setPadding(5f);
            table.addCell(header);
        }

        return table;
    }

    public byte[] getStoredAlarmReportById(int reportId) {
        try {
            String sql = "SELECT report_data FROM StoredAlarmReport WHERE id = ?";

            return jdbcTemplate.queryForObject(sql, new Object[]{reportId}, byte[].class);
        } catch (Exception e) {
            log.error("Error fetching stored alarm report from database", e);
            return null;
        }
    }

    public void saveAlarmReportToDatabase(byte[] pdfBytes, String username) {
        try {
            String sql = "INSERT INTO StoredAlarmReport (report_name, generated_on, report_data, generated_by) VALUES (?, ?, ?, ?)";

            String reportName = "Alarm_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Date now = new Date();

            jdbcTemplate.update(sql, reportName, new java.sql.Timestamp(now.getTime()), pdfBytes, username);

            log.info("Alarm report saved by '{}' into StoredAlarmReport table.", username);
        } catch (Exception e) {
            log.error("Error saving alarm report to database", e);
        }
    }

    public byte[] generateAlarmReportPdf(long startMillis, long endMillis, String username) {
        List<AlarmRecordDTO> logs = fetchAlarmLogs(startMillis, endMillis);

        if (logs == null || logs.isEmpty()) {
            throw new RuntimeException("No alarm records found in the selected time range.");
        }


        System.out.println("Generating alarm report for user: " + username);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 110, 60);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            writer.setPageEvent(new PdfPageEventHelper() {
                Font titleFontBold = new Font(Font.HELVETICA, 18, Font.BOLD);
                Font titleFont = new Font(Font.HELVETICA, 12);
                Font footerFont = new Font(Font.HELVETICA, 9);
                Image logo;

                {
                    try {
                        logo = Image.getInstance(new ClassPathResource("static/images/logo1.png").getInputStream().readAllBytes());
                        logo.scaleToFit(90, 40);
                    } catch (IOException e) {
                        log.error("Logo load error", e);
                        logo = null;
                    }
                }

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        PdfContentByte cb = writer.getDirectContent();

                        // Header table with logo, title, and empty right cell
                        PdfPTable headerTable = new PdfPTable(3);
                        headerTable.setWidths(new float[]{2f, 6f, 2f});
                        headerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                        headerTable.setLockedWidth(true);

                        PdfPCell logoCell = new PdfPCell();
                        logoCell.setBorder(Rectangle.NO_BORDER);
                        if (logo != null) {
                            logoCell.addElement(logo);
                        }
                        headerTable.addCell(logoCell);

                        PdfPCell titleCell = new PdfPCell(new Phrase("Alarm Report of EMS System", titleFontBold));
                        titleCell.setBorder(Rectangle.NO_BORDER);
                        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        headerTable.addCell(titleCell);

                        PdfPCell emptyCell = new PdfPCell(new Phrase(""));
                        emptyCell.setBorder(Rectangle.NO_BORDER);
                        headerTable.addCell(emptyCell);

                        headerTable.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 20, cb);

                        // Metadata table (Start/End Date & Time)
                        PdfPTable metaTable = new PdfPTable(2);
                        metaTable.setWidths(new float[]{1, 1});
                        metaTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                        metaTable.setLockedWidth(true);

                        Font infoFont = new Font(Font.HELVETICA, 9);
                        String startDate = formatEpoch(startMillis);
                        String endDate = formatEpoch(endMillis);

                        PdfPCell startCell = createLeftAlignedCell(
                                "Start Date: " + startDate.split(" ")[0] + "\nStart Time: " + startDate.split(" ")[1], infoFont);
                        PdfPCell endCell = createRightAlignedCell(
                                "End Date: " + endDate.split(" ")[0] + "\nEnd Time: " + endDate.split(" ")[1], infoFont);

                        startCell.setBorder(Rectangle.NO_BORDER);
                        endCell.setBorder(Rectangle.NO_BORDER);

                        metaTable.addCell(startCell);
                        metaTable.addCell(endCell);

                        metaTable.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 60, cb);

                        // Footer
                        String generatedBy = "Generated By: " + username;
                        String generatedOn = "Generated on: " + new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());

                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase(generatedBy, footerFont),
                                document.leftMargin(), 40, 0);

                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase(generatedOn, footerFont),
                                document.leftMargin(), 30, 0);

                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Page " + writer.getPageNumber(), footerFont),
                                (document.right() + document.left()) / 2, 30, 0);

                    } catch (Exception e) {
                        log.error("Error in header/footer rendering", e);
                    }
                }
            });

            document.open();
            Font infoFont = new Font(Font.HELVETICA, 9);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font cellFont = new Font(Font.HELVETICA, 9);

            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1, 1});
            metaTable.setSpacingAfter(10);
            document.add(metaTable);

            PdfPTable table = createAlarmTable(headerFont);

            for (AlarmRecordDTO log : logs) {
                table.addCell(createCell(formatEpoch(log.getTimestamp()), cellFont));
//                table.addCell(createCell(formatEpoch(log.getNormalTime()), cellFont));
                table.addCell(createCell(log.getSource(), cellFont));
                table.addCell(createCell(getAckClassLabel(log.getAckState()), cellFont));
                table.addCell(createCell(log.getMessageText(), cellFont)); // Corrected line
                table.addCell(createCell(getAlarmClassLabel(log.getAlarmClass()), cellFont));
                table.addCell(createCell(formatEpoch(log.getTimeOfLastAlarm()), cellFont));
            }

            document.add(table);

            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(20);

            document.add(footerTable);

            document.close();
            auditReportService.logAlarmReportGeneration(username);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("PDF generation failed", e);
            return null;
        }
    }

}