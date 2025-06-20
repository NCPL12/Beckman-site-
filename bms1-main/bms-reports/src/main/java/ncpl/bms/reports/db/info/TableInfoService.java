package ncpl.bms.reports.db.info;

import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.db.info.TableInfoRepository;
import ncpl.bms.reports.service.ReportDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TableInfoService implements CommandLineRunner {

    @Autowired
    private TableInfoRepository tableInfoRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * This is a configurable value to identify report specific tables only
     */
    @Value("${report.table.prefix}")
    private String tablePrefix;


//    public List<String> getTables() {
//        log.info("Getting tables inside TableInfoService");
//        List<String> tablesList = tableInfoRepository.findAllTableNames();
//
//        // Remove suffixes starting with '_From' from table names
//        List<String> cleanedTablesList = tablesList.stream()
//                .filter(t -> t.startsWith(tablePrefix))
//                .map(this::removeSuffix)
//                .collect(Collectors.toList());
//
//        return cleanedTablesList;
//    }
//
//    private String removeSuffix(String tableName) {
//        // Customize this method based on the actual suffix patterns
//        if (tableName.contains("_From")) {
//            int suffixIndex = tableName.indexOf("_From");
//            return tableName.substring(0, suffixIndex);
//        }
//        return tableName;
//    }
public List<String> getTables() {
    List<String> tablesList = tableInfoRepository.findAllTableNames();

    List<String> cleanedTablesList = tablesList.stream()
            .filter(t -> t.startsWith(tablePrefix))
            .map(this::removeSuffix)
            .filter(t -> !t.equalsIgnoreCase("EMS_NEW_AUDITHISTORY"))  // <-- Ignored here
            .sorted()
            .collect(Collectors.toList());

    return cleanedTablesList;
}

    private String removeSuffix(String tableName) {
        // Customize this method based on the actual suffix patterns
        if (tableName.contains("_From")) {
            int suffixIndex = tableName.indexOf("_From");
            return tableName.substring(0, suffixIndex);
        }
        return tableName;
    }


    //  This code can be used with command line runner during application startup
        @Override
        public void run(String... args) throws Exception {
            List<String> tableNames = tableInfoRepository.findAllTableNames();
            List<String> filteredTablesList = tableNames.stream()
                    .filter(t -> t.startsWith(tablePrefix))
                    .collect(Collectors.toList());

            createTable("report_data", filteredTablesList);
            for (String tableName : tableNames) {
                log.info("Table name is : {} " , tableName);
                TableInfo tableInfo = new TableInfo();
                tableInfo.setTableName(tableName);
                tableInfoRepository.save(tableInfo);
            }
        }


    public void createTable(String tableName, List<String> columnNames) {
        StringBuilder sql = new StringBuilder();

        // Check if the table exists before creating it
        sql.append("IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '")
                .append(tableName)
                .append("') BEGIN ");

        // Create table statement
        sql.append("CREATE TABLE ").append(tableName).append(" (");
        sql.append("report_id INT NOT NULL IDENTITY(1,1), ");
//        sql.append("timestamp BIGINT");
        sql.append("timestamp datetime2");



        // Append column names to the SQL statement
        for (int i = 0; i < columnNames.size(); i++) {
            sql.append(", ").append(columnNames.get(i)).append(" INT");
        }

        // Add primary key and close the table creation statement
        sql.append(", PRIMARY KEY (report_id));"); // Use a semicolon to terminate the CREATE TABLE statement
        sql.append(" END;"); // Use a semicolon to terminate the IF statement

        // Execute the statement
        jdbcTemplate.execute(sql.toString());
    }


}
