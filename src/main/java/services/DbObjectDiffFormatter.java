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
            int totalLines = leftSide ? diff.getDb1SourceLines() : diff.getDb2SourceLines();
            return leftSide
                    ? (totalLines > 0 ? "Present (" + totalLines + " lines)" : "PRESENT")
                    : "MISSING";
        }
        if (diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {
            int totalLines = leftSide ? diff.getDb1SourceLines() : diff.getDb2SourceLines();
            return leftSide
                    ? "MISSING"
                    : (totalLines > 0 ? "Present (" + totalLines + " lines)" : "PRESENT");
        }

        int totalLines = leftSide ? diff.getDb1SourceLines() : diff.getDb2SourceLines();
        if (totalLines > 0) {
            return "Src " + totalLines + " lines";
        }

        return diff.getSourceChangesCount() > 0
                ? "Src " + diff.getSourceChangesCount() + " lines"
                : "UNCHANGED";
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

        if (!diff.getSubProgramDiffs().isEmpty()) {
            // Build a map of line number -> source diff for quick lookup
            java.util.Map<Integer, DbObjectDiff.SourceDiffLine> diffByLine =
                    new java.util.LinkedHashMap<Integer, DbObjectDiff.SourceDiffLine>();
            for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) {
                diffByLine.put(dl.getLineNumber(), dl);
            }

            StringBuilder sb = new StringBuilder();
            java.util.List<DbObjectDiff.SubProgramDiff> missingInD1 = new java.util.ArrayList<DbObjectDiff.SubProgramDiff>();
            java.util.List<DbObjectDiff.SubProgramDiff> missingInD2 = new java.util.ArrayList<DbObjectDiff.SubProgramDiff>();
            int changedCount = 0;
            int sameCount = 0;
            for (DbObjectDiff.SubProgramDiff sp : diff.getSubProgramDiffs()) {
                if ("MISSING_IN_D1".equals(sp.getStatus())) {
                    missingInD1.add(sp);
                } else if ("MISSING_IN_D2".equals(sp.getStatus())) {
                    missingInD2.add(sp);
                } else if ("DIFFERENT".equals(sp.getStatus())) {
                    changedCount++;
                } else {
                    sameCount++;
                }
            }

            if (diff.getSimilarity() >= 0) {
                sb.append("Similarity: ").append(String.format("%.0f%%", diff.getSimilarity() * 100)).append("\n");
            }
            sb.append("Sub-programs: ")
              .append(changedCount).append(" changed, ")
              .append(sameCount).append(" unchanged");
            if (!missingInD2.isEmpty() || !missingInD1.isEmpty()) {
                sb.append(", ").append(missingInD2.size() + missingInD1.size()).append(" missing");
            }

            if (!missingInD2.isEmpty() || !missingInD1.isEmpty()) {
                sb.append("\nMissing sub-programs:");
                for (DbObjectDiff.SubProgramDiff sp : missingInD2) {
                    sb.append("\n- ")
                      .append(sp.getType()).append(" ")
                      .append(sp.getName()).append(" -> Missing in D2");
                }
                for (DbObjectDiff.SubProgramDiff sp : missingInD1) {
                    sb.append("\n- ")
                      .append(sp.getType()).append(" ")
                      .append(sp.getName()).append(" -> Missing in D1");
                }
            }

            for (DbObjectDiff.SubProgramDiff sp : diff.getSubProgramDiffs()) {
                if ("MISSING_IN_D1".equals(sp.getStatus()) || "MISSING_IN_D2".equals(sp.getStatus())) {
                    continue;
                }
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append("\n");
                sb.append(sp.getName())
                  .append(" -> ")
                  .append(sp.getStatus())
                  .append(" (")
                  .append(sp.getStartLine())
                  .append("-")
                  .append(sp.getEndLine())
                  .append(")");
                if ("DIFFERENT".equals(sp.getStatus()) && !sp.getChangedLines().isEmpty()) {
                    sb.append(" changed: ");
                    appendLineList(sb, sp.getChangedLines());
                    int shown = 0;
                    for (int ln : sp.getChangedLines()) {
                        DbObjectDiff.SourceDiffLine dl = diffByLine.get(ln);
                        if (dl != null && shown < 3) {
                            String tag = dl.getChangeType() != null ? dl.getChangeType() : "DIFF";
                            sb.append("\n  L").append(ln).append(" [").append(tag).append("] ");
                            sb.append(safe(db1Name)).append("= ").append(trimValue(dl.getOldLine()));
                            sb.append(" / ");
                            sb.append(safe(db2Name)).append("= ").append(trimValue(dl.getNewLine()));
                            if (dl.getCharChangeSummary() != null) {
                                sb.append("\n    tokens: ").append(trimValue(dl.getCharChangeSummary()));
                            }
                            shown++;
                        }
                    }
                    if (sp.getChangedLines().size() > 3) {
                        sb.append("\n  ... and ").append(sp.getChangedLines().size() - 3).append(" more lines");
                    }
                }
            }
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        if (diff.getSimilarity() >= 0) {
            sb.append("Similarity: ").append(String.format("%.0f%%", diff.getSimilarity() * 100)).append("\n");
        }
        if (!diff.getSourceDiffs().isEmpty()) {
            sb.append("Changed lines: ");
            appendLineList(sb, extractSourceLineNumbers(diff.getSourceDiffs()));
            // Show actual diffs (up to 3)
            int shown = 0;
            for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) {
                if (shown >= 3) break;
                String tag = dl.getChangeType() != null ? dl.getChangeType() : "DIFF";
                sb.append("\n  L").append(dl.getLineNumber()).append(" [").append(tag).append("] ");
                sb.append(safe(db1Name)).append("= ").append(trimValue(dl.getOldLine()));
                sb.append(" / ");
                sb.append(safe(db2Name)).append("= ").append(trimValue(dl.getNewLine()));
                if (dl.getCharChangeSummary() != null) {
                    sb.append("\n    tokens: ").append(trimValue(dl.getCharChangeSummary()));
                }
                shown++;
            }
            if (diff.getSourceDiffs().size() > 3) {
                sb.append("\n  ... and ").append(diff.getSourceDiffs().size() - 3).append(" more");
            }
        }

        if (!diff.getParameterDiffs().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Parameters:");
            for (int i = 0; i < diff.getParameterDiffs().size(); i++) {
                if (i > 0) sb.append("\n");
                DbObjectDiff.ParameterDiffLine line = diff.getParameterDiffs().get(i);
                sb.append("- ")
                  .append(safe(line.getParameterName()))
                  .append(": ")
                  .append(formatTransition(line.getOldMode(), line.getOldDataType(),
                                          line.getNewMode(), line.getNewDataType()));
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

    private static java.util.List<Integer> extractSourceLineNumbers(List<DbObjectDiff.SourceDiffLine> diffs) {
        java.util.List<Integer> numbers = new java.util.ArrayList<Integer>();
        for (DbObjectDiff.SourceDiffLine line : diffs) {
            numbers.add(line.getLineNumber());
        }
        return numbers;
    }

    private static void appendLineList(StringBuilder sb, java.util.List<Integer> nums) {
        if (nums == null || nums.isEmpty()) {
            return;
        }

        java.util.List<Integer> sorted = new java.util.ArrayList<Integer>(nums);
        java.util.Collections.sort(sorted);

        int start = sorted.get(0);
        int prev = start;
        for (int i = 1; i <= sorted.size(); i++) {
            int cur = i < sorted.size() ? sorted.get(i) : -1;
            if (cur == prev + 1) {
                prev = cur;
                continue;
            }

            if (sb.length() > 0) {
                char last = sb.charAt(sb.length() - 1);
                if (last != ' ' && last != ':') {
                    sb.append(", ");
                }
            }

            if (start == prev) {
                sb.append(start);
            } else {
                sb.append(start).append("-").append(prev);
            }

            start = cur;
            prev = cur;
        }
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

    private static String trimValue(String line) {
        if (line == null) return "<none>";
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return "<empty>";
        if (trimmed.length() > 60) return trimmed.substring(0, 57) + "...";
        return trimmed;
    }
}
