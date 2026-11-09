CREATE TABLE moderation_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    target_id BIGINT NOT NULL,
    target_label VARCHAR(255) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(24) NOT NULL,
    admin_note VARCHAR(1000),
    reviewed_by_id BIGINT,
    created_at DATETIME NOT NULL,
    reviewed_at DATETIME,
    CONSTRAINT fk_moderation_report_reporter FOREIGN KEY (reporter_id) REFERENCES user (id),
    CONSTRAINT fk_moderation_report_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES user (id),
    INDEX idx_moderation_report_status_created (status, created_at),
    INDEX idx_moderation_report_target (target_type, target_id),
    INDEX idx_moderation_report_reporter (reporter_id)
);
