package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DatabaseFunctionService {
    public static class TriggerInfo {
        private final String eventObjectTable;
        private final String actionTiming;
        private final String eventManipulation;
        private final String actionStatement;

        public TriggerInfo(String eventObjectTable, String actionTiming, String eventManipulation, String actionStatement) {
            this.eventObjectTable = eventObjectTable;
            this.actionTiming = actionTiming;
            this.eventManipulation = eventManipulation;
            this.actionStatement = actionStatement;
        }

        public boolean sameAs(TriggerInfo other) {
            if (other == null) {
                return false;
            }
            return safeEquals(eventObjectTable, other.eventObjectTable)
                    && safeEquals(actionTiming, other.actionTiming)
                    && safeEquals(eventManipulation, other.eventManipulation)
                    && safeEquals(actionStatement, other.actionStatement);
        }

        public String describe() {
            return "table=" + safeString(eventObjectTable)
                    + ", timing=" + safeString(actionTiming)
                    + ", event=" + safeString(eventManipulation)
                    + ", statement=" + safeString(actionStatement);
        }

        private boolean safeEquals(String left, String right) {
            return safeString(left).equalsIgnoreCase(safeString(right));
        }

        private String safeString(String value) {
            return value == null ? "" : value.trim();
        }
    }

    public static class FunctionExecutionResult {
        private final boolean executable;
        private final boolean success;
        private final String value;
        private final String message;

        public FunctionExecutionResult(boolean executable, boolean success, String value, String message) {
            this.executable = executable;
            this.success = success;
            this.value = value;
            this.message = message;
        }

        public boolean isExecutable() {
            return executable;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getValue() {
            return value;
        }

        public String getMessage() {
            return message;
        }
    }

    public static List<String> checkFun(String database) {
        return checkFun(DbConnectionFactory.getDefaultConfig(database));
    }

    public static List<String> checkFun(DbConnectionFactory.DbConfig dbConfig) {

        List<String> functions = new ArrayList<>();
        String database = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             PreparedStatement ps = connection.prepareStatement(
                 "SELECT ROUTINE_NAME " +
                     "FROM INFORMATION_SCHEMA.ROUTINES " +
                     "WHERE ROUTINE_SCHEMA = ? " +
                     "AND ROUTINE_TYPE = 'FUNCTION'"
             )) {

            ps.setString(1, database);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                functions.add(rs.getString("ROUTINE_NAME"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return functions;
    }

    public static int getFunctionParameterCount(String database, String functionName) {
        return getRoutineParameterCount(DbConnectionFactory.getDefaultConfig(database), functionName);
    }

    public static int getFunctionParameterCount(DbConnectionFactory.DbConfig dbConfig, String functionName) {
        return getRoutineParameterCount(dbConfig, functionName);
    }

    public static List<String> getProcedureNames(String database) {
        return getRoutineNames(DbConnectionFactory.getDefaultConfig(database), "PROCEDURE");
    }

    public static List<String> getProcedureNames(DbConnectionFactory.DbConfig dbConfig) {
        return getRoutineNames(dbConfig, "PROCEDURE");
    }

    public static int getProcedureParameterCount(String database, String procedureName) {
        return getRoutineParameterCount(DbConnectionFactory.getDefaultConfig(database), procedureName);
    }

    public static int getProcedureParameterCount(DbConnectionFactory.DbConfig dbConfig, String procedureName) {
        return getRoutineParameterCount(dbConfig, procedureName);
    }

    public static Set<String> getTriggerNames(String database) {
        return getTriggerNames(DbConnectionFactory.getDefaultConfig(database));
    }

    public static Set<String> getTriggerNames(DbConnectionFactory.DbConfig dbConfig) {
        Set<String> triggerNames = new TreeSet<String>();
        String database = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT TRIGGER_NAME FROM INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_SCHEMA = ?"
             )) {

            ps.setString(1, database);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                triggerNames.add(rs.getString("TRIGGER_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return triggerNames;
    }

    public static TriggerInfo getTriggerInfo(String database, String triggerName) {
        return getTriggerInfo(DbConnectionFactory.getDefaultConfig(database), triggerName);
    }

    public static TriggerInfo getTriggerInfo(DbConnectionFactory.DbConfig dbConfig, String triggerName) {
        String database = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT EVENT_OBJECT_TABLE, ACTION_TIMING, EVENT_MANIPULATION, ACTION_STATEMENT " +
                             "FROM INFORMATION_SCHEMA.TRIGGERS " +
                             "WHERE TRIGGER_SCHEMA = ? AND TRIGGER_NAME = ?"
             )) {

            ps.setString(1, database);
            ps.setString(2, triggerName);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new TriggerInfo(
                        rs.getString("EVENT_OBJECT_TABLE"),
                        rs.getString("ACTION_TIMING"),
                        rs.getString("EVENT_MANIPULATION"),
                        rs.getString("ACTION_STATEMENT")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static List<String> getRoutineNames(DbConnectionFactory.DbConfig dbConfig, String routineType) {
        List<String> routineNames = new ArrayList<String>();
        String database = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT ROUTINE_NAME " +
                             "FROM INFORMATION_SCHEMA.ROUTINES " +
                             "WHERE ROUTINE_SCHEMA = ? " +
                             "AND ROUTINE_TYPE = ?"
             )) {

            ps.setString(1, database);
            ps.setString(2, routineType);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                routineNames.add(rs.getString("ROUTINE_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return routineNames;
    }

    private static int getRoutineParameterCount(DbConnectionFactory.DbConfig dbConfig, String routineName) {
        String database = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT COUNT(*) AS param_count " +
                             "FROM INFORMATION_SCHEMA.PARAMETERS " +
                             "WHERE SPECIFIC_SCHEMA = ? " +
                             "AND SPECIFIC_NAME = ? " +
                             "AND ORDINAL_POSITION > 0"
             )) {

            ps.setString(1, database);
            ps.setString(2, routineName);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("param_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static FunctionExecutionResult executeFunctionForComparison(String database, String functionName, int parameterCount) {
        return executeFunctionForComparison(DbConnectionFactory.getDefaultConfig(database), functionName, parameterCount);
    }

    public static FunctionExecutionResult executeFunctionForComparison(DbConnectionFactory.DbConfig dbConfig, String functionName, int parameterCount) {
        if (parameterCount > 1) {
            return new FunctionExecutionResult(
                    false,
                    false,
                    null,
                    "Skipped output check (" + parameterCount + " parameters)"
            );
        }

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig)) {
            if (parameterCount == 0) {
                String sql = "SELECT " + functionName + "() AS result";
                try (PreparedStatement ps = connection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new FunctionExecutionResult(true, true, rs.getString("result"), "");
                    }
                    return new FunctionExecutionResult(true, false, null, "No output returned");
                }
            }

            String sql = "SELECT " + functionName + "(?) AS result";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new FunctionExecutionResult(true, true, rs.getString("result"), "");
                    }
                    return new FunctionExecutionResult(true, false, null, "No output returned");
                }
            }
        } catch (SQLException e) {
            return new FunctionExecutionResult(true, false, null, e.getMessage());
        }
    }
}