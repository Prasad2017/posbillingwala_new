-- Unified FCM device tokens for Owner / Dealer / Admin (POS keeps licenses.fcm_token).
CREATE TABLE IF NOT EXISTS `fcm_device_tokens` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `app_type` VARCHAR(16) NOT NULL COMMENT 'owner|dealer|admin',
  `account_id` INT NOT NULL COMMENT 'users.id',
  `device_id` VARCHAR(255) NOT NULL DEFAULT '',
  `fcm_token` TEXT NOT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_fcm_device` (`app_type`, `account_id`, `device_id`),
  KEY `idx_fcm_app_type` (`app_type`),
  KEY `idx_fcm_token_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
