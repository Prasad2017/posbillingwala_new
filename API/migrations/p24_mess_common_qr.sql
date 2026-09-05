-- P24: Mess Common QR + Registration No. + meal session tokens (additive only).
-- Does NOT replace existing mess_token (one-time scan QR) or mess_invoice (paper coupons).

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1) Registration number on mess members
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member' AND COLUMN_NAME = 'registration_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `mess_member` ADD COLUMN `registration_no` VARCHAR(64) NULL AFTER `member_address`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill REG-{id} for existing rows without a registration number
UPDATE `mess_member`
SET `registration_no` = CONCAT('REG-', LPAD(`id`, 4, '0'))
WHERE (`registration_no` IS NULL OR `registration_no` = '') AND `id` IS NOT NULL;

-- Unique per shop (userId + registration_no). Multiple NULLs allowed historically; after backfill should be set.
SET @idx_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member' AND INDEX_NAME = 'uq_mess_member_reg'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `mess_member` ADD UNIQUE KEY `uq_mess_member_reg` (`userId`, `registration_no`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) Common Mess QR (one ACTIVE per shop/branch via app rules)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mess_qr` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` INT NOT NULL,
  `organization_id` INT DEFAULT NULL,
  `branch_id` INT DEFAULT NULL,
  `public_token` VARCHAR(64) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `print_device_id` VARCHAR(255) DEFAULT NULL,
  `mess_label` VARCHAR(255) DEFAULT NULL,
  `branch_label` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deactivated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mess_qr_public_token` (`public_token`),
  KEY `idx_mess_qr_user_status` (`userId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 3) Configurable meal sessions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mess_meal_session` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` INT NOT NULL,
  `organization_id` INT DEFAULT NULL,
  `branch_id` INT DEFAULT NULL,
  `session_name` VARCHAR(64) NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `token_prefix` VARCHAR(8) NOT NULL DEFAULT 'L',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `menu_notes` TEXT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mess_meal_session_user` (`userId`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 4) Registered meal tokens (Common QR flow) — separate from mess_token
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mess_meal_token` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `public_id` VARCHAR(64) NOT NULL,
  `userId` INT NOT NULL,
  `organization_id` INT DEFAULT NULL,
  `branch_id` INT DEFAULT NULL,
  `qr_id` INT UNSIGNED DEFAULT NULL,
  `session_id` INT UNSIGNED NOT NULL,
  `session_name` VARCHAR(64) NOT NULL,
  `member_id` INT NOT NULL,
  `registration_no` VARCHAR(64) NOT NULL,
  `member_name` VARCHAR(255) NOT NULL DEFAULT '',
  `token_number` VARCHAR(32) NOT NULL,
  `token_date` DATE NOT NULL,
  `token_seq` INT UNSIGNED NOT NULL DEFAULT 1,
  `print_status` VARCHAR(24) NOT NULL DEFAULT 'PRINT_PENDING',
  `print_device_id` VARCHAR(255) DEFAULT NULL,
  `printed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_mess_meal_token_public` (`public_id`),
  UNIQUE KEY `uq_mess_meal_token_once` (`userId`, `member_id`, `token_date`, `session_id`),
  KEY `idx_mess_meal_token_pending` (`userId`, `print_status`, `token_date`),
  KEY `idx_mess_meal_token_device` (`print_device_id`, `print_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sequence counter per shop/date/prefix (for L-001 style numbers)
CREATE TABLE IF NOT EXISTS `mess_meal_token_seq` (
  `userId` INT NOT NULL,
  `token_date` DATE NOT NULL,
  `token_prefix` VARCHAR(8) NOT NULL,
  `last_seq` INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`userId`, `token_date`, `token_prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5) Audit log
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mess_token_audit` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` INT DEFAULT NULL,
  `event_type` VARCHAR(64) NOT NULL,
  `public_token` VARCHAR(64) DEFAULT NULL,
  `registration_no` VARCHAR(64) DEFAULT NULL,
  `token_public_id` VARCHAR(64) DEFAULT NULL,
  `detail` VARCHAR(255) DEFAULT NULL,
  `source_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mess_audit_user_time` (`userId`, `created_at`),
  KEY `idx_mess_audit_event` (`event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 6) Simple rate-limit bucket for public endpoints
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mess_public_rate_limit` (
  `bucket_key` VARCHAR(128) NOT NULL,
  `window_start` INT UNSIGNED NOT NULL,
  `hit_count` INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`bucket_key`, `window_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
