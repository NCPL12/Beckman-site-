package ncpl.bms.reports.service;import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.db.info.TableInfoService;
import ncpl.bms.reports.model.dao.ReportTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;


@Service
@Slf4j
public class ReportDataService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableInfoService tableInfoService;

    @Autowired
    private ReportTemplateService templateService;
    public List<Map<String, Object>> generateReportData(Long templateId, String fromDateMillis, String toDateMillis) {
        List<String> tables = tableInfoService.getTables();
        if (tables == null || tables.isEmpty()) {
            throw new RuntimeException("No tables retrieved from tableInfoService.");
        }
        int max = 0;
        String tableWithMaxRecords = null;

        Timestamp fromDate = new Timestamp(Long.parseLong(fromDateMillis));
        Timestamp toDate = new Timestamp(Long.parseLong(toDateMillis));

        // STEP 1: Clean previous data if it exists
        String checkSql = "SELECT COUNT(*) FROM report_data WHERE timestamp BETWEEN ? AND ?";
        Integer existingCount = jdbcTemplate.queryForObject(checkSql, new Object[]{fromDate.getTime(), toDate.getTime()}, Integer.class);
        if (existingCount != null && existingCount > 0) {
            String deleteSql = "DELETE FROM report_data WHERE timestamp BETWEEN ? AND ?";
            jdbcTemplate.update(deleteSql, fromDate.getTime(), toDate.getTime());
            log.info("Old report data deleted for given date range.");
        }
        // STEP 2: Find table with maximum rows
        for (String tableName : tables) {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE timestamp BETWEEN ? AND ?";
            Integer count = jdbcTemplate.queryForObject(sql, new Object[]{fromDate, toDate}, Integer.class);
            if (count != null && count > max) {
                max = count;
                tableWithMaxRecords = tableName;
            }
        }

        if (tableWithMaxRecords == null) {
            log.warn("No records found in any table for the provided date range.");
            return new ArrayList<>();
        }

        System.out.println("Table with maximum records: {}"+ tableWithMaxRecords);
        final String finalTableWithMaxRecords = tableWithMaxRecords;

        // STEP 3: Insert into report_data
        String fetchSql = "SELECT value, timestamp FROM " + finalTableWithMaxRecords + " WHERE timestamp BETWEEN ? AND ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(fetchSql, fromDate, toDate);

        List<Integer> reportIds = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            long timestampInMillis = convertTimestampToMillis(row.get("timestamp").toString());
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                String insertSql = "INSERT INTO report_data (timestamp, " + finalTableWithMaxRecords + ") VALUES (?, ?)";
                PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, timestampInMillis);
                ps.setObject(2, row.get("value"));
                return ps;
            }, keyHolder);

            reportIds.add(keyHolder.getKey().intValue());
        }

        // STEP 4: Update remaining columns
        List<String> filteredTablesList = tables.stream()
                .filter(t -> !t.equals(finalTableWithMaxRecords))
                .collect(Collectors.toList());

        for (String columnNameToUpdate : filteredTablesList) {
            String updateSql = "SELECT value FROM " + columnNameToUpdate + " WHERE timestamp BETWEEN ? AND ?";
            List<Map<String, Object>> rowsUpdate = jdbcTemplate.queryForList(updateSql, fromDate, toDate);

            int counter = 0;
            for (Map<String, Object> row : rowsUpdate) {
                if (counter >= reportIds.size()) break;

                int reportId = reportIds.get(counter++);
                String updateQuery = "UPDATE report_data SET " + columnNameToUpdate + " = ? WHERE report_id = ?";
                jdbcTemplate.update(updateQuery, row.get("value"), reportId);
            }
        }
        return getReportData(templateId, fromDateMillis, toDateMillis);
    }
    private long convertTimestampToMillis(String timestamp) {
        // Normalize the timestamp to ensure 3 digits for milliseconds
        if (timestamp.contains(".")) {
            int fractionLength = timestamp.substring(timestamp.indexOf(".") + 1).length();
            if (fractionLength == 1) {
                timestamp = timestamp + "00";
            } else if (fractionLength == 2) {
                timestamp = timestamp + "0";
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    public List<Map<String, Object>> getReportData(Long templateId, String fromDate, String toDate) {
        ReportTemplate template = templateService.getById(templateId);
        List<String> allParams = template.getParameters();
        // Step 1: Fetch valid column names from report_data table
        List<String> validColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'report_data'",
                String.class
        );
        // Step 2: Sanitize parameters (remove suffixes) and filter only existing columns
        List<String> safeColumns = allParams.stream()
                .map(this::removeSuffix) // remove _From_, _To_, _Unit_
                .distinct()
                .filter(validColumns::contains) // only keep those that exist in report_data
                .collect(Collectors.toList());

        // Step 3: Prepare final SELECT clause

        StringBuilder columns = new StringBuilder("timestamp");
        for (String column : safeColumns) {
            columns.append(", ").append(column);
        }

        String sqlSelect = "SELECT " + columns + " FROM report_data WHERE timestamp BETWEEN ? AND ?";
        return jdbcTemplate.queryForList(sqlSelect, fromDate, toDate);
    }
    private String removeSuffix(String columnName) {
        String base = columnName;
        if (base.contains("_From_")) {
            base = base.substring(0, base.indexOf("_From_"));
        }
        if (base.contains("_To_")) {
            base = base.substring(0, base.indexOf("_To_"));
        }
        if (base.contains("_Unit_")) {
            base = base.substring(0, base.indexOf("_Unit_"));
        }
        return base;
    }
    public Map<String, Map<String, Integer>> calculateStatistics(Long templateId, String fromDate, String toDate) {
        ReportTemplate template = templateService.getById(templateId);
        Map<String, Map<String, Integer>> statistics = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order

        for (String parameter : template.getParameters()) {
            // Remove suffix from parameter
            String cleanParameter = removeSuffix(parameter);

            // Prepare SQL query with the cleaned parameter name
            String sql = "SELECT MAX(" + cleanParameter + ") AS max_val, MIN(" + cleanParameter + ") AS min_val, AVG(" + cleanParameter + ") AS avg_val FROM report_data WHERE timestamp BETWEEN ? AND ?";
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, fromDate, toDate);

            // Store statistics in a nested map
            Map<String, Integer> statMap = new HashMap<>();
            statMap.put("max", convertToInteger(result.get("max_val")));
            statMap.put("min", convertToInteger( result.get("min_val")));
            statMap.put("avg", convertToInteger(result.get("avg_val")));
            statistics.put(cleanParameter, statMap);
        }
        return statistics;
    }
    private Integer convertToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

}


