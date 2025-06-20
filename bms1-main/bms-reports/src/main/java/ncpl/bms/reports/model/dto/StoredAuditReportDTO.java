package ncpl.bms.reports.model.dto;

import lombok.Data;

@Data
public class StoredAuditReportDTO {
    private int id;
    private String reportName;
    private String generatedOn;
}