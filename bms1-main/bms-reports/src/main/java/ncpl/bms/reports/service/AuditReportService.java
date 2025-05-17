package ncpl.bms.reports.service;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
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
            PdfWriter.getInstance(document, out);
            document.open();

            String title = "Audit Report from " + fromDateStr + " to " + toDateStr;
            document.add(new Paragraph(title));
            document.add(new Paragraph(" ")); // Add some space between title and table

            // Convert fromDate and toDate from String to milliseconds
            long fromDateMillis = convertToMillis(fromDateStr);
            long toDateMillis = convertToMillis(toDateStr);

            // SQL query with date range filter
            String sql = "SELECT * FROM audit_report WHERE timestamp BETWEEN ? AND ?";
            List<Map<String, Object>> auditLogs = jdbcTemplate.queryForList(sql, fromDateMillis, toDateMillis);

            // Add table to PDF
            PdfPTable table = new PdfPTable(3);
            table.addCell("Timestamp");
            table.addCell("Username");
            table.addCell("Action");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Map<String, Object> log : auditLogs) {
                // Directly retrieve the timestamp as Long
                Long timestampMillis = (Long) log.get("timestamp");
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        new java.util.Date(timestampMillis).toInstant(),
                        ZoneId.systemDefault()
                );
                String formattedDate = dateTime.format(formatter);

                table.addCell(formattedDate);
                table.addCell((String) log.get("username"));
                table.addCell((String) log.get("action"));
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            logger.error("Error generating audit report PDF", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
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
}
