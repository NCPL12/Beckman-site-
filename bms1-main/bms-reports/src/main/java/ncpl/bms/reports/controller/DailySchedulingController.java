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
public class DailySchedulingController {


    @Autowired
    private DailySchedulingService dailySchedulingService;

    @Autowired
    private TableInfoService tableInfoService;
    @Autowired
    private ReportTemplateService templateService;

    @Autowired
    private LogReportService logService;

    @Scheduled(cron = "0 0 * * * ?") // This will run the job at the start of every hour
    public void runDailyReportGenerationTask() {
        log.info("started cron Job for daily scheduling  ");
        dailySchedulingService.generateDailyReports();
    }

    @GetMapping("/daily-reports")
    public ResponseEntity<List<ReportDTO>> getReports() {
        try {
            List<ReportDTO> reports = dailySchedulingService.getAllDailyReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            //log.error("Error fetching daily reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/daily-reports/{id}")
    public ResponseEntity<byte[]> getReportById(@PathVariable Long id) {
        ReportDTO reportDTO = dailySchedulingService.getDailyReportById(id);
        if (reportDTO == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + id + ".pdf");
        return new ResponseEntity<>(reportDTO.getPdfData(), headers, HttpStatus.OK);
    }

    @PutMapping("/daily-reports/review/{id}")
    public ResponseEntity<Void> reviewReport(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            dailySchedulingService.reviewReport(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error reviewing report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/daily-reports/approve/{id}")
    public ResponseEntity<Void> approveReport(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            dailySchedulingService.approveReport(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error approving report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
