package ApiServer.dto;

public class TableDifferenceResult {

    private String tableName;
    private String status;
    private String missingIn;
    private String legacyMessage;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMissingIn() {
        return missingIn;
    }

    public void setMissingIn(String missingIn) {
        this.missingIn = missingIn;
    }

    public String getLegacyMessage() {
        return legacyMessage;
    }

    public void setLegacyMessage(String legacyMessage) {
        this.legacyMessage = legacyMessage;
    }
}
