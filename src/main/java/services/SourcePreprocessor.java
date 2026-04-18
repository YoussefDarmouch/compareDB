package services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PL/SQL source preprocessing pipeline:
 *   Raw source -> lines -> strip comments -> normalize spaces -> remove empty lines
 */
public final class SourcePreprocessor {

    private SourcePreprocessor() {}

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("--.*$");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    /**
     * Full preprocessing pipeline.
     * Returns a clean list of non-empty, normalised source lines.
     */
    public static List<String> process(String rawSource) {
        return processWithRawLines(rawSource).lines;
    }

    /**
     * Same as {@link #process(String)} but keeps, for each output line, the 1-based line number
     * in the original raw source (before empty-line removal). Use this when diff output must
     * reference real file/DB source positions.
     */
    public static ProcessedWithRawLines processWithRawLines(String rawSource) {
        if (rawSource == null || rawSource.isEmpty()) {
            return new ProcessedWithRawLines(
                    new ArrayList<String>(), new ArrayList<Integer>());
        }
        List<String> lines = splitLines(rawSource);
        lines = stripBlockComments(lines);
        lines = stripLineComments(lines);
        lines = normalizeSpaces(lines);
        List<String> out = new ArrayList<String>(lines.size());
        List<Integer> raw = new ArrayList<Integer>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).isEmpty()) {
                out.add(lines.get(i));
                raw.add(i + 1);
            }
        }
        return new ProcessedWithRawLines(out, raw);
    }

    /** Result of {@link #processWithRawLines(String)}: logical lines + matching raw line numbers. */
    public static final class ProcessedWithRawLines {
        public final List<String> lines;
        /** Same length as {@link #lines}; 1-based index into the original newline-split source. */
        public final List<Integer> rawLineNumbers1Based;

        public ProcessedWithRawLines(List<String> lines, List<Integer> rawLineNumbers1Based) {
            this.lines = lines;
            this.rawLineNumbers1Based = rawLineNumbers1Based;
        }
    }

    // ── Step 1: Split raw source into lines ──────────────────────────────

    static List<String> splitLines(String source) {
        String[] parts = source.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        List<String> result = new ArrayList<String>(parts.length);
        for (String p : parts) {
            result.add(p);
        }
        return result;
    }

    // ── Step 2: Strip block comments  /* ... */  ─────────────────────────

    static List<String> stripBlockComments(List<String> lines) {
        List<String> result = new ArrayList<String>(lines.size());
        boolean inBlock = false;

        for (String line : lines) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < line.length()) {
                if (inBlock) {
                    int end = line.indexOf("*/", i);
                    if (end < 0) {
                        // rest of line is inside comment
                        break;
                    }
                    i = end + 2;
                    inBlock = false;
                } else {
                    int start = line.indexOf("/*", i);
                    if (start < 0) {
                        sb.append(line, i, line.length());
                        break;
                    }
                    sb.append(line, i, start);
                    int end = line.indexOf("*/", start + 2);
                    if (end < 0) {
                        inBlock = true;
                        break;
                    }
                    i = end + 2;
                }
            }
            result.add(sb.toString());
        }
        return result;
    }

    // ── Step 3: Strip single-line comments  --  ──────────────────────────

    static List<String> stripLineComments(List<String> lines) {
        List<String> result = new ArrayList<String>(lines.size());
        for (String line : lines) {
            // Be careful not to strip inside string literals:
            // For simplicity we strip from the first unquoted --
            result.add(removeLineComment(line));
        }
        return result;
    }

    private static String removeLineComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'') {
                inString = !inString;
            } else if (!inString && c == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    // ── Step 4: Normalize whitespace  ────────────────────────────────────

    static List<String> normalizeSpaces(List<String> lines) {
        List<String> result = new ArrayList<String>(lines.size());
        for (String line : lines) {
            result.add(MULTI_SPACE.matcher(line.trim()).replaceAll(" "));
        }
        return result;
    }

    // ── Step 5: Remove empty lines  ─────────────────────────────────────

    static List<String> removeEmptyLines(List<String> lines) {
        List<String> result = new ArrayList<String>();
        for (String line : lines) {
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }
}
