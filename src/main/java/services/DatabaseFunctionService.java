package services;

import java.sql.*;
import java.util.*;

public class DatabaseFunctionService {

    // =========================================================
    // ROUTINE PARAMETER
    // =========================================================
    public static class RoutineParameter {
        private final String name;
        private final String dataType;
        private final String mode;

        public RoutineParameter(String name, String dataType, String mode) {
            this.name = name;
            this.dataType = dataType;
            this.mode = mode;
        }

        public String getName() {
            return name;
        }

        public String getDataType() {
            return dataType;
        }

        public String getMode() {
            return mode;
        }
    }

    // =========================================================
    // ENGINE SWITCH HELPERS
    // =========================================================

    public static Set<String> getObjectNames(DbConnectionFactory.DbConfig config,
                                             DbObjectDiff.ObjectType type) {

        if (config.getEngine() == DbConnectionFactory.DbEngine.MYSQL) {
            return getObjectNamesMysql(config, type);
        } else {
            return getObjectNamesOracle(config, type);
        }
    }

    public static String getObjectSource(DbConnectionFactory.DbConfig config,
                                         DbObjectDiff.ObjectType type,
                                         String name) {

        if (config.getEngine() == DbConnectionFactory.DbEngine.MYSQL) {
            return getObjectSourceMysql(config, type, name);
        } else {
            return getObjectSourceOracle(config, type, name);
        }
    }

