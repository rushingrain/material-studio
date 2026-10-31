CREATE DATABASE IF NOT EXISTS material_studio
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE material_studio;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nickname VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(500) NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    example_image_url VARCHAR(500) NOT NULL,
    required_image_count TINYINT UNSIGNED NOT NULL,
    allowed_mime_types JSON NOT NULL,
    max_image_size_bytes BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (code),
    KEY idx_template_category_active (category, active, sort_order),
    CONSTRAINT chk_template_image_count CHECK (required_image_count > 0),
    CONSTRAINT chk_template_image_size CHECK (max_image_size_bytes > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS upload_asset (
    id CHAR(36) NOT NULL,
    template_id BIGINT UNSIGNED NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_upload_asset_storage_key (storage_key),
    KEY idx_upload_asset_template_created (template_id, created_at DESC),
    CONSTRAINT fk_upload_asset_template FOREIGN KEY (template_id) REFERENCES template (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task (
    id CHAR(36) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    template_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(24) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    failure_stage VARCHAR(32) NULL,
    failure_reason VARCHAR(1000) NULL,
    result_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_task_user_created (user_id, created_at DESC),
    KEY idx_task_status_created (status, created_at),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_task_template FOREIGN KEY (template_id) REFERENCES template (id),
    CONSTRAINT chk_task_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_task_stage CHECK (stage IN ('QUEUED', 'ASSET_CHECK', 'SIMULATING', 'RESULT_PACKAGING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_asset (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    task_id CHAR(36) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    sort_order TINYINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_asset_order (task_id, sort_order),
    CONSTRAINT fk_task_asset_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE
) ENGINE=InnoDB;
