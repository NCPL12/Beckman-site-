package ncpl.bms.reports.repository;

import ncpl.bms.reports.model.dao.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;




public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    List<ReportTemplate> findByName(String name);

}
