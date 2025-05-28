package ncpl.bms.reports.controller;

import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.db.info.TableInfoService;
import ncpl.bms.reports.model.dao.ReportTemplate;
import ncpl.bms.reports.model.dto.GroupDTO;
import ncpl.bms.reports.model.dto.ReportDTO;
import ncpl.bms.reports.service.*;
import ncpl.bms.reports.util.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class ReportsController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private UserService userService;

    @Autowired
    private TableInfoService tableInfoService;

    @Autowired
    private ReportTemplateService templateService;

    @Autowired
    private ReportDataService reportDataService;

    @Autowired
    private DateConverter dateConverter;
    @Autowired
    private GroupService groupService;


    // Edit Template (Includes `units`)
    @PutMapping("editTemplate/{id}")
    public ReportTemplate updateTemplate(@PathVariable Long id, @RequestBody ReportTemplate updatedTemplate) {
        return templateService.updateTemplate(id, updatedTemplate);
    }

    // Delete Templates
    @PostMapping("deleteTemplates")
    public void deleteTemplates(@RequestBody List<Long> ids) {
        templateService.deleteTemplatesByIds(ids);
    }

    // Get Available Parameters
    @GetMapping("parameters")
    public List<String> getParameters() {
        return tableInfoService.getTables();
    }

    @PostMapping("createTemplate")
    public ResponseEntity<ReportTemplate> createTemplate(@RequestBody Map<String, Object> requestBody) {
        ReportTemplate reportTemplate = new ReportTemplate();

        reportTemplate.setName((String) requestBody.get("name"));
        reportTemplate.setReport_group((String) requestBody.get("report_group"));
        reportTemplate.setAdditionalInfo((String) requestBody.get("additionalInfo"));

        reportTemplate.setRoomId((String) requestBody.get("roomId"));       // ✅ added
        reportTemplate.setRoomName((String) requestBody.get("roomName"));   // ✅ added

        Object paramsObject = requestBody.get("parameters");
        List<String> parameters = (paramsObject instanceof List) ? (List<String>) paramsObject : List.of();
        reportTemplate.setParameters(parameters);

        ReportTemplate savedTemplate = templateService.saveTemplate(reportTemplate);
        return ResponseEntity.ok(savedTemplate);
    }

    // Find Templates by Name
    @GetMapping("findByName")
    public List<ReportTemplate> findByName(@RequestParam String name) {
        return templateService.getTemplatesByName(name);
    }
    @GetMapping("templates")
    public ResponseEntity<List<Map<String, Object>>> getAllTemplates() {
        List<ReportTemplate> templates = templateService.getTemplates();

        List<Map<String, Object>> responseList = templates.stream().map(template -> {
            Map<String, Object> templateMap = new HashMap<>();
            templateMap.put("id", template.getId());
            templateMap.put("name", template.getName() != null ? template.getName() : ""); // Prevent null values
            templateMap.put("parameters", template.getParameters() != null ? template.getParameters() : new ArrayList<>()); // Convert to List<String>

            templateMap.put("additionalInfo", template.getAdditionalInfo() != null ? template.getAdditionalInfo() : ""); // Prevent null values
            templateMap.put("report_group", template.getReport_group() != null ? template.getReport_group() : ""); // Prevent null values

            return templateMap;
        }).toList();

        return ResponseEntity.ok(responseList);
    }


    // Get Template by ID
    @GetMapping("templates/{id}")
    public ResponseEntity<Map<String, Object>> getTemplateById(@PathVariable long id) {
        ReportTemplate template = templateService.getById(id);

        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", template.getId());
        response.put("name", template.getName() != null ? template.getName() : ""); // Prevent null values
        response.put("parameters", template.getParameters() != null ? template.getParameters() : new ArrayList<>()); // Convert to List<String>
//        response.put("units", template.getUnits() != null ? template.getUnits() : ""); // Prevent null values
        response.put("additionalInfo", template.getAdditionalInfo() != null ? template.getAdditionalInfo() : ""); // Prevent null values
        response.put("report_group", template.getReport_group() != null ? template.getReport_group() : ""); // Prevent null values

        return ResponseEntity.ok(response);
    }

    // Export Report as PDF
    @GetMapping("exportReport")
    public void exportPdfReport(@RequestParam Long id,
                                @RequestParam String fromDate,
                                @RequestParam String toDate,
                                @RequestParam("username") String username,
                                @RequestParam String assignedTo,
                                @RequestParam(required = false) String assigned_approver) throws Exception {

        Long fromDateMills = dateConverter.stringToLong(fromDate);
        Long toDateMills = dateConverter.stringToLong(toDate);
        ReportTemplate template = templateService.getById(id);

        if ("null".equals(assigned_approver)) {
            assigned_approver = null;
        }

        pdfService.generatePdf(id, fromDateMills.toString(), toDateMills.toString(), username, assignedTo, assigned_approver);
    }

    // Get All Reports
    @GetMapping("/reports")
    public ResponseEntity<List<ReportDTO>> getReports() {
        try {
            List<ReportDTO> reports = pdfService.getAllReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get Report by ID
    @GetMapping("/reports/{id}")
    public ResponseEntity<byte[]> getReportById(@PathVariable Long id) {
        ReportDTO reportDTO = pdfService.getReportById(id);
        if (reportDTO == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Clean filename: remove "null_and_" if present
        String filename = reportDTO.getName();
        if (filename == null || filename.toLowerCase().startsWith("null")) {
            filename = filename == null ? "Report.pdf" : filename.replaceFirst("null_and_", "");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");

        // This sets filename in browser viewer
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

        return new ResponseEntity<>(reportDTO.getPdfData(), headers, HttpStatus.OK);
    }


    // Approve Report
    @PutMapping("/reports/approve/{id}")
    public ResponseEntity<Void> approveReport(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            pdfService.approveReport(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Review Report
    @PutMapping("/reports/review/{id}")
    public ResponseEntity<Void> reviewReport(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        try {
            String username = requestBody.get("username");
            pdfService.reviewReport(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get All Groups
    @GetMapping("/groups")
    public ResponseEntity<List<GroupDTO>> getGroups() {
        try {
            List<GroupDTO> groups = pdfService.getAllGroups();
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
//            log.error("Error fetching groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping("/add_group")
    public ResponseEntity<String> createGroup(@RequestBody GroupDTO groupDTO) {
        try {
//            log.info("Received group: {}", groupDTO.getName());
            groupService.saveGroup(groupDTO);
            return ResponseEntity.ok("Group saved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error saving group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save group: " + e.getMessage());
        }
    }



}
