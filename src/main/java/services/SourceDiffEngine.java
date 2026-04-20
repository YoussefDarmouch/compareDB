package services;

import java.util.ArrayList;
import java.util.List;

/**
 * LCS-based line-level diff with character-level change detection.
 *
 * Pipeline:
 *   List&lt;String&gt; (old, preprocessed) + List&lt;String&gt; (new, preprocessed)
 *     → LCS alignment
 *     → DiffResult  (similarity score + list of DiffEntry with CharChange objects)
 */
public final class SourceDiffEngine {

    private SourceDiffEngine() {}

    // =====================================================================
    //  Public API
    // =====================================================================

    /**
     * Compare two preprocessed source line-lists using LCS alignment.
     */
    public static DiffResult diff(List<String> oldLines, List<String> newLines) {
        if (oldLines == null) oldLines = new ArrayList<String>();
        if (newLines == null) newLines = new ArrayList<String>();

        // 1. Compute LCS table
        int m = oldLines.size();
        int n = newLines.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (normEq(oldLines.get(i - 1), newLines.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // 2. Back-trace to build the diff entries
        List<DiffEntry> entries = new ArrayList<DiffEntry>();
        int i = m, j = n;
        // Build in reverse, then reverse
        List<DiffEntry> rev = new ArrayList<DiffEntry>();

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && normEq(oldLines.get(i - 1), newLines.get(j - 1))) {
                rev.add(new DiffEntry(DiffType.EQUAL, i, j,
                        oldLines.get(i - 1), newLines.get(j - 1), null));
                i--; j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                rev.add(new DiffEntry(DiffType.ADDED, -1, j,
                        null, newLines.get(j - 1), null));
                j--;
            } else {
                rev.add(new DiffEntry(DiffType.REMOVED, i, -1,
                        oldLines.get(i - 1), null, null));
                i--;
            }
        }

        // Reverse to get forward order
        for (int k = rev.size() - 1; k >= 0; k--) {
            entries.add(rev.get(k));
        }

        // 3. Merge adjacent REMOVED+ADDED pairs into MODIFIED with char-level diff
        List<DiffEntry> merged = mergeModified(entries, oldLines, newLines);

        // 4. Compute similarity
        int lcsLen = dp[m][n];
        int maxLen = Math.max(m, n);
        double similarity = maxLen == 0 ? 1.0 : (double) lcsLen / maxLen;

        return new DiffResult(merged, similarity, m, n, lcsLen);
    }

    // =====================================================================
    //  Merge REMOVED+ADDED → MODIFIED
    // =====================================================================

    private static List<DiffEntry> mergeModified(List<DiffEntry> entries,
                                                  List<String> oldLines,
                                                  List<String> newLines) {
        List<DiffEntry> result = new ArrayList<DiffEntry>();
        int idx = 0;

        while (idx < entries.size()) {
            DiffEntry cur = entries.get(idx);

            // Look for a REMOVED immediately followed by ADDED → treat as MODIFIED
            if (cur.type == DiffType.REMOVED && idx + 1 < entries.size()
                    && entries.get(idx + 1).type == DiffType.ADDED) {

                DiffEntry next = entries.get(idx + 1);
                List<CharChange> charChanges = computeCharDiff(cur.oldLine, next.newLine);
                result.add(new DiffEntry(DiffType.MODIFIED, cur.oldLineNum, next.newLineNum,
                        cur.oldLine, next.newLine, charChanges));
                idx += 2;
                continue;
            }

            result.add(cur);
            idx++;
        }
        return result;
    }

    // =====================================================================
    //  Character-level diff (simple word-token LCS)
    // =====================================================================

    static List<CharChange> computeCharDiff(String oldLine, String newLine) {
        if (oldLine == null) oldLine = "";
        if (newLine == null) newLine = "";

        // Tokenize by non-word boundaries to preserve operators/punctuation
        List<String> oldTok = tokenize(oldLine);
        List<String> newTok = tokenize(newLine);

        int m = oldTok.size(), n = newTok.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int ii = 1; ii <= m; ii++) {
            for (int jj = 1; jj <= n; jj++) {
                if (oldTok.get(ii - 1).equals(newTok.get(jj - 1))) {
                    dp[ii][jj] = dp[ii - 1][jj - 1] + 1;
                } else {
                    dp[ii][jj] = Math.max(dp[ii - 1][jj], dp[ii][jj - 1]);
                }
            }
        }

        // Back-trace
        List<CharChange> changes = new ArrayList<CharChange>();
        int ii = m, jj = n;

