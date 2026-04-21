package ApiServer.dto;

public class ParameterChange {

    private String name;
    private String oldMode;
    private String newMode;
    private String oldDataType;
    private String newDataType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOldMode() {
        return oldMode;
    }

    public void setOldMode(String oldMode) {
        this.oldMode = oldMode;
    }

    public String getNewMode() {
        return newMode;
    }

    public void setNewMode(String newMode) {
        this.newMode = newMode;
    }

    public String getOldDataType() {
        return oldDataType;
    }

    public void setOldDataType(String oldDataType) {
        this.oldDataType = oldDataType;
    }

    public String getNewDataType() {
        return newDataType;
    }

    public void setNewDataType(String newDataType) {
        this.newDataType = newDataType;
    }
}
