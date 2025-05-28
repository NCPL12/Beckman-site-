package ncpl.bms.reports.service;

import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.model.dao.ReportTemplate;
import ncpl.bms.reports.repository.ReportTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ReportTemplateService {

    @Autowired
    private ReportTemplateRepository reportTemplateRepo;


    public ReportTemplate saveTemplate(ReportTemplate reportTemplate) {
        return reportTemplateRepo.save(reportTemplate);
    }

    public List<ReportTemplate> getTemplates(){
        return reportTemplateRepo.findAll();
    }

    public ReportTemplate getById(Long id) {
//        log.info("Get template with id {} ", id);
        return reportTemplateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id " + id));
    }

    public List<ReportTemplate> getTemplatesByName(String name) {
        return reportTemplateRepo.findByName(name);
    }

    public void deleteTemplatesByIds(List<Long> ids) {
        reportTemplateRepo.deleteAllById(ids);
    }

    public ReportTemplate updateTemplate(Long id, ReportTemplate updatedTemplate) {
        Optional<ReportTemplate> existingTemplate = reportTemplateRepo.findById(id);
        if (existingTemplate.isPresent()) {
            ReportTemplate template = existingTemplate.get();
            template.setName(updatedTemplate.getName());
            template.setReport_group(updatedTemplate.getReport_group() != null ? updatedTemplate.getReport_group() : template.getReport_group());
            template.setParameters(updatedTemplate.getParameters());
            template.setAdditionalInfo(updatedTemplate.getAdditionalInfo());

            // ✅ Add these lines
            template.setRoomId(updatedTemplate.getRoomId());
            template.setRoomName(updatedTemplate.getRoomName());

            return reportTemplateRepo.save(template);
        } else {
            throw new RuntimeException("Template not found with id " + id);
        }
    }
}
