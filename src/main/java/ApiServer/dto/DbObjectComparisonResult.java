package ApiServer.dto;

import java.util.ArrayList;
import java.util.List;

public class DbObjectComparisonResult {

    private String name;
    private String objectType;
    private String changeType;
    private String status;
    private String risk;
    private Double similarity;
    private String executionResult;
    private String details;
    private String legacySummary;
    private DatabaseSideSummary db1 = new DatabaseSideSummary();
    private DatabaseSideSummary db2 = new DatabaseSideSummary();
    private List<LineChange> lineChanges = new ArrayList<LineChange>();
    private List<ParameterChange> parameterChanges = new ArrayList<ParameterChange>();
    private List<SubProgramResult> subPrograms = new ArrayList<SubProgramResult>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getLegacySummary() {
        return legacySummary;
    }

    public void setLegacySummary(String legacySummary) {
        this.legacySummary = legacySummary;
    }

    public DatabaseSideSummary getDb1() {
        return db1;
    }

    public void setDb1(DatabaseSideSummary db1) {
        this.db1 = db1 != null ? db1 : new DatabaseSideSummary();
    }

    public DatabaseSideSummary getDb2() {
        return db2;
    }

    public void setDb2(DatabaseSideSummary db2) {
        this.db2 = db2 != null ? db2 : new DatabaseSideSummary();
    }

    public List<LineChange> getLineChanges() {
        return lineChanges;
    }

    public void setLineChanges(List<LineChange> lineChanges) {
        this.lineChanges = lineChanges != null ? lineChanges : new ArrayList<LineChange>();
    }

    public List<ParameterChange> getParameterChanges() {
        return parameterChanges;
    }

    public void setParameterChanges(List<ParameterChange> parameterChanges) {
        this.parameterChanges = parameterChanges != null ? parameterChanges : new ArrayList<ParameterChange>();
    }

    public List<SubProgramResult> getSubPrograms() {
        return subPrograms;
    }

    public void setSubPrograms(List<SubProgramResult> subPrograms) {
        this.subPrograms = subPrograms != null ? subPrograms : new ArrayList<SubProgramResult>();
    }
}
