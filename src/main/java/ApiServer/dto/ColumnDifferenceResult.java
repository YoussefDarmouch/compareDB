package ApiServer.dto;

public class ColumnDifferenceResult {

    private String tableName;
    private String columnName;
    private String status;
    private String onlyIn;
    private String legacyMessage;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOnlyIn() {
        return onlyIn;
    }

    public void setOnlyIn(String onlyIn) {
        this.onlyIn = onlyIn;
    }

    public String getLegacyMessage() {
        return legacyMessage;
    }

    public void setLegacyMessage(String legacyMessage) {
        this.legacyMessage = legacyMessage;
    }
}
