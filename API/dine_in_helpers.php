<?php
/**
 * Dine-In schema helpers — ensure columns/tables exist without blocking requests.
 * PHP 7.0+ safe.
 */

require_once __DIR__ . '/php_compat.php';

if (!function_exists('dine_in_ensure_printer_kot_columns')) {
    function dine_in_ensure_printer_kot_columns($con)
    {
        static $done = false;
        if ($done || $con === null) {
            return;
        }
        $cols = array(
            'duplicateBillUse' => "ALTER TABLE `company_printer_setting` ADD COLUMN `duplicateBillUse` VARCHAR(10) NULL DEFAULT 'off'",
            'kotEnable' => "ALTER TABLE `company_printer_setting` ADD COLUMN `kotEnable` VARCHAR(10) NULL DEFAULT 'on'",
            'kotPrefix' => "ALTER TABLE `company_printer_setting` ADD COLUMN `kotPrefix` VARCHAR(32) NULL DEFAULT 'KOT-'",
            'kotCopies' => "ALTER TABLE `company_printer_setting` ADD COLUMN `kotCopies` VARCHAR(8) NULL DEFAULT '1'",
            'kotAutoPrint' => "ALTER TABLE `company_printer_setting` ADD COLUMN `kotAutoPrint` VARCHAR(10) NULL DEFAULT 'off'",
            'kotPreview' => "ALTER TABLE `company_printer_setting` ADD COLUMN `kotPreview` VARCHAR(10) NULL DEFAULT 'on'",
        );
        foreach ($cols as $name => $alter) {
            try {
                $res = db_safe_query($con, "SHOW COLUMNS FROM `company_printer_setting` LIKE '" . mysqli_real_escape_string($con, $name) . "'");
                if ($res && mysqli_num_rows($res) === 0) {
                    db_safe_query($con, $alter);
                }
                if ($res) {
                    mysqli_free_result($res);
                }
            } catch (Throwable $e) {
                // ignore probe failures
            }
        }
        $done = true;
    }
}

if (!function_exists('dine_in_ensure_invoice_columns')) {
    function dine_in_ensure_invoice_columns($con)
    {
        static $done = false;
        if ($done || $con === null) {
            return;
        }
        try {
            $a = db_safe_query($con, "SHOW COLUMNS FROM `invoice` LIKE 'diningSessionId'");
            if ($a && mysqli_num_rows($a) === 0) {
                db_safe_query($con, "ALTER TABLE `invoice` ADD COLUMN `diningSessionId` VARCHAR(64) NULL DEFAULT NULL");
            }
            if ($a) {
                mysqli_free_result($a);
            }
            $b = db_safe_query($con, "SHOW COLUMNS FROM `invoice` LIKE 'billPrintStatus'");
            if ($b && mysqli_num_rows($b) === 0) {
                db_safe_query($con, "ALTER TABLE `invoice` ADD COLUMN `billPrintStatus` VARCHAR(32) NULL DEFAULT ''");
            }
            if ($b) {
                mysqli_free_result($b);
            }
        } catch (Throwable $e) {
            // ignore
        }
        $done = true;
    }
}

