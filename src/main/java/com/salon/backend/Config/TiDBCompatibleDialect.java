package com.salon.backend.Config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.internal.MySQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * MySQL dialect that also works on TiDB (the production database).
 *
 * <p>For MySQL 8, Hibernate writes pessimistic locks as
 * {@code FOR UPDATE OF <alias>}. TiDB reads the alias as a table name and
 * fails with "Table '...' doesn't exist". This dialect writes a plain
 * {@code FOR UPDATE}, which MySQL and TiDB both accept. All locked queries in
 * this app read one table, so the lock covers the same rows.
 */
public class TiDBCompatibleDialect extends MySQLDialect {

    public TiDBCompatibleDialect() {
        super();
    }

    public TiDBCompatibleDialect(DatabaseVersion version) {
        super(version);
    }

    public TiDBCompatibleDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public LockingSupport getLockingSupport() {
        return new PlainForUpdateLockingSupport(getVersion());
    }

    @Override
    protected boolean supportsAliasLocks() {
        return false;
    }

    @Override
    public String getForUpdateString(String aliases) {
        return getForUpdateString();
    }

    /** MySQL locking rules, but without "OF alias" on the lock clause. */
    private static final class PlainForUpdateLockingSupport extends MySQLLockingSupport {

        PlainForUpdateLockingSupport(DatabaseVersion version) {
            super(version);
        }

        @Override
        public RowLockStrategy getWriteRowLockStrategy() {
            return RowLockStrategy.NONE;
        }

        @Override
        public RowLockStrategy getReadRowLockStrategy() {
            return RowLockStrategy.NONE;
        }
    }
}
