package ncpl.bms.reports.model.dto;

import lombok.Data;

@Data
public class AuditLogDTO {
    private String timestamp;
    private String operation;
    private String target;
    private String slotName;
    private String oldValue;
    private String value;
    private String userName;
}