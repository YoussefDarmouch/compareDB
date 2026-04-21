package ApiServer.dto;

import java.util.ArrayList;
import java.util.List;

public class SubProgramResult {

    private String name;
    private String type;
    private String status;
    private Integer startLine;
    private Integer endLine;
    private List<Integer> changedLines = new ArrayList<Integer>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public List<Integer> getChangedLines() {
        return changedLines;
    }

    public void setChangedLines(List<Integer> changedLines) {
        this.changedLines = changedLines != null ? changedLines : new ArrayList<Integer>();
    }
}