        // Collect in reverse
        List<CharChange> rev = new ArrayList<CharChange>();
        while (ii > 0 || jj > 0) {
            if (ii > 0 && jj > 0 && oldTok.get(ii - 1).equals(newTok.get(jj - 1))) {
                rev.add(new CharChange(CharChangeType.EQUAL, oldTok.get(ii - 1), newTok.get(jj - 1)));
                ii--; jj--;
            } else if (jj > 0 && (ii == 0 || dp[ii][jj - 1] >= dp[ii - 1][jj])) {
                rev.add(new CharChange(CharChangeType.ADDED, null, newTok.get(jj - 1)));
                jj--;
            } else {
                rev.add(new CharChange(CharChangeType.REMOVED, oldTok.get(ii - 1), null));
                ii--;
            }
        }
        for (int k = rev.size() - 1; k >= 0; k--) {
            changes.add(rev.get(k));
        }
        return changes;
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                cur.append(c);
            } else {
                if (cur.length() > 0) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
                if (!Character.isWhitespace(c)) {
                    tokens.add(String.valueOf(c));
                }
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens;
    }

    // =====================================================================
    //  Normalization for line equality
    // =====================================================================

    private static boolean normEq(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String v) {
        if (v == null) return "";
        return v.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    // =====================================================================
    //  Data classes
    // =====================================================================

    public enum DiffType {
        EQUAL,     // line unchanged
        ADDED,     // line exists only in new
        REMOVED,   // line exists only in old
        MODIFIED   // line exists in both but differs (has char-level changes)
    }

    public enum CharChangeType {
        EQUAL, ADDED, REMOVED
    }

    public static final class CharChange {
        private final CharChangeType type;
        private final String oldToken;
        private final String newToken;

        public CharChange(CharChangeType type, String oldToken, String newToken) {
            this.type = type;
            this.oldToken = oldToken;
            this.newToken = newToken;
        }

        public CharChangeType getType() { return type; }
        public String getOldToken()     { return oldToken; }
        public String getNewToken()     { return newToken; }

        @Override
        public String toString() {
            switch (type) {
                case ADDED:   return "[+" + newToken + "]";
                case REMOVED: return "[-" + oldToken + "]";
                default:      return oldToken;
            }
        }
    }

    public static final class DiffEntry {
        final DiffType type;
        final int oldLineNum;   // 1-based, -1 if not applicable
        final int newLineNum;   // 1-based, -1 if not applicable
        final String oldLine;
        final String newLine;
        final List<CharChange> charChanges; // null for EQUAL/ADDED/REMOVED

        public DiffEntry(DiffType type, int oldLineNum, int newLineNum,
                         String oldLine, String newLine, List<CharChange> charChanges) {
            this.type = type;
            this.oldLineNum = oldLineNum;
            this.newLineNum = newLineNum;
            this.oldLine = oldLine;
            this.newLine = newLine;
            this.charChanges = charChanges;
        }

        public DiffType getType()                { return type; }
        public int getOldLineNum()               { return oldLineNum; }
        public int getNewLineNum()               { return newLineNum; }
        public String getOldLine()               { return oldLine; }
        public String getNewLine()               { return newLine; }
        public List<CharChange> getCharChanges() { return charChanges; }

        /** Human-readable summary of what tokens changed on a MODIFIED line. */
        public String charChangeSummary() {
            if (charChanges == null || charChanges.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (CharChange cc : charChanges) {
                if (cc.type != CharChangeType.EQUAL) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(cc.toString());
                }
            }
            return sb.toString();
        }
    }

    public static final class DiffResult {
        private final List<DiffEntry> entries;
        private final double similarity;  // 0.0 .. 1.0
        private final int oldLineCount;
        private final int newLineCount;
        private final int lcsLength;

        public DiffResult(List<DiffEntry> entries, double similarity,
                          int oldLineCount, int newLineCount, int lcsLength) {
            this.entries = entries;
            this.similarity = similarity;
            this.oldLineCount = oldLineCount;
            this.newLineCount = newLineCount;
            this.lcsLength = lcsLength;
        }

        public List<DiffEntry> getEntries()    { return entries; }
        public double getSimilarity()          { return similarity; }
        public int getOldLineCount()           { return oldLineCount; }
        public int getNewLineCount()           { return newLineCount; }
        public int getLcsLength()              { return lcsLength; }

        /** Number of non-EQUAL entries. */
        public int getChangeCount() {
            int count = 0;
            for (DiffEntry e : entries) {
                if (e.type != DiffType.EQUAL) count++;
            }
            return count;
        }

        /** True when both sides are identical after preprocessing. */
        public boolean isIdentical() {
            return getChangeCount() == 0;
        }

        /** Lines that are MODIFIED (present in both but differ). */
        public List<DiffEntry> getModifiedEntries() {
            List<DiffEntry> result = new ArrayList<DiffEntry>();
            for (DiffEntry e : entries) {
                if (e.type == DiffType.MODIFIED) result.add(e);
            }
            return result;
        }
    }
}
