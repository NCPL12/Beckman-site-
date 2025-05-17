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

//---------------------COMPLETE FILE BY VISHAL---------------------
@Component
@Slf4j
public class LogReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(LogReportService.class);

    // Log user login action
    public void logUserLogin(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "login";

        String sql = "INSERT INTO log_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log user logout action
    public void logUserLogout(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "logout";

        String sql = "INSERT INTO log_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    // Log user auto logout action
    public void logUserAutoLogout(String username) {
        long currentTimeMillis = System.currentTimeMillis();
        String action = "auto-logout";

        String sql = "INSERT INTO log_report (timestamp, username, action) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, String.valueOf(currentTimeMillis), username, action);
    }

    public List<Map<String, Object>> getAllLogReports() {
        String sql = "SELECT * FROM log_report ORDER BY timestamp DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public ByteArrayInputStream generateLogReportPdf(String fromDateStr, String toDateStr) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            String title = "Log Report from " + fromDateStr + " to " + toDateStr;
            document.add(new Paragraph(title));
            document.add(new Paragraph(" ")); // Add some space between title and table

            // Convert fromDate and toDate from String to milliseconds
            long fromDateMillis = convertToMillis(fromDateStr);
            long toDateMillis = convertToMillis(toDateStr);

            // SQL query with date range filter
            String sql = "SELECT * FROM log_report WHERE timestamp BETWEEN ? AND ?";
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql, fromDateMillis, toDateMillis);

            // Add table to PDF
            PdfPTable table = new PdfPTable(3);
            table.addCell("Timestamp");
            table.addCell("Username");
            table.addCell("Action");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Map<String, Object> log : logs) {
                // Extract timestamp as Long
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
            logger.error("Error generating PDF report", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private long convertToMillis(String dateStr) {
        try {
            // Convert the date string to LocalDateTime, then to milliseconds
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date", e);
            return 0; // Handle error appropriately
        }
    }
}