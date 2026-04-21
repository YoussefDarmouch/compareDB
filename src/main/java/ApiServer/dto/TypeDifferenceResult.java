package ApiServer.dto;

public class TypeDifferenceResult {

    private String tableName;
    private String columnName;
    private String status;
    private String db1Name;
    private String db1Type;
    private String db2Name;
    private String db2Type;
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

    public String getDb1Name() {
        return db1Name;
    }

    public void setDb1Name(String db1Name) {
        this.db1Name = db1Name;
    }

    public String getDb1Type() {
        return db1Type;
    }

    public void setDb1Type(String db1Type) {
        this.db1Type = db1Type;
    }

    public String getDb2Name() {
        return db2Name;
    }

    public void setDb2Name(String db2Name) {
        this.db2Name = db2Name;
    }

    public String getDb2Type() {
        return db2Type;
    }

    public void setDb2Type(String db2Type) {
        this.db2Type = db2Type;
    }

    public String getLegacyMessage() {
        return legacyMessage;
    }

    public void setLegacyMessage(String legacyMessage) {
        this.legacyMessage = legacyMessage;
    }
}
