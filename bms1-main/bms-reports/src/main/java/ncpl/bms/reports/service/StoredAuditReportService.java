package ncpl.bms.reports.service;

import ncpl.bms.reports.model.dto.StoredAuditReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StoredAuditReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<StoredAuditReportDTO> getAllStoredAuditReports() {
        String sql = "SELECT id, report_name, generated_on FROM StoredAuditReport ORDER BY generated_on DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<StoredAuditReportDTO> reports = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            StoredAuditReportDTO dto = new StoredAuditReportDTO();
            dto.setId((Integer) row.get("id"));
            dto.setReportName((String) row.get("report_name"));
            dto.setGeneratedOn(((java.sql.Timestamp) row.get("generated_on")).toString());
            reports.add(dto);
        }

        return reports;
    }
}