package ncpl.bms.reports.service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.model.dao.ReportTemplate;
import ncpl.bms.reports.model.dto.ReportDTO;
import ncpl.bms.reports.util.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

//--------------------COMPLETE FILE IS WRITTEN BY VISHAL----------------------//

@Component
@Slf4j
public class WeeklySchedulingService {
    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private ReportTemplateService templateService;

    @Value("${report.address}")
    private String address;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${report.heading}")
    private String heading;

    @Autowired
    private DateConverter dateConverter;

    private List<Map<String, Object>> reportDataList = null;

    public void generatePdfWeeklySchedule(Long templateId, String fromDateTime, String toDate, String username,  String assignedTo, String assigned_approver) throws Exception {
        reportDataList = reportDataService.generateReportData(templateId, fromDateTime, toDate);

        // Convert the date range to 'dd-MM-yyyy HH:mm:ss' format
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedFromDateTime = dateTimeFormatter.format(new Date(Long.parseLong(fromDateTime)));
        String formattedToDateTime = dateTimeFormatter.format(new Date(Long.parseLong(toDate)));

//        Map<String, Map<String, Integer>> statistics = reportDataService.calculateStatistics(templateId, fromDateTime, toDate);
        Document document = new Document(PageSize.A4.rotate());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);

        // Use TablePageEvent if required for header/footer
        WeeklySchedulingService.TablePageEvent event = new WeeklySchedulingService.TablePageEvent(formattedFromDateTime, formattedToDateTime, username);
        writer.setPageEvent(event);

        document.open();

        Map<String, Object> stringObjectMap = reportDataList.get(0);
        PdfPTable table = new PdfPTable(stringObjectMap.size());
        table.setWidthPercentage(100f);
        table.setSpacingBefore(5);

        PdfPCell cell = new PdfPCell();
        addTableHeader(stringObjectMap, table, cell);

        // Extract "From" and "To" values for each parameter
        Map<String, double[]> parameterRanges = extractParameterRanges(templateId);

        for (Map<String, Object> map : reportDataList) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String valueStr = String.valueOf(entry.getValue());
                if (valueStr == null || valueStr.trim().isEmpty() || valueStr.equals("null")) {
                    valueStr = "";
                }

                PdfPCell valueCell = new PdfPCell(new Phrase(valueStr));
                valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                if (!valueStr.isEmpty()) {
                    double value = Double.parseDouble(valueStr);
                    String paramName = entry.getKey();
                    double[] range = parameterRanges.get(paramName);
                    if (range != null) {
                        double fromValue = range[0];
                        double toValue = range[1];
                        if (value > toValue) {
                            valueCell.setBackgroundColor(CMYKColor.RED);
                        } else if (value < fromValue) {
                            valueCell.setBackgroundColor(CMYKColor.BLUE);
                        }
                    }
                }

                table.addCell(valueCell);
            }
        }
