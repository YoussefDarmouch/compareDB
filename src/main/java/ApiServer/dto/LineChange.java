package ApiServer.dto;

public class LineChange {

    private Integer lineNumber;
    private String changeType;
    private String oldLine;
    private String newLine;
    private String tokenSummary;

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getOldLine() {
        return oldLine;
    }

    public void setOldLine(String oldLine) {
        this.oldLine = oldLine;
    }

    public String getNewLine() {
        return newLine;
    }

    public void setNewLine(String newLine) {
        this.newLine = newLine;
    }

    public String getTokenSummary() {
        return tokenSummary;
    }

    public void setTokenSummary(String tokenSummary) {
        this.tokenSummary = tokenSummary;
    }
}
