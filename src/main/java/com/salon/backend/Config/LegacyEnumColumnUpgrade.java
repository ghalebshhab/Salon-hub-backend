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

/**
 * Hibernate created some enum columns as native MySQL {@code ENUM(...)} types.
 * {@code ddl-auto=update} never widens those, so inserting a newly added enum
 * constant fails. This runner converts the listed columns to VARCHAR once.
 * It is idempotent (it only touches columns that are still ENUM) and lossless
 * (every stored value is kept as text).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LegacyEnumColumnUpgrade implements ApplicationRunner {

    private static final List<EnumColumn> COLUMNS = List.of(
            new EnumColumn("salon_booking", "status", 20, false),
            new EnumColumn("user_notification", "event_type", 50, false),
            new EnumColumn("user_notification", "booking_status", 20, true)
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (EnumColumn column : COLUMNS) {
            upgrade(column);
        }
    }

    private void upgrade(EnumColumn column) {
        try {
            List<String> types = jdbcTemplate.queryForList(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    String.class,
                    column.table(),
                    column.column()
            );
            if (types.isEmpty() || !"enum".equalsIgnoreCase(types.get(0))) {
                return;
            }
            // Identifiers come from the constant list above, never from input.
            jdbcTemplate.execute("ALTER TABLE `" + column.table() + "` MODIFY `" + column.column()
                    + "` VARCHAR(" + column.length() + ")" + (column.nullable() ? " NULL" : " NOT NULL"));
            log.info("Converted {}.{} from ENUM to VARCHAR({})",
                    column.table(), column.column(), column.length());
        } catch (DataAccessException exception) {
            // Non-MySQL databases (for example tests) have no such columns to convert.
            log.warn("Could not inspect or convert {}.{}: {}",
                    column.table(), column.column(), exception.getMessage());
        }
    }

    private record EnumColumn(String table, String column, int length, boolean nullable) {
    }
}