//        addStatisticsRow("Max", statistics, table);
//        addStatisticsRow("Min", statistics, table);
//        addStatisticsRow("Avg", statistics, table);
        document.add(table);
        document.close();
        // Create the PDF file name in the same format as before
        String templateName = templateService.getById(templateId).getName().replaceAll("[^a-zA-Z0-9]", "_"); // Replace non-alphanumeric characters with underscores
        String pdfFileName = templateName + "_" + formattedFromDateTime + "_TO_" + formattedToDateTime + ".pdf";
        Date currentDate = new Date(Calendar.getInstance().getTimeInMillis());
        long currentTimeMillis = currentDate.getTime();
        String currentDateStr = Long.toString(currentTimeMillis);

        //int chk = (assigned_approver == null) ? 0 : 1;
        int chk = (assigned_approver == null || assigned_approver.trim().isEmpty()) ? 0 : 1;
        // Insert the PDF into the database using JdbcTemplate
        String sql = "INSERT INTO stored_reports_weekly (name, from_date, to_date, pdf_data, generated_by, generated_date, assigned_review, assigned_approver, is_approver_required) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, pdfFileName);  // Set the formatted file name
            ps.setString(2, fromDateTime);
            ps.setString(3, toDate);
            ps.setBytes(4, byteArrayOutputStream.toByteArray());
            ps.setString(5, username);
            ps.setString(6, currentDateStr);
            ps.setString(7, assignedTo);
            ps.setString(8, assigned_approver);
            log.info("APPROVER IS {}", assigned_approver);
            log.info("chk is {} " ,chk);
            ps.setBoolean(9, chk==1);
            return ps;
        });
    }

    private Map<String, double[]> extractParameterRanges(Long templateId) {
        ReportTemplate template = templateService.getById(templateId);
        Map<String, double[]> parameterRanges = new HashMap<>();

        for (String parameter : template.getParameters()) {
            String cleanParameter = removeSuffix(parameter);
            double fromValue = getFromValue(parameter);
            double toValue = getToValue(parameter);
            parameterRanges.put(cleanParameter, new double[]{fromValue, toValue});
        }

        return parameterRanges;
    }

    private String removeSuffix(String columnName) {
        if (columnName.contains("_From_")) {
            return columnName.substring(0, columnName.indexOf("_From_"));
        }
        return columnName;
    }

    private double getFromValue(String paramName) {
        if (paramName.contains("_From_")) {
            int fromIndex = paramName.lastIndexOf("_From_") + 6;
            int toIndex = paramName.lastIndexOf("_To_");
            String fromStr = paramName.substring(fromIndex, toIndex);
            return Double.parseDouble(fromStr);
        }
        return Double.NEGATIVE_INFINITY;  // Default if no "From" found
    }

    private double getToValue(String paramName) {
        if (paramName.contains("_To_")) {
            int toIndex = paramName.lastIndexOf("_To_") + 4;
            String toStr = paramName.substring(toIndex);
            return Double.parseDouble(toStr);
        }
        return Double.POSITIVE_INFINITY;  // Default if no "To" found
    }


    //------------------Vishal (Code Added)



    private void addStatisticsRow(String label, Map<String, Map<String, Integer>> statistics, PdfPTable table) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label));
        labelCell.setBackgroundColor(CMYKColor.YELLOW); // Set the background color to yellow
        table.addCell(labelCell);

        for (String parameter : statistics.keySet()) {
            String value = String.valueOf(statistics.get(parameter).get(label.toLowerCase()));
            PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : ""));
            valueCell.setBackgroundColor(CMYKColor.YELLOW); // Set the background color to yellow
            table.addCell(valueCell);
        }
    }



    private void addTableHeader(Map<String, Object> stringObjectMap, PdfPTable table, PdfPCell cell) {
        cell.setBackgroundColor(CMYKColor.GRAY);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        font.setColor(CMYKColor.WHITE);

        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        table.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);
        for (Map.Entry<String,Object> entry : stringObjectMap.entrySet()) {
            cell.setPhrase(new Phrase(entry.getKey(), font));
            table.addCell(cell);
        }
    }

    private class TablePageEvent extends PdfPageEventHelper {


        private final String fromDateTime;
        private final String toDateTime;
        private final String username;


        public TablePageEvent(String fromDateTime, String toDateTime, String username) {
            this.fromDateTime = fromDateTime;
            this.toDateTime = toDateTime;
            this.username = username;
        }


        public void onStartPage(PdfWriter writer, Document document) {

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitle.setSize(13);
            PdfPCell cell = new PdfPCell();

            // Create Table Cells for table header
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);

            // Set width of each column to be equal
            float[] columnWidths = {13f,10f, 50f};
            headerTable.setWidths(columnWidths);

            // Create cells and add to the table
            PdfPCell cell1 = new PdfPCell(new Paragraph("Cell 1"));
            int noBorder = Rectangle.NO_BORDER;
            cell1.setBorder(noBorder);
            Image image = null;
            try {
                image = Image.getInstance(new ClassPathResource("static/images/logo1.png").getURL());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            cell1.setImage(image);
            PdfPCell cell2 = new PdfPCell(new Paragraph(""));
            cell2.setBorder(noBorder);

            PdfPCell cell3 = new PdfPCell(new Paragraph(address, fontTitle));
            cell3.setBorder(noBorder);

            PdfPCell cell4 = new PdfPCell(new Paragraph(""));
            cell4.setBorder(noBorder);

            PdfPCell cell5 = new PdfPCell(new Paragraph(""));
            cell5.setBorder(noBorder);


            PdfPCell cell6 = new PdfPCell(new Paragraph(heading+" °C\n\n", fontTitle));
            cell6.setBorder(noBorder);
            cell6.setHorizontalAlignment(Element.ALIGN_LEFT);

            String startTime = fromDateTime.split(" ")[1];
            PdfPCell cell7 = new PdfPCell(new Paragraph("Start Date:" + fromDateTime.split(" ")[0]  +"\nStart Time:" + startTime  ));
            cell7.setBorder(noBorder);

            String endTime = toDateTime.split(" ")[1];
            PdfPCell cell8 = new PdfPCell(new Paragraph(""));
            cell8.setBorder(noBorder);

            PdfPCell cell9 = new PdfPCell(new Paragraph("End Date:" +toDateTime.split(" ")[0] +"\nEnd Time:"+endTime+"\nRange: (20-25) °C" ));
            cell9.setBorder(noBorder);
            cell9.setHorizontalAlignment(Element.ALIGN_RIGHT);


            headerTable.addCell(cell1);
            headerTable.addCell(cell2);
            headerTable.addCell(cell3);

            headerTable.addCell(cell4);
            headerTable.addCell(cell5);
            headerTable.addCell(cell6);

            headerTable.addCell(cell7);
            headerTable.addCell(cell8);
            headerTable.addCell(cell9);
            document.add(headerTable);

            if(writer.getCurrentPageNumber() > 1) {

                Map<String, Object> stringObjectMap = reportDataList.get(0);

                PdfPTable table = new PdfPTable(stringObjectMap.size());
                table.setWidthPercentage(100f);
                table.setSpacingBefore(5);
                addTableHeader(stringObjectMap, table, cell);
                document.add(table);
            }

        }

        public ReportDTO getReportById(Long reportId) {
            String sql = "SELECT name, from_date, to_date, pdf_data, generated_by, generated_date, is_approved, approved_by, approved_date, assigned_review, reviewed_by, review_date, is_approver_required, assigned_approver FROM stored_reports WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{reportId}, (rs, rowNum) -> new ReportDTO(
                    reportId,
                    rs.getString("name"),
                    rs.getString("from_date"),
                    rs.getString("to_date"),
                    rs.getBytes("pdf_data"),
                    rs.getString("generated_by"),
                    rs.getString("generated_date"),
                    rs.getBoolean("is_approved"),
                    rs.getString("approved_by"),
                    rs.getString("approved_date"),
                    rs.getString("assigned_review"),  // Retrieve assignedReview
                    rs.getString("reviewed_by"),      // Retrieve reviewedBy
                    rs.getString("review_date"),      // Retrieve reviewDate
                    rs.getBoolean("is_approver_required"),
                    rs.getString("assigned_approver")
            ));
        }





        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footerTable = new PdfPTable(2);

            footerTable.setWidthPercentage(100);

            // Set width of each column
            float[] columnWidths = {50f, 50f};
            footerTable.setWidths(columnWidths);

            Font fontTiltle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTiltle.setSize(13);

            LocalDateTime currentDate = LocalDateTime.now();