if (!function_exists('dine_in_ensure_schema')) {
    function dine_in_ensure_schema($con)
    {
        static $done = false;
        if ($done || $con === null) {
            return true;
        }
        dine_in_ensure_printer_kot_columns($con);
        dine_in_ensure_invoice_columns($con);

        $tables = array(
            "CREATE TABLE IF NOT EXISTS `dining_area` (
              `areaId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `areaName` VARCHAR(128) NOT NULL,
              `areaSortOrder` INT NOT NULL DEFAULT 0,
              `areaActive` VARCHAR(8) NOT NULL DEFAULT '1',
              `areaNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`areaId`),
              KEY `idx_dining_area_license` (`licenseId`, `branch_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `table_type` (
              `tableTypeId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `tableTypeName` VARCHAR(128) NOT NULL,
              `defaultCapacity` INT NOT NULL DEFAULT 4,
              `tableTypeActive` VARCHAR(8) NOT NULL DEFAULT '1',
              `tableTypeNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`tableTypeId`),
              KEY `idx_table_type_license` (`licenseId`, `branch_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `pos_table` (
              `tableId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `tableNumber` VARCHAR(32) NOT NULL,
              `tableName` VARCHAR(64) DEFAULT NULL,
              `tableTypeId` VARCHAR(64) DEFAULT NULL,
              `capacity` INT NOT NULL DEFAULT 4,
              `areaId` VARCHAR(64) DEFAULT NULL,
              `tableActive` VARCHAR(8) NOT NULL DEFAULT '1',
              `positionX` VARCHAR(32) DEFAULT NULL,
              `positionY` VARCHAR(32) DEFAULT NULL,
              `sortOrder` INT NOT NULL DEFAULT 0,
              `statusOverride` VARCHAR(32) DEFAULT NULL,
              `posTableNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`tableId`),
              UNIQUE KEY `uq_pos_table_license_number` (`licenseId`, `branch_id`, `tableNumber`),
              KEY `idx_pos_table_license` (`licenseId`, `branch_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `dining_session` (
              `sessionId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `localSessionId` VARCHAR(64) DEFAULT NULL,
              `primaryTableNumber` VARCHAR(32) NOT NULL,
              `joinedTableNumbers` VARCHAR(255) DEFAULT NULL,
              `sessionStatus` VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
              `guestCount` INT NOT NULL DEFAULT 0,
              `startedAt` VARCHAR(64) DEFAULT NULL,
              `closedAt` VARCHAR(64) DEFAULT NULL,
              `customerName` VARCHAR(255) DEFAULT NULL,
              `customerMobile` VARCHAR(64) DEFAULT NULL,
              `waiterName` VARCHAR(128) DEFAULT NULL,
              `unpaidInvoiceNumber` VARCHAR(64) DEFAULT NULL,
              `paidAmount` VARCHAR(32) NOT NULL DEFAULT '0',
              `sessionVersion` INT NOT NULL DEFAULT 1,
              `sessionNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (`sessionId`),
              KEY `idx_dining_session_table` (`licenseId`, `branch_id`, `primaryTableNumber`),
              KEY `idx_dining_session_status` (`sessionStatus`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `order_round` (
              `orderRoundId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `localOrderRoundId` VARCHAR(64) DEFAULT NULL,
              `sessionId` VARCHAR(64) NOT NULL,
              `roundNumber` INT NOT NULL DEFAULT 1,
              `createdAt` VARCHAR(64) DEFAULT NULL,
              `kotId` VARCHAR(64) DEFAULT NULL,
              `orderRoundNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`orderRoundId`),
              KEY `idx_order_round_session` (`sessionId`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `kot` (
              `kotId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `localKotId` VARCHAR(64) DEFAULT NULL,
              `sessionId` VARCHAR(64) DEFAULT NULL,
              `orderRoundId` VARCHAR(64) DEFAULT NULL,
              `kotNumber` VARCHAR(64) NOT NULL,
              `tableNumber` VARCHAR(32) DEFAULT NULL,
              `printStatus` VARCHAR(32) DEFAULT 'PENDING',
              `createdAt` VARCHAR(64) DEFAULT NULL,
              `kitchenName` VARCHAR(128) DEFAULT NULL,
              `kotNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`kotId`),
              KEY `idx_kot_license` (`licenseId`, `branch_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `kot_item` (
              `kotItemId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
              `licenseId` INT NOT NULL,
              `organization_id` INT DEFAULT NULL,
              `branch_id` INT DEFAULT NULL,
              `device_id` VARCHAR(255) DEFAULT NULL,
              `kotId` VARCHAR(64) NOT NULL,
              `cartId` VARCHAR(64) DEFAULT NULL,
              `productName` VARCHAR(255) NOT NULL,
              `productQuantity` VARCHAR(32) NOT NULL DEFAULT '1',
              `portionName` VARCHAR(128) DEFAULT NULL,
              `kotItemNetworkStatus` VARCHAR(64) DEFAULT NULL,
              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (`kotItemId`),
              KEY `idx_kot_item_kot` (`kotId`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        );
        foreach ($tables as $sql) {
            try {
                db_safe_query($con, $sql);
            } catch (Throwable $e) {
                // ignore
            }
        }
        $done = true;
        return true;
    }
}
