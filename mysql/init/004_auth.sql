USE material_studio;

INSERT INTO app_user (id, nickname)
VALUES
    (1, '演示用户'),
    (2, '光影访客')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

CREATE TABLE IF NOT EXISTS user_credential (
    user_id BIGINT UNSIGNED NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    password_salt CHAR(32) NOT NULL,
    password_iterations INT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_credential_username (username),
    CONSTRAINT fk_user_credential_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_session (
    token_hash CHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_hash),
    KEY idx_user_session_user_expires (user_id, expires_at),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE upload_asset
    ADD COLUMN user_id BIGINT UNSIGNED NULL AFTER id;

UPDATE upload_asset
SET user_id = 1
WHERE user_id IS NULL;

ALTER TABLE upload_asset
    MODIFY user_id BIGINT UNSIGNED NOT NULL,
    ADD KEY idx_upload_asset_user_created (user_id, created_at DESC),
    ADD CONSTRAINT fk_upload_asset_user FOREIGN KEY (user_id) REFERENCES app_user (id);

INSERT INTO user_credential
    (user_id, username, password_hash, password_salt, password_iterations)
VALUES
    (1, 'demo', '370d8fe7acecba4f4bd47cac0862194364d2cf8470f9f2b74ca2a0de91975901',
     '2c5c8b95d775e5732593b1af8ec9dd93', 210000),
    (2, 'alice', '159eb003b87a8fb2ad861325e76ca9afbcaf39bc72b318d49d85b2c0edd1fdfe',
     '62188e2c9cb128800336f4cfd45be139', 210000)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    password_salt = VALUES(password_salt),
    password_iterations = VALUES(password_iterations);
