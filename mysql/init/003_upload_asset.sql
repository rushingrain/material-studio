USE material_studio;

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
