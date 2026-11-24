package com.salon.backend.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Hibernate once created CHECK constraints that list the allowed values of
 * some enum columns (for example {@code method IN ('CASH', ...)}). A new enum
 * constant such as {@code CLIQ} then fails on insert. The Java enums already
 * guard these columns, so this runner drops those constraints once.
 * It is idempotent: it only drops constraints that still exist. MySQL and
 * TiDB name the constraints differently, so they are found by table and column.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PaymentCheckConstraintUpgrade implements ApplicationRunner {

    private static final List<String[]> COLUMNS = List.of(
            new String[]{"booking_payment", "method"},
            new String[]{"payment_transaction", "type"}
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] column : COLUMNS) {
            dropChecks(column[0], column[1]);
        }
    }

    private void dropChecks(String table, String column) {
        try {
            List<Map<String, Object>> checks = jdbcTemplate.queryForList(
                    "SELECT tc.CONSTRAINT_NAME AS name, cc.CHECK_CLAUSE AS clause "
                            + "FROM information_schema.TABLE_CONSTRAINTS tc "
                            + "JOIN information_schema.CHECK_CONSTRAINTS cc "
                            + "ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA "
                            + "AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                            + "WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = ? "
                            + "AND tc.CONSTRAINT_TYPE = 'CHECK'",
                    table
            );
            for (Map<String, Object> check : checks) {
                String clause = String.valueOf(check.get("clause"));
                if (!clause.contains("`" + column + "`") && !clause.contains(column + " ")) {
                    continue;
                }
                String name = String.valueOf(check.get("name")).replace("`", "");
                // Table names come from the constant list above; the name comes from the schema.
                jdbcTemplate.execute("ALTER TABLE `" + table + "` DROP CHECK `" + name + "`");
                log.info("Dropped CHECK constraint {} on {}.{}", name, table, column);
            }
        } catch (DataAccessException exception) {
            // Databases without information_schema.CHECK_CONSTRAINTS (for example H2 in tests)
            // or without such constraints need nothing here.
            log.warn("Could not inspect or drop CHECK constraints on {}.{}: {}",
                    table, column, exception.getMessage());
        }
    }
}
