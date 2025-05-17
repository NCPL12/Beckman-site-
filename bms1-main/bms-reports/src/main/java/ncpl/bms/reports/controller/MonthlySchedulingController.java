package ncpl.bms.reports.controller;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.db.info.TableInfoService;
import ncpl.bms.reports.model.dto.ReportDTO;
import ncpl.bms.reports.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

//--------------------COMPLETE FILE IS WRITTEN BY VISHAL----------------------//

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class MonthlySchedulingController {
    @Autowired
    private MonthlySchedulingService monthlySchedulingService;

    @Autowired
    private TableInfoService tableInfoService;
    @Autowired
    private ReportTemplateService templateService;

    @Autowired
    private LogReportService logService;

    @Scheduled(cron = "0 0 * * * ?")
    public void runMonthlyReportGenerationTask() {
        //log.info("Started cron job for monthly report generation");
        monthlySchedulingService.generateMonthlyReports();
    }

    @GetMapping("/monthly-reports")
    public ResponseEntity<List<ReportDTO>> getReports() {
        try {
            List<ReportDTO> reports = monthlySchedulingService.getAllMonthlyReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
           // log.error("Error fetching monthly reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/monthly-reports/{id}")
    public ResponseEntity<byte[]> getReportById(@PathVariable Long id) {
        ReportDTO reportDTO = monthlySchedulingService.getMonthlyReportById(id);
        if (reportDTO == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + id + ".pdf");
        return new ResponseEntity<>(reportDTO.getPdfData(), headers, HttpStatus.OK);
    }

    @PutMapping("/monthly-reports/review/{id}")
    public ResponseEntity<Void> reviewReport(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            monthlySchedulingService.reviewReport(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            //log.error("Error reviewing report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/monthly-reports/approve/{id}")
    public ResponseEntity<Void> approveReport(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            monthlySchedulingService.approveReport(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            //log.error("Error approving report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
