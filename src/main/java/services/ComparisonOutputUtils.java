package services;

import java.util.Locale;

public final class ComparisonOutputUtils {

    private ComparisonOutputUtils() {
    }

    public static String[] comparisonHeaders(String objectHeader, String db1Name, String db2Name) {
        return new String[]{objectHeader, "STATUS", db1Name, db2Name, "IMPACT", "DETAILS"};
    }

    public static String[] tableRow(String tableName,
                                    boolean existsInDb1,
                                    boolean existsInDb2,
                                    String db1Name,
                                    String db2Name) {
        String status;
        String impact;
        String details;

        if (existsInDb1 && existsInDb2) {
            status = "SHARED";
            impact = "LOW";
            details = "Exists in both databases";
        } else if (existsInDb1) {
            status = "ONLY IN " + db1Name;
            impact = "HIGH";
            details = "Missing in " + db2Name;
        } else {
            status = "ONLY IN " + db2Name;
            impact = "HIGH";
            details = "Missing in " + db1Name;
        }

        return new String[]{
                tableName,
                status,
                existsInDb1 ? "PRESENT" : "MISSING",
                existsInDb2 ? "PRESENT" : "MISSING",
                impact,
                details
        };
    }

    public static String[] typeRow(String tableName,
                                   String columnName,
                                   String db1Type,
                                   String db2Type) {
        String objectName = tableName + "." + columnName;
        String impact = classifyTypeImpact(db1Type, db2Type);
        return new String[]{
                objectName,
                "DIFFERENT",
                safe(db1Type),
                safe(db2Type),
                impact,
                "Column type differs"
        };
    }

    public static String[] columnRow(String tableName,
                                     String columnName,
                                     String existsIn,
                                     String db1Name,
                                     String db2Name) {
        boolean inDb1 = existsIn != null && existsIn.contains(db1Name);
        boolean inDb2 = existsIn != null && existsIn.contains(db2Name);
        String status = "ONLY IN " + (inDb1 ? db1Name : db2Name);

        return new String[]{
                tableName + "." + columnName,
                status,
                inDb1 ? "PRESENT" : "MISSING",
                inDb2 ? "PRESENT" : "MISSING",
                "HIGH",
                "Column exists only in one database"
        };
    }

    public static String[] dataRow(String tableName,
                                   String rowId,
                                   String columnName,
                                   String db1Value,
                                   String db2Value) {
        String impact = classifyDataImpact(db1Value, db2Value);
        return new String[]{
                tableName + "[" + rowId + "]." + columnName,
                "DIFFERENT",
                safeValue(db1Value),
                safeValue(db2Value),
                impact,
                "Row " + rowId + ", column " + columnName + " differs"
        };
    }

    public static String[] objectInfoRow(String message) {
        return new String[]{"-", "INFO", "-", "-", "LOW", safe(message)};
    }

    public static String impactForObjectStatus(String status) {
        if (status == null) {
            return "LOW";
        }

        String normalized = status.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ONLY IN")) {
            return "HIGH";
        }
        if (normalized.startsWith("DIFFERENT")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String classifyTypeImpact(String db1Type, String db2Type) {
        String leftBase = baseType(db1Type);
        String rightBase = baseType(db2Type);
        if (!leftBase.equals(rightBase)) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private static String classifyDataImpact(String db1Value, String db2Value) {
        boolean leftMissing = isMissingValue(db1Value);
        boolean rightMissing = isMissingValue(db2Value);
        if (leftMissing != rightMissing) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private static boolean isMissingValue(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.trim();
        return normalized.isEmpty() || "NULL".equalsIgnoreCase(normalized) || "null".equals(normalized);
    }

    private static String baseType(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        int idx = normalized.indexOf('(');
        return idx >= 0 ? normalized.substring(0, idx) : normalized;
    }

    private static String safeValue(String value) {
        if (value == null) {
            return "NULL";
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? "NULL" : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}