    /**
     * Oracle {@code USER_SOURCE} text for {@code TYPE = 'PACKAGE BODY'} only (no package spec).
     * Returns {@code null} when the engine is not Oracle, on error, or when the body object does not exist.
     */
    public static String getPackageBodySource(DbConnectionFactory.DbConfig config, String name) {
        if (config.getEngine() != DbConnectionFactory.DbEngine.ORACLE) {
            return null;
        }
        try (Connection conn = DbConnectionFactory.getConnection(config)) {
            return getOracleSourceByType(conn, name, "PACKAGE BODY");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, RoutineParameter> getObjectParameters(
            DbConnectionFactory.DbConfig config,
            DbObjectDiff.ObjectType type,
            String name) {

        if (type == DbObjectDiff.ObjectType.TRIGGER || type == DbObjectDiff.ObjectType.PACKAGE) {
            return new LinkedHashMap<String, RoutineParameter>();
        }

        if (config.getEngine() == DbConnectionFactory.DbEngine.MYSQL) {
            return getRoutineParametersMysql(config, name);
        } else {
            return getRoutineParametersOracle(config, name);
        }
    }

    // =========================================================
    // MYSQL IMPLEMENTATION
    // =========================================================

    private static Set<String> getObjectNamesMysql(DbConnectionFactory.DbConfig config,
                                                   DbObjectDiff.ObjectType type) {

        Set<String> list = new TreeSet<String>();

        try (Connection conn = DbConnectionFactory.getConnection(config)) {

            String sql;

            if (type == DbObjectDiff.ObjectType.TRIGGER) {
                sql = "SELECT TRIGGER_NAME FROM INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_SCHEMA=?";
            } else {
                sql = "SELECT ROUTINE_NAME FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA=? AND ROUTINE_TYPE=?";
            }

            PreparedStatement ps = conn.prepareStatement(sql);

            if (type == DbObjectDiff.ObjectType.TRIGGER) {
                ps.setString(1, config.getDatabaseName());
            } else {
                ps.setString(1, config.getDatabaseName());
                ps.setString(2, type == DbObjectDiff.ObjectType.FUNCTION ? "FUNCTION" : "PROCEDURE");
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(rs.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private static String getObjectSourceMysql(DbConnectionFactory.DbConfig config,
                                               DbObjectDiff.ObjectType type,
                                               String name) {

        try (Connection conn = DbConnectionFactory.getConnection(config)) {

            String sql;

            if (type == DbObjectDiff.ObjectType.TRIGGER) {
                sql = "SELECT ACTION_STATEMENT FROM INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_SCHEMA=? AND TRIGGER_NAME=?";
            } else {
                sql = "SELECT ROUTINE_DEFINITION FROM INFORMATION_SCHEMA.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA=? AND ROUTINE_NAME=? AND ROUTINE_TYPE=?";
            }

            PreparedStatement ps = conn.prepareStatement(sql);

            if (type == DbObjectDiff.ObjectType.TRIGGER) {
                ps.setString(1, config.getDatabaseName());
                ps.setString(2, name);
            } else {
                ps.setString(1, config.getDatabaseName());
                ps.setString(2, name);
                ps.setString(3, type == DbObjectDiff.ObjectType.FUNCTION ? "FUNCTION" : "PROCEDURE");
            }

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Map<String, RoutineParameter> getRoutineParametersMysql(
            DbConnectionFactory.DbConfig config,
            String name) {

        Map<String, RoutineParameter> map = new LinkedHashMap<String, RoutineParameter>();

        try (Connection conn = DbConnectionFactory.getConnection(config)) {

                String sql = "SELECT PARAMETER_NAME, DATA_TYPE, DTD_IDENTIFIER, PARAMETER_MODE " +
                    "FROM INFORMATION_SCHEMA.PARAMETERS " +
                    "WHERE SPECIFIC_SCHEMA=? AND SPECIFIC_NAME=? ORDER BY ORDINAL_POSITION";

            PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, config.getDatabaseName());
                ps.setString(2, name);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String pname = rs.getString(1);
                if (pname == null) pname = "RETURN";

                map.put(pname,
                        new RoutineParameter(
                                pname,
                        normalizeMysqlParamType(rs.getString(3), rs.getString(2)),
                        rs.getString(4)
                        ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }

    // =========================================================
    // ORACLE IMPLEMENTATION
    // =========================================================

    private static Set<String> getObjectNamesOracle(DbConnectionFactory.DbConfig config,
                                                    DbObjectDiff.ObjectType type) {

        Set<String> list = new TreeSet<String>();

        try (Connection conn = DbConnectionFactory.getConnection(config)) {

            String sql;

            if (type == DbObjectDiff.ObjectType.TRIGGER) {
                sql = "SELECT TRIGGER_NAME FROM USER_TRIGGERS";
            } else {
                sql = "SELECT OBJECT_NAME FROM USER_OBJECTS WHERE OBJECT_TYPE=?";
            }

            PreparedStatement ps = conn.prepareStatement(sql);

            if (type != DbObjectDiff.ObjectType.TRIGGER) {
                ps.setString(1, oracleObjectType(type));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(rs.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private static String getObjectSourceOracle(DbConnectionFactory.DbConfig config,
                                                DbObjectDiff.ObjectType type,
                                                String name) {
        try (Connection conn = DbConnectionFactory.getConnection(config)) {

            if (type == DbObjectDiff.ObjectType.PACKAGE) {
                String spec = getOracleSourceByType(conn, name, "PACKAGE");
                String body = getOracleSourceByType(conn, name, "PACKAGE BODY");

                if (spec == null && body == null) {
                    return null;
                }

                StringBuilder merged = new StringBuilder();
                if (spec != null) {
                    merged.append(spec);
                }
                if (body != null) {
                    if (merged.length() > 0) {
                        merged.append("\n");
                    }
                    merged.append("-- PACKAGE BODY --\n").append(body);
                }
                return merged.toString();
            }

            return getOracleSourceByType(conn, name, oracleSourceType(type));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Map<String, RoutineParameter> getRoutineParametersOracle(
            DbConnectionFactory.DbConfig config,
            String name) {

        Map<String, RoutineParameter> map = new LinkedHashMap<String, RoutineParameter>();

        try (Connection conn = DbConnectionFactory.getConnection(config)) {

                String sql = "SELECT ARGUMENT_NAME, DATA_TYPE, IN_OUT, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, TYPE_NAME " +
                    "FROM USER_ARGUMENTS WHERE OBJECT_NAME=? ORDER BY POSITION";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name.toUpperCase());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String pname = rs.getString(1);
                if (pname == null) pname = "RETURN";

                map.put(pname,
                        new RoutineParameter(
                                pname,
                        normalizeOracleParamType(
                            rs.getString(2),
                            rs.getObject(4),
                            rs.getObject(5),
                            rs.getObject(6),
                            rs.getString(7)
                        ),
                                rs.getString(3)
                        ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }

    private static String oracleObjectType(DbObjectDiff.ObjectType type) {
        switch (type) {
            case FUNCTION:
                return "FUNCTION";
            case PROCEDURE:
                return "PROCEDURE";
            case PACKAGE:
                return "PACKAGE";
            default:
                return "TRIGGER";
        }
    }

    private static String oracleSourceType(DbObjectDiff.ObjectType type) {
        switch (type) {
            case FUNCTION:
                return "FUNCTION";
            case PROCEDURE:
                return "PROCEDURE";
            case TRIGGER:
                return "TRIGGER";
            case PACKAGE:
                return "PACKAGE";
            default:
                return "PROCEDURE";
        }
    }

    private static String normalizeMysqlParamType(String dtdIdentifier, String fallbackDataType) {
        if (dtdIdentifier != null && !dtdIdentifier.trim().isEmpty()) {
            return dtdIdentifier.trim().toUpperCase(Locale.ROOT);
        }
        if (fallbackDataType != null && !fallbackDataType.trim().isEmpty()) {
            return fallbackDataType.trim().toUpperCase(Locale.ROOT);
        }
        return "UNKNOWN";
    }

    private static String normalizeOracleParamType(String dataType,
                                                   Object dataLengthObj,
                                                   Object dataPrecisionObj,
                                                   Object dataScaleObj,
                                                   String typeName) {
        String base = dataType == null ? "UNKNOWN" : dataType.trim().toUpperCase(Locale.ROOT);

        if ("NUMBER".equals(base)) {
            Integer precision = toInteger(dataPrecisionObj);
            Integer scale = toInteger(dataScaleObj);
            if (precision != null) {
                if (scale != null && scale.intValue() > 0) {
                    return base + "(" + precision + "," + scale + ")";
                }
                return base + "(" + precision + ")";
            }
            return base;
        }

        if ("VARCHAR2".equals(base) || "CHAR".equals(base) || "NCHAR".equals(base) || "NVARCHAR2".equals(base)) {
            Integer len = toInteger(dataLengthObj);
            if (len != null && len.intValue() > 0) {
                return base + "(" + len + ")";
            }
            return base;
        }

        if (("TABLE".equals(base) || "PL/SQL RECORD".equals(base)) && typeName != null && !typeName.trim().isEmpty()) {
            return base + "(" + typeName.trim().toUpperCase(Locale.ROOT) + ")";
        }

        return base;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String getOracleSourceByType(Connection conn, String name, String type) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String sql = "SELECT TEXT FROM USER_SOURCE WHERE NAME=? AND TYPE=? ORDER BY LINE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.toUpperCase());
            ps.setString(2, type);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString(1));
                }
            }
        }

        return sb.length() == 0 ? null : sb.toString();
    }
}