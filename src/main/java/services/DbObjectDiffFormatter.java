package services;

import java.util.List;

public final class DbObjectDiffFormatter {

    private DbObjectDiffFormatter() {}

    public static String formatBlock(DbObjectDiff diff) {

        StringBuilder sb = new StringBuilder();

        sb.append("OBJECT: ")
                .append(safe(diff.getObjectType() != null ? diff.getObjectType().name() : null))
                .append(" ")
                .append(safe(diff.getObjectName()))
                .append("\n");

        sb.append("TYPE: ")
                .append(safe(diff.getObjectType() != null ? diff.getObjectType().name() : null))
                .append("\n");

        sb.append("STATUS: ")
                .append(safe(diff.getChangeType() != null ? diff.getChangeType().name() : null))
                .append("\n\n");

        sb.append("--- SOURCE DIFF ---\n");
        appendSourceDiff(sb, diff.getSourceDiffs());

        sb.append("\n--- PARAMETERS DIFF ---\n");
        appendParameterDiff(sb, diff.getParameterDiffs());

        sb.append("\n--- SUMMARY ---\n");
        sb.append("Source changes: ").append(diff.getSourceChangesCount()).append("\n");
        sb.append("Parameter changes: ").append(diff.getParameterChangesCount()).append("\n");

        return sb.toString();
    }

    public static String formatSummaryRow(DbObjectDiff diff, String db1Name, String db2Name) {
        return safe(diff.getObjectName())
                + "|"
                + resolveStatus(diff, db1Name, db2Name)
                + "|"
                + buildDbSummary(diff, true)
                + "|"
                + buildDbSummary(diff, false)
                + "|"
                + buildImpact(diff)
                + "|"
                + buildDetails(diff, db1Name, db2Name);
    }

    // =========================================================
    // SOURCE DIFF
    // =========================================================
    private static void appendSourceDiff(StringBuilder sb,
                                         List<DbObjectDiff.SourceDiffLine> sourceLines) {

        if (sourceLines == null || sourceLines.isEmpty()) {
            sb.append("(no source changes)\n");
            return;
        }

        for (DbObjectDiff.SourceDiffLine line : sourceLines) {

            sb.append("LINE ").append(line.getLineNumber()).append(":\n");

            sb.append("- ")
                    .append(safe(line.getOldLine()))
                    .append("\n");

            sb.append("+ ")
                    .append(safe(line.getNewLine()))
                    .append("\n\n");
        }
    }

    // =========================================================
    // PARAMETER DIFF
    // =========================================================
    private static void appendParameterDiff(StringBuilder sb,
                                            List<DbObjectDiff.ParameterDiffLine> parameterLines) {

        if (parameterLines == null || parameterLines.isEmpty()) {
            sb.append("(no parameter changes)\n");
            return;
        }

        for (DbObjectDiff.ParameterDiffLine line : parameterLines) {

            String name = safe(line.getParameterName());
            String oldMode = safe(line.getOldMode());
            String newMode = safe(line.getNewMode());
            String oldType = safe(line.getOldDataType());
            String newType = safe(line.getNewDataType());

            sb.append("- ")
                    .append(name)
                    .append(" ")
                    .append(oldMode)
                    .append(" ")
                    .append(oldType)
                    .append("\n");

            sb.append("+ ")
                    .append(name)
                    .append(" ")
                    .append(newMode)
                    .append(" ")
                    .append(newType)
                    .append("\n\n");
        }
    }

