<?php
/**
 * FCM schema helpers — auto-adds columns/tables on first use.
 * PHP 7.0+ safe (no mysqli exception dependency).
 */

require_once __DIR__ . '/db_prepared.php';

if (!function_exists('fcm_ensure_schema')) {
    function fcm_ensure_schema($con)
    {
        static $done = false;
        if ($done) {
            return true;
        }

        try {
            $col = db_stmt_fetch_one(
                $con,
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'fcm_token' LIMIT 1"
            );
            if ($col === null) {
                db_safe_query($con, "ALTER TABLE `licenses` ADD COLUMN `fcm_token` TEXT NULL AFTER `android_device_id`");
                db_safe_query($con, "ALTER TABLE `licenses` ADD COLUMN `fcm_token_updated_at` DATETIME NULL AFTER `fcm_token`");
            }

            db_safe_query(
                $con,
                "CREATE TABLE IF NOT EXISTS `push_notification_log` (
                    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    `license_id` INT UNSIGNED NOT NULL,
                    `notification_type` VARCHAR(64) NOT NULL,
                    `notification_key` VARCHAR(128) NOT NULL,
                    `sent_date` DATE NOT NULL,
                    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uniq_daily_push` (`license_id`, `notification_type`, `notification_key`, `sent_date`),
                    KEY `idx_sent_date` (`sent_date`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
        } catch (Throwable $e) {
            return false;
        }

        $done = true;
        return true;
    }
}

?>
