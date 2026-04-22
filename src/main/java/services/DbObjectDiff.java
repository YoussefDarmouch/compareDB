package services;

import java.util.ArrayList;
import java.util.List;

public class DbObjectDiff {

    public enum ObjectType {
        PROCEDURE,
        FUNCTION,
        TRIGGER,
        PACKAGE
    }

    public enum ChangeType {
        ADDED,
        REMOVED,
        MODIFIED
    }

    public static class SourceDiffLine {
        private final int lineNumber;
        private final String oldLine;
        private final String newLine;
        private final String changeType;       // "ADDED", "REMOVED", "MODIFIED" or null (legacy)
        private final String charChangeSummary; // token-level changes for MODIFIED lines

        /** Legacy constructor (backward-compatible). */
        public SourceDiffLine(int lineNumber, String oldLine, String newLine) {
            this(lineNumber, oldLine, newLine, null, null);
        }

        /** Full constructor with LCS diff metadata. */
        public SourceDiffLine(int lineNumber, String oldLine, String newLine,
                              String changeType, String charChangeSummary) {
            this.lineNumber = lineNumber;
            this.oldLine = oldLine;
            this.newLine = newLine;
            this.changeType = changeType;
            this.charChangeSummary = charChangeSummary;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getOldLine() {
            return oldLine;
        }

        public String getNewLine() {
            return newLine;
        }

        public String getChangeType() {
            return changeType;
        }

        public String getCharChangeSummary() {
            return charChangeSummary;
        }
    }

    public static class ParameterDiffLine {
        private final String parameterName;
        private final String oldDataType;
        private final String newDataType;
        private final String oldMode;
        private final String newMode;

        public ParameterDiffLine(String parameterName, String oldDataType, String newDataType, String oldMode, String newMode) {
            this.parameterName = parameterName;
            this.oldDataType = oldDataType;
            this.newDataType = newDataType;
            this.oldMode = oldMode;
            this.newMode = newMode;
        }

        public String getParameterName() {
            return parameterName;
        }

        public String getOldDataType() {
            return oldDataType;
        }

        public String getNewDataType() {
            return newDataType;
        }

        public String getOldMode() {
            return oldMode;
        }

        public String getNewMode() {
            return newMode;
        }
    }

    public static class SubProgramDiff {
        private final String name;
        private final String type;   // "PROCEDURE" or "FUNCTION"
        private final String status; // "DIFFERENT", "SAME", "PRESENT", "MISSING_IN_D1", "MISSING_IN_D2"
        private final int startLine;
        private final int endLine;
        private final List<Integer> changedLineNumbers;

        public SubProgramDiff(String name, String type, String status, int startLine, int endLine, List<Integer> changedLineNumbers) {
            this.name = name;
            this.type = type != null ? type : "PROCEDURE";
            this.status = status;
            this.startLine = startLine;
            this.endLine = endLine;
            this.changedLineNumbers = changedLineNumbers != null ? changedLineNumbers : new ArrayList<Integer>();
        }

        public String getName()                  { return name; }
        public String getType()                  { return type; }
        public String getStatus()                { return status; }
        public int getStartLine()                { return startLine; }
        public int getEndLine()                  { return endLine; }
        public List<Integer> getChangedLines()   { return changedLineNumbers; }
    }

    private final String objectName;
    private final ObjectType objectType;
    private final ChangeType changeType;
    private final List<SourceDiffLine> sourceDiffs;
    private final List<ParameterDiffLine> parameterDiffs;
    private final List<SubProgramDiff> subProgramDiffs;
    private int db1SourceLines;
    private int db2SourceLines;
    private double similarity = -1; // 0.0..1.0 from LCS, -1 = not computed

    public DbObjectDiff(String objectName, ObjectType objectType, ChangeType changeType) {
        this.objectName = objectName;
        this.objectType = objectType;
        this.changeType = changeType;
        this.sourceDiffs = new ArrayList<>();
        this.parameterDiffs = new ArrayList<>();
        this.subProgramDiffs = new ArrayList<>();
    }

    public String getObjectName() {
        return objectName;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public List<SourceDiffLine> getSourceDiffs() {
        return sourceDiffs;
    }

    public List<ParameterDiffLine> getParameterDiffs() {
        return parameterDiffs;
    }

    public List<SubProgramDiff> getSubProgramDiffs() {
        return subProgramDiffs;
    }

    public int getDb1SourceLines() { return db1SourceLines; }
    public void setDb1SourceLines(int n) { this.db1SourceLines = n; }
    public int getDb2SourceLines() { return db2SourceLines; }
    public void setDb2SourceLines(int n) { this.db2SourceLines = n; }

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double s) { this.similarity = s; }

    public int getSourceChangesCount() {
        return sourceDiffs.size();
    }

    public int getParameterChangesCount() {
        return parameterDiffs.size();
    }

    /** Human-readable execution-result for FUNCTION / PROCEDURE / TRIGGER display. */
    public String getExecutionResult() {
        if (objectType == ObjectType.PACKAGE) return "N/A";
        if (changeType == ChangeType.ADDED)   return "ADDED";
        if (changeType == ChangeType.REMOVED) return "REMOVED";
        // MODIFIED
        StringBuilder sb = new StringBuilder();
        if (!sourceDiffs.isEmpty() && !parameterDiffs.isEmpty()) {
            sb.append("SRC(").append(sourceDiffs.size()).append(") + PARAM(").append(parameterDiffs.size()).append(")");
        } else if (!sourceDiffs.isEmpty()) {
            sb.append("SRC_DIFF(").append(sourceDiffs.size()).append(" lines)");
        } else if (!parameterDiffs.isEmpty()) {
            sb.append("PARAM_DIFF(").append(parameterDiffs.size()).append(")");
        } else {
            return "SAME";
        }
        return sb.toString();
    }
}
