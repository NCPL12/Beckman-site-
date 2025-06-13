package ncpl.bms.reports.service;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfContentByte;
import org.springframework.core.io.ClassPathResource;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AuditReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AuditReportService.class);

    // Log user login action
    public void logUserLogin(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "login";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log user logout action
    public void logUserLogout(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "logout";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log user auto logout action
    public void logUserAutoLogout(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "auto-logout";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log template creation
    public void logTemplateCreation(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "created-template";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log template deletion
    public void logTemplateDeletion(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "deleted-template";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log template editing
    public void logTemplateEditing(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "edited-template";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log report generation
    public void logReportGeneration(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "generated-report-manual";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log report review
    public void logReviewReport(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "reviewed-report";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }


    // Log report approval
    public void logReportApproval(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "approved-report";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log download of login report
    public void logDownloadLoginReport(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "downloaded-log-report";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log download of login report
    public void logDownloadReport(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "downloaded-report";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log download of audit report
    public void logDownloadAuditReport(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "downloaded-audit-report";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log scheduling of daily report
    public void logScheduledDailyReport(String username, String reportType) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "report-scheduled-daily";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log scheduling of weekly report
    public void logScheduledWeeklyReport(String username, String reportType) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "report-scheduled-weekly";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log scheduling of monthly report
    public void logScheduledMonthlyReport(String username, String reportType) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "report-scheduled-monthly";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Get all audit reports
    public List<Map<String, Object>> getAllAuditReports() {
        String sql = "SELECT * FROM audit_report ORDER BY timestamp DESC";
        return jdbcTemplate.queryForList(sql);
    }
    public ByteArrayInputStream generateAuditReportPdf(String fromDateStr, String toDateStr) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new AuditFooterEvent());
            document.open();
// === Header Section (Title + Logo) ===
            PdfPTable headerTop = new PdfPTable(2);
            headerTop.setWidthPercentage(100);
            headerTop.setWidths(new float[]{80f, 20f}); // 80% title + 20% logo

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

// Title cell (left)
            PdfPCell titleCell = new PdfPCell(new Phrase("Audit Report", titleFont));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            headerTop.addCell(titleCell);

// Logo cell (right)
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                Image logo = Image.getInstance(new ClassPathResource("static/images/logo1.png").getURL());
                logo.scaleToFit(80, 50);
                logo.setAlignment(Image.ALIGN_RIGHT);
                logoCell.addElement(logo);
            } catch (Exception e) {
                logoCell.addElement(new Phrase("Logo"));
            }
            headerTop.addCell(logoCell);

            document.add(headerTop);

// === Date Line (Centered) ===
            PdfPTable dateTable = new PdfPTable(1);
            dateTable.setWidthPercentage(100);

            String dateText = "From " + fromDateStr + " To " + toDateStr;
            PdfPCell dateCell = new PdfPCell(new Phrase(dateText, dateFont));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            dateCell.setPaddingBottom(10f); // add some space before table

            dateTable.addCell(dateCell);
            document.add(dateTable);

            // === Spacer ===
            document.add(new Paragraph(" ")); // One line of spacing

            // === Table with audit data ===
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100f);
            table.setSpacingBefore(10f);
            table.setWidths(new float[]{30f, 35f, 35f});

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(new Color(0, 123, 128));
            headerCell.setPadding(5);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            headerCell.setPhrase(new Phrase("Timestamp", headerFont)); table.addCell(headerCell);
            headerCell.setPhrase(new Phrase("Username", headerFont));  table.addCell(headerCell);
            headerCell.setPhrase(new Phrase("Activity", headerFont));    table.addCell(headerCell);

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            long fromMillis = convertToMillis(fromDateStr);
            long toMillis = convertToMillis(toDateStr);
            String sql = "SELECT * FROM audit_report WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
            List<Map<String, Object>> auditLogs = jdbcTemplate.queryForList(sql, fromMillis, toMillis);

            for (Map<String, Object> log : auditLogs) {
                Long timestampMillis = (Long) log.get("timestamp");
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        new java.util.Date(timestampMillis).toInstant(),
                        ZoneId.systemDefault()
                );
                String formattedDate = dateTime.format(formatter);

                PdfPCell cell1 = new PdfPCell(new Phrase(formattedDate, cellFont));
                PdfPCell cell2 = new PdfPCell(new Phrase((String) log.get("username"), cellFont));
                PdfPCell cell3 = new PdfPCell(new Phrase((String) log.get("action"), cellFont));

                for (PdfPCell cell : List.of(cell1, cell2, cell3)) {
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(5f);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            log.error("❌ Error generating audit report PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private static class AuditFooterEvent extends PdfPageEventHelper {
        private PdfTemplate totalPageTemplate;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPageTemplate = writer.getDirectContent().createTemplate(50, 50);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPageTemplate.beginText();
            totalPageTemplate.setFontAndSize(baseFont, 10);
            totalPageTemplate.setTextMatrix(0, 0);
            totalPageTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPageTemplate.endText();
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.beginText();
            cb.setFontAndSize(baseFont, 10);

            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"));
            int pageNumber = writer.getPageNumber();

            cb.setTextMatrix(document.right() - 120, document.bottom() - 10 + 20);
//            cb.showText("Printed On: " + nowStr);

//            String pageText = "Page No: " + pageNumber + " of ";
            cb.setTextMatrix(document.right() - 120, document.bottom() - 10 - 4);
//            cb.showText(pageText);

            cb.endText();
//            cb.addTemplate(totalPageTemplate, document.right() - 120 + baseFont.getWidthPoint(pageText, 10), document.bottom() - 14);
        }
    }



    private long convertToMillis(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date", e);
            return 0; // Handle error appropriately
        }
    }

    public void logAlarmReportGeneration(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "generated-alarm-report";

        String sql = "INSERT INTO audit_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }
}
