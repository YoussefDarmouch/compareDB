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

        public SourceDiffLine(int lineNumber, String oldLine, String newLine) {
            this.lineNumber = lineNumber;
            this.oldLine = oldLine;
            this.newLine = newLine;
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

    private final String objectName;
    private final ObjectType objectType;
    private final ChangeType changeType;
    private final List<SourceDiffLine> sourceDiffs;
    private final List<ParameterDiffLine> parameterDiffs;

    public DbObjectDiff(String objectName, ObjectType objectType, ChangeType changeType) {
        this.objectName = objectName;
        this.objectType = objectType;
        this.changeType = changeType;
        this.sourceDiffs = new ArrayList<>();
        this.parameterDiffs = new ArrayList<>();
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

    public int getSourceChangesCount() {
        return sourceDiffs.size();
    }

    public int getParameterChangesCount() {
        return parameterDiffs.size();
    }
}
