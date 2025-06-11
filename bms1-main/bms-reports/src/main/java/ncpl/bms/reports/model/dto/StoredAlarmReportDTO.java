package ncpl.bms.reports.model.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class StoredAlarmReportDTO {
    private int id;
    private String reportName;
    private Timestamp generatedOn;
    private String generatedBy;
    private String reviewedBy;
    private Timestamp reviewDate;
}