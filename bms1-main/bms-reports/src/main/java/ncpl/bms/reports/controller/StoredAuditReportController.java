package ncpl.bms.reports.controller;

import ncpl.bms.reports.model.dto.StoredAuditReportDTO;
import ncpl.bms.reports.service.StoredAuditReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/stored-audit-report")
@CrossOrigin(origins = "http://localhost:4200")
public class StoredAuditReportController {

    @Autowired
    private StoredAuditReportService storedAuditReportService;

    @GetMapping("/list")
    public ResponseEntity<List<StoredAuditReportDTO>> getAllStoredAuditReports() {
        List<StoredAuditReportDTO> reports = storedAuditReportService.getAllStoredAuditReports();
        return ResponseEntity.ok(reports);
    }
}