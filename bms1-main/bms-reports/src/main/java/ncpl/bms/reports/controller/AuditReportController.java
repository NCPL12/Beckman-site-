package ncpl.bms.reports.controller;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.service.AuditReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

//--------------------COMPLETE FILE IS WRITTEN BY VISHAL----------------------//

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class AuditReportController {

    @Autowired
    private AuditReportService auditService;

    // Log user login operation
    @PostMapping("/audit-login")
    public void logUserLogin(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logUserLogin(username);
    }

    // Log user auto logout operation
    @PostMapping("/audit-auto-logout")
    public void logUserAutoLogout(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logUserAutoLogout(username);
    }

    // Log user logout operation
    @PostMapping("/audit-logout")
    public void logUserLogout(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logUserLogout(username);
    }

    // Log template creation
    @PostMapping("/log-created-template")
    public void logTemplateCreation(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logTemplateCreation(username);
    }

    // Log template deletion
    @PostMapping("/log-deleted-template")
    public void logTemplateDeletion(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logTemplateDeletion(username);
    }

    // Log template editing
    @PostMapping("/log-edited-template")
    public void logTemplateEditing(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logTemplateEditing(username);
    }

    // Log report generation
    @PostMapping("/log-generated-report")
    public void logReportGeneration(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logReportGeneration(username);
    }

    // Log review of a report
    @PostMapping("/log-review-report")
    public void logReviewReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logReviewReport(username);
    }


    // Log report approval
    @PostMapping("/log-approved-report")
    public void logReportApproval(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logReportApproval(username);
    }

    // Log downloading login report
    @PostMapping("/log-download-log-report")
    public void logDownloadLoginReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logDownloadLoginReport(username);
    }

    // Log downloading report
    @PostMapping("/log-download-report")
    public void logDownloadReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logDownloadReport(username);
    }

    // Log downloading audit report
    @PostMapping("/log-download-audit-report")
    public void logDownloadAuditReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        auditService.logDownloadAuditReport(username);
    }

    // Get all audit reports
    @GetMapping("/audit-reports")
    public List<Map<String, Object>> getAuditReports() {
        return auditService.getAllAuditReports();
    }


    // Download audit report as PDF
    @GetMapping("/download-audit-report")
    public ResponseEntity<byte[]> downloadAuditReportPdf(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate) {

        ByteArrayInputStream bais = auditService.generateAuditReportPdf(fromDate, toDate);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=audit-report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bais.readAllBytes());
    }

    @PostMapping("/log-scheduled-daily-report")
    public void logScheduledDailyReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        String reportType = requestBody.get("reportType"); // daily, weekly, or monthly
        auditService.logScheduledDailyReport(username, reportType);
    }

    @PostMapping("/log-scheduled-weekly-report")
    public void logScheduledWeeklyReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        String reportType = requestBody.get("reportType"); // daily, weekly, or monthly
        auditService.logScheduledWeeklyReport(username, reportType);
    }

    @PostMapping("/log-scheduled-monthly-report")
    public void logScheduledMonthlyReport(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        String reportType = requestBody.get("reportType"); // daily, weekly, or monthly
        auditService.logScheduledMonthlyReport(username, reportType);
    }
}
