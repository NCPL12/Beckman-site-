package ncpl.bms.reports.service;

import ncpl.bms.reports.model.dto.StoredAlarmReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Service
public class StoredAlarmReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Fetch all stored alarm reports
    public List<StoredAlarmReportDTO> getAllStoredAlarmReports() {
        String sql = "SELECT id, report_name, generated_on, reviewed_by, review_date, generated_by " +
                "FROM StoredAlarmReport ORDER BY generated_on DESC";

        return jdbcTemplate.query(sql, (ResultSet rs, int rowNum) -> {
            StoredAlarmReportDTO dto = new StoredAlarmReportDTO();
            dto.setId(rs.getInt("id"));
            dto.setReportName(rs.getString("report_name"));

            // Handle datetime column
            dto.setGeneratedOn(rs.getTimestamp("generated_on"));

            // Handle bigint column (epoch millis)
            long reviewDateMillis = rs.getLong("review_date");
            dto.setReviewDate(reviewDateMillis > 0 ? new Timestamp(reviewDateMillis) : null);

            dto.setGeneratedBy(rs.getString("generated_by"));
            dto.setReviewedBy(rs.getString("reviewed_by"));
            return dto;
        });
    }


}