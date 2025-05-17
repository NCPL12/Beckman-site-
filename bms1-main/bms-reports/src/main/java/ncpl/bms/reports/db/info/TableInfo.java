package ncpl.bms.reports.db.info;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class TableInfo {
    @Id
    private String tableName;

    public TableInfo() {
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
