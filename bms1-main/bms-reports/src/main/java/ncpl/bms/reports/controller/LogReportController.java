package ncpl.bms.reports.controller;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.service.*;
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
public class LogReportController {

    @Autowired
    private LogReportService logService;

    @PostMapping("/log-login")
    public void logUserLogin(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        logService.logUserLogin(username);
    }

    @PostMapping("/log-logout")
    public void logUserLogout(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        logService.logUserLogout(username);
    }

    @PostMapping("/log-auto-logout")
    public void logUserAutoLogout(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");
        logService.logUserAutoLogout(username);
    }

    @GetMapping("/log-reports")
    public List<Map<String, Object>> getLogReports() {
        return logService.getAllLogReports();
    }



    @GetMapping("/download-log-report")
    public ResponseEntity<byte[]> downloadLogReportPdf(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate){


        ByteArrayInputStream bais = logService.generateLogReportPdf(fromDate, toDate);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=log-report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bais.readAllBytes());
    }


}

