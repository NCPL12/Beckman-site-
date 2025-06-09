package ncpl.bms.reports.service;

import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.db.info.TableInfoService;
import ncpl.bms.reports.model.dao.ReportTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Calendar;

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

        Timestamp fromDate = new Timestamp(Long.parseLong(fromDateMillis));
        Timestamp toDate = new Timestamp(Long.parseLong(toDateMillis));

        log.info("Report generation started for template: {}, from: {}, to: {}", templateId, fromDate, toDate);

        // Clean existing report_data entries in range
        String checkSql = "SELECT COUNT(*) FROM report_data WHERE timestamp BETWEEN ? AND ?";
        Integer existingCount = jdbcTemplate.queryForObject(checkSql, new Object[]{fromDate, toDate}, Integer.class);
        if (existingCount != null && existingCount > 0) {
            log.warn("Existing report_data rows in range: {} — deleting them", existingCount);
            String deleteSql = "DELETE FROM report_data WHERE timestamp BETWEEN ? AND ?";
            jdbcTemplate.update(deleteSql, fromDate, toDate);
        }

        // Find table with most records
        int max = 0;
        String tableWithMaxRecords = null;
        for (String tableName : tables) {
            try {
                String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE timestamp BETWEEN ? AND ?";
                Integer count = jdbcTemplate.queryForObject(sql, new Object[]{fromDate, toDate}, Integer.class);
                log.info("Table: {} → count: {}", tableName, count);
                if (count != null && count > max) {
                    max = count;
                    tableWithMaxRecords = tableName;
                }
            } catch (Exception e) {
                log.error("Error counting rows from table {}: {}", tableName, e.getMessage());
            }
        }

        if (tableWithMaxRecords == null) {
            log.warn("No data found in any source table.");
            return Collections.emptyList();
        }

        final String mainTable = tableWithMaxRecords;
        log.info("Selected base table for report_data: {}", mainTable);

        // Step 1: Insert timestamps + base values into report_data
        String fetchSql = "SELECT timestamp, value FROM " + mainTable + " WHERE timestamp BETWEEN ? AND ?";
        List<Map<String, Object>> baseRows = jdbcTemplate.queryForList(fetchSql, fromDate, toDate);

        for (Map<String, Object> row : baseRows) {
            Timestamp ts;
            Object val = row.get("value");
            try {
                ts = normalizeTimestamp((Timestamp) row.get("timestamp"));
            } catch (Exception e) {
                log.error("Timestamp parse error: {}", row.get("timestamp"));
                continue;
            }

            // Check if already inserted
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM report_data WHERE timestamp = ?", new Object[]{ts}, Integer.class
            );

            if (count != null && count == 0) {
                log.debug("Inserting into report_data: timestamp={}, {}={}", ts, mainTable, val);

                jdbcTemplate.update(con -> {
                    String sql = "INSERT INTO report_data (timestamp, " + mainTable + ") VALUES (?, ?)";
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setTimestamp(1, ts);
                    ps.setObject(2, val);
                    return ps;
                });
            } else {
                log.warn("Skipped insert: timestamp {} already exists in report_data", ts);
            }
        }

        // Step 2: Update other columns by matching timestamps
        List<String> otherTables = tables.stream().filter(t -> !t.equals(mainTable)).collect(Collectors.toList());

        for (String table : otherTables) {
            String readSql = "SELECT timestamp, value FROM " + table + " WHERE timestamp BETWEEN ? AND ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(readSql, fromDate, toDate);

            for (Map<String, Object> row : rows) {
                Timestamp ts;
                try {
                    ts = normalizeTimestamp((Timestamp) row.get("timestamp"));
                } catch (Exception e) {
                    log.warn("Skipping invalid timestamp from {}: {}", table, row.get("timestamp"));
                    continue;
                }
                Object value = row.get("value");

                int updated = jdbcTemplate.update(
                        "UPDATE report_data SET " + table + " = ? WHERE timestamp = ?",
                        value, ts
                );

                if (updated == 0) {
                    log.warn("No matching timestamp in report_data for {}.{} → {}", table, ts, value);
                } else {
                    log.debug("Updated report_data: {}={} at {}", table, value, ts);
                }
            }
        }

        log.info("Report data populated. Fetching for preview...");
        return getReportData(templateId, fromDate.toString(), toDate.toString());
    }

    public List<Map<String, Object>> getReportData(Long templateId, String fromDate, String toDate) {
        ReportTemplate template = templateService.getById(templateId);
        List<String> allParams = template.getParameters();

        List<String> validColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'report_data'",
                String.class
        );

        List<String> safeColumns = allParams.stream()
                .map(this::removeSuffix)
                .distinct()
                .filter(validColumns::contains)
                .collect(Collectors.toList());

        StringBuilder columns = new StringBuilder("timestamp");
        for (String column : safeColumns) {
            columns.append(", ").append(column);
        }

        String sqlSelect = "SELECT " + columns + " FROM report_data WHERE timestamp BETWEEN ? AND ?";
        return jdbcTemplate.queryForList(sqlSelect, Timestamp.valueOf(fromDate), Timestamp.valueOf(toDate));
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
        Map<String, Map<String, Integer>> statistics = new LinkedHashMap<>();

        log.info("Calculating statistics for template {}, from {} to {}", templateId, fromDate, toDate);

        // Check total rows in report_data in the given range
        String debugSql = "SELECT COUNT(*) FROM report_data WHERE timestamp BETWEEN ? AND ?";
        Integer rowCount = jdbcTemplate.queryForObject(debugSql,
                new Object[]{Timestamp.valueOf(fromDate), Timestamp.valueOf(toDate)},
                Integer.class);
        log.info("Row count in report_data for given range: {}", rowCount);

        for (String parameter : template.getParameters()) {
            String cleanParameter = removeSuffix(parameter);

            // Check if column exists in report_data
            String existsSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'report_data' AND COLUMN_NAME = ?";
            Integer exists = jdbcTemplate.queryForObject(existsSql, new Object[]{cleanParameter}, Integer.class);

            if (exists == null || exists == 0) {
                log.warn("Column '{}' not found in report_data, skipping", cleanParameter);
                continue;
            }

            // Check how many non-null values are present
            String nullCheckSql = "SELECT COUNT(*) FROM report_data WHERE " + cleanParameter + " IS NOT NULL AND timestamp BETWEEN ? AND ?";
            Integer nonNullCount = jdbcTemplate.queryForObject(nullCheckSql,
                    new Object[]{Timestamp.valueOf(fromDate), Timestamp.valueOf(toDate)},
                    Integer.class);
            log.info("Non-null count for '{}': {}", cleanParameter, nonNullCount);

            // Now fetch statistics
            String sql = "SELECT MAX(" + cleanParameter + ") AS max_val, MIN(" + cleanParameter + ") AS min_val, AVG(" + cleanParameter + ") AS avg_val " +
                    "FROM report_data WHERE timestamp BETWEEN ? AND ?";

            Map<String, Object> result = jdbcTemplate.queryForMap(sql, Timestamp.valueOf(fromDate), Timestamp.valueOf(toDate));
            log.info("Raw stats for '{}': {}", cleanParameter, result);

            Map<String, Integer> statMap = new HashMap<>();
            statMap.put("max", convertToInteger(result.get("max_val")));
            statMap.put("min", convertToInteger(result.get("min_val")));
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

    private Timestamp normalizeTimestamp(Timestamp ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts.getTime());

        // Align to nearest 15-minute bucket (customize if needed)
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        int minute = cal.get(Calendar.MINUTE);
        int rounded = (minute / 15) * 15;
        cal.set(Calendar.MINUTE, rounded);

        return new Timestamp(cal.getTimeInMillis());
    }
}
