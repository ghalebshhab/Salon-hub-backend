-- Community notifications (friend request received / accepted / declined) have no
-- salon. The baseline made salon_id and salon_name NOT NULL, so every friend
-- request failed with a constraint error. The UserNotification entity already
-- allows null for both columns.
ALTER TABLE user_notification
    MODIFY COLUMN salon_id BIGINT NULL,
    MODIFY COLUMN salon_name VARCHAR(255) NULL;
