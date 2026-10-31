-- Generalize the existing booking inbox so salon lifecycle events can use it.
-- Existing rows remain BOOKING notifications; salon notifications do not need a booking.

SET @add_domain = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'user_notification'
       AND column_name = 'domain') = 0,
    'ALTER TABLE user_notification ADD COLUMN domain ENUM(''BOOKING'', ''SALON'') NOT NULL DEFAULT ''BOOKING''',
    'SELECT 1'
);
PREPARE add_domain_statement FROM @add_domain;
EXECUTE add_domain_statement;
DEALLOCATE PREPARE add_domain_statement;

SET @add_reason = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'user_notification'
       AND column_name = 'reason') = 0,
    'ALTER TABLE user_notification ADD COLUMN reason VARCHAR(1000) NULL',
    'SELECT 1'
);
PREPARE add_reason_statement FROM @add_reason;
EXECUTE add_reason_statement;
DEALLOCATE PREPARE add_reason_statement;

ALTER TABLE user_notification
    MODIFY COLUMN booking_id BIGINT NULL,
    MODIFY COLUMN booking_date DATE NULL,
    MODIFY COLUMN booking_time TIME NULL,
    MODIFY COLUMN booking_status ENUM(
        'ACCEPTED',
        'CANCELLED',
        'COMPLETED',
        'PENDING',
        'REJECTED'
    ) NULL,
    MODIFY COLUMN event_type ENUM(
        'BOOKING_ACCEPTED',
        'BOOKING_CANCELLED',
        'BOOKING_EMPLOYEE_REASSIGNED',
        'BOOKING_REJECTED',
        'BOOKING_RESCHEDULE_ACCEPTED',
        'BOOKING_RESCHEDULE_PROPOSED',
        'BOOKING_RESCHEDULE_REJECTED',
        'BOOKING_TIME_SUGGESTED',
        'SALON_BLOCKED',
        'SALON_REQUEST_ACCEPTED',
        'SALON_REQUEST_REJECTED',
        'SALON_REQUEST_RESUBMITTED',
        'SALON_REQUEST_SUBMITTED',
        'SALON_UNBLOCKED'
    ) NOT NULL;
