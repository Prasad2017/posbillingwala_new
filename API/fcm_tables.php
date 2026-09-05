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

            db_safe_query(
                $con,
                "CREATE TABLE IF NOT EXISTS `fcm_device_tokens` (
                    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    `app_type` VARCHAR(16) NOT NULL,
                    `account_id` INT NOT NULL,
                    `device_id` VARCHAR(255) NOT NULL DEFAULT '',
                    `fcm_token` TEXT NOT NULL,
                    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uq_fcm_device` (`app_type`, `account_id`, `device_id`),
                    KEY `idx_fcm_app_type` (`app_type`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
        } catch (Throwable $e) {
            return false;
        }

        $done = true;
        return true;
    }
}

if (!function_exists('fcm_device_upsert')) {
    /**
     * @param string $appType owner|dealer|admin
     */
    function fcm_device_upsert($con, $appType, $accountId, $deviceId, $fcmToken)
    {
        fcm_ensure_schema($con);
        $appType = strtolower(trim((string) $appType));
        $accountId = (int) $accountId;
        $deviceId = trim((string) $deviceId);
        $fcmToken = trim((string) $fcmToken);
        if (!in_array($appType, array('owner', 'dealer', 'admin'), true) || $accountId <= 0) {
            return false;
        }
        if ($fcmToken === '') {
            return db_stmt_execute(
                $con,
                'DELETE FROM fcm_device_tokens WHERE app_type = ? AND account_id = ? AND device_id = ?',
                'sis',
                $appType,
                $accountId,
                $deviceId
            );
        }
        return db_stmt_execute(
            $con,
            'INSERT INTO fcm_device_tokens (app_type, account_id, device_id, fcm_token)
             VALUES (?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE fcm_token = VALUES(fcm_token), updated_at = NOW()',
            'siss',
            $appType,
            $accountId,
            $deviceId,
            $fcmToken
        );
    }
}

if (!function_exists('fcm_device_list_tokens')) {
    function fcm_device_list_tokens($con, $appType)
    {
        fcm_ensure_schema($con);
        $appType = strtolower(trim((string) $appType));
        return db_stmt_fetch_all(
            $con,
            'SELECT id, app_type, account_id, device_id, fcm_token FROM fcm_device_tokens
             WHERE app_type = ? AND fcm_token IS NOT NULL AND TRIM(fcm_token) <> \'\'',
            's',
            $appType
        );
    }
}

if (!function_exists('fcm_clear_invalid_device_token')) {
    function fcm_clear_invalid_device_token($con, $rowId)
    {
        db_stmt_execute($con, 'DELETE FROM fcm_device_tokens WHERE id = ?', 'i', (int) $rowId);
    }
}
?>
