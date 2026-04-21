package ApiServer.dto;

public class DataDifferenceResult {

    private String tableName;
    private String rowId;
    private String columnName;
    private String status;
    private String db1Name;
    private String db1Value;
    private String db2Name;
    private String db2Value;
    private String legacyMessage;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
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

    public String getDb1Name() {
        return db1Name;
    }

    public void setDb1Name(String db1Name) {
        this.db1Name = db1Name;
    }

    public String getDb1Value() {
        return db1Value;
    }

    public void setDb1Value(String db1Value) {
        this.db1Value = db1Value;
    }

    public String getDb2Name() {
        return db2Name;
    }

    public void setDb2Name(String db2Name) {
        this.db2Name = db2Name;
    }

    public String getDb2Value() {
        return db2Value;
    }

    public void setDb2Value(String db2Value) {
        this.db2Value = db2Value;
    }

    public String getLegacyMessage() {
        return legacyMessage;
    }

    public void setLegacyMessage(String legacyMessage) {
        this.legacyMessage = legacyMessage;
    }
}
