-- FCM push notification schema (also auto-applied by API/fcm_tables.php on first use)

ALTER TABLE `licenses`
  ADD COLUMN IF NOT EXISTS `fcm_token` TEXT NULL AFTER `android_device_id`,
  ADD COLUMN IF NOT EXISTS `fcm_token_updated_at` DATETIME NULL AFTER `fcm_token`;

CREATE TABLE IF NOT EXISTS `push_notification_log` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `license_id` INT UNSIGNED NOT NULL,
  `notification_type` VARCHAR(64) NOT NULL,
  `notification_key` VARCHAR(128) NOT NULL,
  `sent_date` DATE NOT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_daily_push` (`license_id`, `notification_type`, `notification_key`, `sent_date`),
  KEY `idx_sent_date` (`sent_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
