package ncpl.bms.reports.model.dto;

import lombok.Data;

@Data
public class AlarmRecordDTO {
    private String source;
    private Long timestamp;
    private Long ackState;
    private Long alarmClass;
    private Long normalTime;
    private Long ackTime;
    private String messageText;
    private Long timeOfLastAlarm;

}