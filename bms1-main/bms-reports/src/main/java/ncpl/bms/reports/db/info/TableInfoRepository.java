package ncpl.bms.reports.db.info;

import ncpl.bms.reports.db.info.TableInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface TableInfoRepository extends JpaRepository<TableInfo, String> {
  //  @Query(value = "show tables", nativeQuery = true)
  @Query(value = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'", nativeQuery = true)
    List<String> findAllTableNames();
}
