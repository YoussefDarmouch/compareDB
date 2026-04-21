package ApiServer.dto;

public class DatabaseSideSummary {

    private String summary;
    private boolean present;
    private Integer sourceLineCount;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public Integer getSourceLineCount() {
        return sourceLineCount;
    }

    public void setSourceLineCount(Integer sourceLineCount) {
        this.sourceLineCount = sourceLineCount;
    }
}
