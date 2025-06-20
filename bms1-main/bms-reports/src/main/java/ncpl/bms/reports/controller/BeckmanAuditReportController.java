package ncpl.bms.reports.controller;

import ncpl.bms.reports.service.BeckmanAuditReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/v1/audit-report")
@CrossOrigin(origins = "http://localhost:4200")
public class BeckmanAuditReportController {

    @Autowired
    private BeckmanAuditReportService auditService;

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadAuditReport(@RequestParam String startDate,
                                                      @RequestParam String endDate) {
        try {
            byte[] pdf = auditService.generateAuditReportPdf(startDate, endDate);

            if (pdf == null || pdf.length == 0) {
                return ResponseEntity.noContent().build();
            }

            // ✅ Save to DB with proper name
            auditService.saveAuditReportPdf(pdf, startDate, endDate);

            // ✅ Format filename from dates
            Date start = tryParseDate(startDate);
            Date end = tryParseDate(endDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedStart = outputFormat.format(start);
            String formattedEnd = outputFormat.format(end);
            String filename = "Audit_Report_" + formattedStart + "_to_" + formattedEnd + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .body(pdf);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> viewStoredAuditReport(@PathVariable int id) {
        byte[] pdfBytes = auditService.getStoredAuditReportById(id);

        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=stored_audit_report_" + id + ".pdf")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdfBytes);
    }

    // ✅ Flexible date parsing utility
    private Date tryParseDate(String input) throws java.text.ParseException {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(input);
            } catch (java.text.ParseException ignored) {}
        }
        throw new java.text.ParseException("Unrecognized date format: " + input, 0);
    }
}