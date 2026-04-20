package services;

import java.util.List;

/**
 * @deprecated Logic has been split into dedicated services:
 *   FunctionComparisonService, ProcedureComparisonService,
 *   TriggerComparisonService, PackageComparisonService.
 *
 * Kept as a thin dispatcher so any lingering callers continue to compile.
 * Do NOT add new logic here.
 */
@Deprecated
public final class DbObjectComparisonService {

    private DbObjectComparisonService() {}

    @Deprecated
    public static List<DbObjectDiff> compareObjects(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2,
            DbObjectDiff.ObjectType objectType) {

        switch (objectType) {
            case FUNCTION:  return FunctionComparisonService.compare(db1, db2);
            case PROCEDURE: return ProcedureComparisonService.compare(db1, db2);
            case TRIGGER:   return TriggerComparisonService.compare(db1, db2);
            case PACKAGE:
            default:        return PackageComparisonService.compare(db1, db2);
        }
    }
}