//    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//    String formattedDate = currentDate.format(formatter);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = currentDate.format(formatter);

            PdfPCell cell1 = new PdfPCell(new Paragraph("Generated By:\nOperator:" + username + "\nDate: " + formattedDate));
            cell1.setBorder(Rectangle.NO_BORDER);

            PdfPCell cell2 = new PdfPCell(new Paragraph(""));
            cell2.setBorder(Rectangle.NO_BORDER);

            footerTable.addCell(cell1);
            footerTable.addCell(cell2);

            try {
                footerTable.setTotalWidth(document.right() - document.left());
                footerTable.writeSelectedRows(0, -1, document.left(), document.bottom() + 20, writer.getDirectContent());
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }

    }



    public void generateWeeklyReports() {
        // Get the current day of the week and hour
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()); // Get current time with system's time zone
        int currentHour = now.getHour();
        DayOfWeek currentDay = now.getDayOfWeek();

        log.info("Inside Weekly Report Function");

        // Fetch the list of reports that are scheduled for the current day and hour
        String sql = "SELECT IdOfReport, Name, assigned_review, isApproverRequired, assignedApprover, TimeOfReport, dayOfReport FROM weekly_scheduled_reports WHERE TimeOfReport = ? AND dayOfReport = ?";
        List<Map<String, Object>> scheduledReports = jdbcTemplate.queryForList(sql, currentHour, currentDay.toString());

        for (Map<String, Object> report : scheduledReports) {
            Integer reportIdInteger = (Integer) report.get("IdOfReport");
            Long reportId = reportIdInteger.longValue(); // Convert Integer to Long if needed

            String reportName = (String) report.get("Name");
            String assignedTo = (String) report.get("assigned_review");
            String assignedApprover = (String) report.get("assignedApprover");

            // Calculate the fromDate and toDate for the previous 7 days, based on TimeOfReport
            ZonedDateTime fromDateTime = now.minusDays(7).withHour(currentHour).withMinute(0).withSecond(0).withNano(0).minusMonths(16); // for testing
            ZonedDateTime toDateTime = now.withHour(currentHour).withMinute(0).withSecond(0).withNano(0).minusMonths(16); // for testing
            log.info("Weekly schedule started for ReportId:{}, FromDate: {}, ToDate: {}",
                    reportId, fromDateTime, toDateTime);
            String fromDate = Long.toString(fromDateTime.toInstant().toEpochMilli()); // Use system's timezone
            String toDate = Long.toString(toDateTime.toInstant().toEpochMilli());

            log.info("Weekly schedule started for ReportId:{}, Report: {}, AssignedTo: {}, AssignedApprover: {}, FromDate: {}, ToDate: {}",
                    reportId, reportName, assignedTo, assignedApprover, fromDate, toDate);

            // Generate the PDF for this report
            try {
                generatePdfWeeklySchedule(reportId, fromDate, toDate, "Automatic", assignedTo, assignedApprover);
            } catch (Exception e) {
                log.error("Failed to generate report for IdOfReport: {}", reportId, e);
            }
        }
    }




    public List<ReportDTO> getAllWeeklyReports() {
        String sql = "SELECT id, name, from_date, to_date, generated_by, generated_date, is_approved, approved_by, approved_date, assigned_review, reviewed_by, review_date, is_approver_required, assigned_approver FROM stored_reports_weekly ORDER BY generated_date DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ReportDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("from_date"),
                rs.getString("to_date"),
                null, // pdfData will be set in getReportById
                rs.getString("generated_by"),
                rs.getString("generated_date"),
                rs.getBoolean("is_approved"),
                rs.getString("approved_by"),
                rs.getString("approved_date"),
                rs.getString("assigned_review"),  // Retrieve assignedReview
                rs.getString("reviewed_by"),      // Retrieve reviewedBy
                rs.getString("review_date") ,      // Retrieve reviewDate
                rs.getBoolean("is_approver_required"),
                rs.getString("assigned_approver")
        ));
    }

    public ReportDTO getWeeklyReportById(Long reportId) {
        String sql = "SELECT name, from_date, to_date, pdf_data, generated_by, generated_date, is_approved, approved_by, approved_date, assigned_review, reviewed_by, review_date, is_approver_required, assigned_approver FROM stored_reports_weekly WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{reportId}, (rs, rowNum) -> new ReportDTO(
                reportId,
                rs.getString("name"),
                rs.getString("from_date"),
                rs.getString("to_date"),
                rs.getBytes("pdf_data"),
                rs.getString("generated_by"),
                rs.getString("generated_date"),
                rs.getBoolean("is_approved"),
                rs.getString("approved_by"),
                rs.getString("approved_date"),
                rs.getString("assigned_review"),  // Retrieve assignedReview
                rs.getString("reviewed_by"),      // Retrieve reviewedBy
                rs.getString("review_date"),      // Retrieve reviewDate
                rs.getBoolean("is_approver_required"),
                rs.getString("assigned_approver")
        ));
    }

    public void reviewReport(Long reportId, String username) throws Exception {
        // Update the PDF and approval details in the database
        long reviewedTimeMillis = System.currentTimeMillis();
        String sql = "UPDATE stored_reports_weekly SET reviewed_by = ?, review_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, username, String.valueOf(reviewedTimeMillis), reportId);
    }

    public void approveReport(Long reportId, String username) throws Exception {
        // Fetch the existing PDF from the database
        ReportDTO reportDTO = getReportById(reportId);
        if (reportDTO == null) {
            throw new Exception("Report not found");
        }

        // Read the existing PDF
        PdfReader pdfReader = new PdfReader(new ByteArrayInputStream(reportDTO.getPdfData()));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper pdfStamper = new PdfStamper(pdfReader, byteArrayOutputStream);

        // Get the content of the last page
        PdfContentByte content = pdfStamper.getOverContent(pdfReader.getNumberOfPages());

        // Create a table similar to the footer style
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);

        Font fontTiltle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        fontTiltle.setSize(13);

        // For PDF: using the formatter to show in 'yyyy-MM-dd' format
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);

        PdfPCell reviewCell = new PdfPCell(new Paragraph("Reviewed By:\nSupervisor: " + username + "\nDate: " + formattedDate, fontTiltle));
        reviewCell.setBorder(Rectangle.NO_BORDER);
        reviewCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        footerTable.addCell(reviewCell);

        // Set the table's position at the bottom right of the last page
        Rectangle pageSize = pdfReader.getPageSize(pdfReader.getNumberOfPages());
        float leftMargin = pageSize.getLeft();
        float bottomMargin = pageSize.getBottom();
        float rightMargin = pageSize.getRight();

        footerTable.setTotalWidth(rightMargin - leftMargin);
        footerTable.writeSelectedRows(0, -1, leftMargin, bottomMargin + 50, content);

        pdfStamper.close();
        pdfReader.close();

        // Convert the current time to milliseconds for storing in the database
        long approvedTimeMillis = System.currentTimeMillis();

        // Update the PDF and approval details in the database
        String sql = "UPDATE stored_reports_weekly SET pdf_data = ?, is_approved = ?, approved_by = ?, approved_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, byteArrayOutputStream.toByteArray(), true, username, String.valueOf(approvedTimeMillis), reportId);
    }

    public ReportDTO getReportById(Long reportId) {
        String sql = "SELECT name, from_date, to_date, pdf_data, generated_by, generated_date, is_approved, approved_by, approved_date, assigned_review, reviewed_by, review_date, is_approver_required, assigned_approver FROM stored_reports_daily WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{reportId}, (rs, rowNum) -> new ReportDTO(
                reportId,
                rs.getString("name"),
                rs.getString("from_date"),
                rs.getString("to_date"),
                rs.getBytes("pdf_data"),
                rs.getString("generated_by"),
                rs.getString("generated_date"),
                rs.getBoolean("is_approved"),
                rs.getString("approved_by"),
                rs.getString("approved_date"),
                rs.getString("assigned_review"),  // Retrieve assignedReview
                rs.getString("reviewed_by"),      // Retrieve reviewedBy
                rs.getString("review_date"),      // Retrieve reviewDate
                rs.getBoolean("is_approver_required"),
                rs.getString("assigned_approver")
        ));
    }



}
