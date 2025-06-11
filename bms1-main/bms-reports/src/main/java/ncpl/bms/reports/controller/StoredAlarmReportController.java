package ncpl.bms.reports.controller;

import ncpl.bms.reports.model.dto.StoredAlarmReportDTO;
import ncpl.bms.reports.service.StoredAlarmReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/stored-alarm-report")
@CrossOrigin(origins = "http://localhost:4200")
public class StoredAlarmReportController {

    @Autowired
    private StoredAlarmReportService storedAlarmReportService;

    // List all stored alarm reports
    @GetMapping("/list")
    public ResponseEntity<List<StoredAlarmReportDTO>> listStoredAlarmReports() {
        try {
            List<StoredAlarmReportDTO> reports = storedAlarmReportService.getAllStoredAlarmReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}