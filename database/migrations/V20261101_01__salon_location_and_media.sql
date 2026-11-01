-- Salon profile location and Cloudinary media metadata.

ALTER TABLE salon
    ADD COLUMN google_maps_url VARCHAR(1000) NULL,
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL,
    ADD COLUMN profile_picture_public_id VARCHAR(255) NULL;

CREATE TABLE employee_work_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    salon_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    secure_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    resource_type ENUM('IMAGE', 'VIDEO') NOT NULL,
    caption VARCHAR(300) NULL,
    width INT NULL,
    height INT NULL,
    bytes BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_work_media_salon_created (salon_id, created_at),
    INDEX idx_work_media_employee (employee_id),
    CONSTRAINT fk_work_media_salon FOREIGN KEY (salon_id) REFERENCES salon(id),
    CONSTRAINT fk_work_media_employee FOREIGN KEY (employee_id) REFERENCES user(id)
);