    // =========================================================
    // SAFE STRING
    // =========================================================
    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "<none>";
        }
        return value.trim();
    }

    private static String resolveStatus(DbObjectDiff diff, String db1Name, String db2Name) {
        if (diff == null || diff.getChangeType() == null) {
            return "UNKNOWN";
        }

        switch (diff.getChangeType()) {
            case ADDED:
                return "ONLY IN " + safe(db1Name);
            case REMOVED:
                return "ONLY IN " + safe(db2Name);
            case MODIFIED:
                return "DIFFERENT";
            default:
                return diff.getChangeType().name();
        }
    }

    private static String buildDbSummary(DbObjectDiff diff, boolean leftSide) {
        if (diff.getChangeType() == DbObjectDiff.ChangeType.ADDED) {
            return leftSide ? "PRESENT" : "MISSING";
        }
        if (diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {
            return leftSide ? "MISSING" : "PRESENT";
        }

        StringBuilder sb = new StringBuilder();
        if (diff.getSourceChangesCount() > 0) {
            sb.append("Src ").append(diff.getSourceChangesCount()).append(" lines");
        }

        String params = buildParameterSummary(diff, leftSide);
        if (!"-".equals(params)) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append("Params ").append(params);
        }

        if (sb.length() == 0) {
            return "UNCHANGED";
        }
        return sb.toString();
    }

    private static String buildParameterSummary(DbObjectDiff diff, boolean leftSide) {
        if (diff.getParameterDiffs().isEmpty()) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < diff.getParameterDiffs().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            DbObjectDiff.ParameterDiffLine line = diff.getParameterDiffs().get(i);
            sb.append(safe(line.getParameterName()));

            String mode = leftSide ? safeMode(line.getOldMode()) : safeMode(line.getNewMode());
            String type = leftSide ? safeType(line.getOldDataType()) : safeType(line.getNewDataType());
            if (!mode.isEmpty() || !type.isEmpty()) {
                sb.append(":");
                if (!mode.isEmpty()) {
                    sb.append(mode);
                }
                if (!type.isEmpty()) {
                    if (!mode.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(type);
                }
            }
        }
        return sb.toString().replace('|', '/');
    }

    private static String buildImpact(DbObjectDiff diff) {
        if (diff.getChangeType() == DbObjectDiff.ChangeType.ADDED || diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {
            return "HIGH";
        }

        if (diff.getSourceChangesCount() >= 5 || diff.getParameterChangesCount() >= 2) {
            return "HIGH";
        }

        if (diff.getSourceChangesCount() > 0 || diff.getParameterChangesCount() > 0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private static String buildDetails(DbObjectDiff diff, String db1Name, String db2Name) {
        if (diff.getChangeType() == DbObjectDiff.ChangeType.ADDED) {
            return "Object exists only in " + safe(db1Name);
        }
        if (diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {
            return "Object exists only in " + safe(db2Name);
        }

        StringBuilder sb = new StringBuilder();
        if (!diff.getSourceDiffs().isEmpty()) {
            sb.append("Changed lines ");
            for (int i = 0; i < diff.getSourceDiffs().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(diff.getSourceDiffs().get(i).getLineNumber());
            }
        }

        if (!diff.getParameterDiffs().isEmpty()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append("Params ");
            for (int i = 0; i < diff.getParameterDiffs().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                DbObjectDiff.ParameterDiffLine line = diff.getParameterDiffs().get(i);
                sb.append(safe(line.getParameterName()))
                        .append(" ")
                        .append(formatTransition(line.getOldMode(), line.getOldDataType(), line.getNewMode(), line.getNewDataType()));
            }
        }

        if (sb.length() == 0) {
            return "No detailed differences captured";
        }

        return sb.toString().replace('|', '/');
    }

    private static String formatTransition(String oldMode, String oldType, String newMode, String newType) {
        return compactSignature(oldMode, oldType) + " -> " + compactSignature(newMode, newType);
    }

    private static String compactSignature(String mode, String type) {
        String safeMode = safeMode(mode);
        String safeType = safeType(type);
        if (safeMode.isEmpty()) {
            return safeType;
        }
        if (safeType.isEmpty()) {
            return safeMode;
        }
        return safeMode + " " + safeType;
    }

    private static String safeMode(String value) {
        if (value == null || value.trim().isEmpty() || "<none>".equals(safe(value))) {
            return "";
        }
        return value.trim();
    }

    private static String safeType(String value) {
        if (value == null || value.trim().isEmpty() || "<none>".equals(safe(value))) {
            return "";
        }
        return value.trim();
    }
}