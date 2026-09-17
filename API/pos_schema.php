<?php
/**
 * P29 schema ensure — CREATE TABLE IF NOT EXISTS + additive license columns.
 * Safe to call from APIs. Full DBA path remains API/migrations/p29_multi_user_printers.sql
 */
require_once __DIR__ . '/db_prepared.php';

if (!function_exists('pos_schema_add_column')) {
    function pos_schema_add_column($con, $table, $column, $ddl)
    {
        $exists = db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            'ss',
            $table,
            $column
        );
        if ($exists > 0) {
            return;
        }
        @mysqli_query($con, 'ALTER TABLE `' . $table . '` ADD COLUMN ' . $ddl);
    }
}

if (!function_exists('pos_normalize_paper_size')) {
    function pos_normalize_paper_size($raw)
    {
        $n = strtolower(preg_replace('/\s+/', '', (string) $raw));
        if ($n !== '' && strpos($n, '3') !== false) {
            return '3-Inch';
        }
        return '2-Inch';
    }
}

if (!function_exists('pos_normalize_connection_type')) {
    function pos_normalize_connection_type($raw, $fallback = 'BLUETOOTH')
    {
        $v = strtoupper(trim((string) $raw));
        if ($v === 'USB') {
            return 'USB';
        }
        if ($v === 'WIFI' || $v === 'NETWORK') {
            return 'WIFI';
        }
        if ($v === 'BLUETOOTH' || $v === 'BT' || $v === 'BLE') {
            return 'BLUETOOTH';
        }
        $fb = strtoupper(trim((string) $fallback));
        if ($fb === 'USB') {
            return 'USB';
        }
        if ($fb === 'WIFI' || $fb === 'NETWORK') {
            return 'WIFI';
        }
        return 'BLUETOOTH';
    }
}

if (!function_exists('pos_schema_ensure')) {
    function pos_schema_ensure($con)
    {
        static $done = false;
        if ($done || !($con instanceof mysqli)) {
            return;
        }
        $done = true;

        pos_schema_add_column($con, 'licenses', 'userManagementEnabled', '`userManagementEnabled` TINYINT(1) NOT NULL DEFAULT 0');
        pos_schema_add_column($con, 'licenses', 'maxUsers', '`maxUsers` INT(11) NOT NULL DEFAULT 10');
        pos_schema_add_column($con, 'licenses', 'maxDevices', '`maxDevices` INT(11) NOT NULL DEFAULT 5');
        pos_schema_add_column($con, 'licenses', 'maxPrinters', '`maxPrinters` INT(11) NOT NULL DEFAULT 0');
        pos_schema_add_column($con, 'licenses', 'permissionVersion', '`permissionVersion` INT(11) NOT NULL DEFAULT 1');

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_staff` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `organization_id` INT(11) NOT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `name` VARCHAR(120) NOT NULL,
          `mobileNumber` VARCHAR(15) NOT NULL,
          `address` VARCHAR(255) DEFAULT NULL,
          `profileImage` TEXT DEFAULT NULL,
          `role` VARCHAR(32) NOT NULL,
          `pinHash` VARCHAR(255) NOT NULL,
          `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
          `monthlySalary` DECIMAL(12,2) NOT NULL DEFAULT 0,
          `lastLoginAt` DATETIME DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_pos_staff_store_mobile` (`licenseId`, `mobileNumber`),
          KEY `idx_pos_staff_org` (`organization_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        pos_schema_add_column($con, 'pos_staff', 'monthlySalary', '`monthlySalary` DECIMAL(12,2) NOT NULL DEFAULT 0');

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_salary_payment` (
          `id` INT(11) NOT NULL AUTO_INCREMENT,
          `licenseId` INT(11) NOT NULL,
          `staffId` INT(11) NOT NULL,
          `salaryMonth` CHAR(7) NOT NULL,
          `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
          `paidOn` DATE NOT NULL,
          `note` VARCHAR(255) NOT NULL DEFAULT '',
          `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uq_salary_staff_month` (`staffId`, `salaryMonth`),
          KEY `idx_salary_license_month` (`licenseId`, `salaryMonth`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        pos_schema_add_column($con, 'invoice', 'createdByStaffId', '`createdByStaffId` INT NULL DEFAULT NULL');
        pos_schema_add_column($con, 'invoice', 'createdByStaffName', "`createdByStaffName` VARCHAR(120) NOT NULL DEFAULT ''");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_role_permission` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `role` VARCHAR(32) NOT NULL,
          `permissionKey` VARCHAR(64) NOT NULL,
          `allowed` TINYINT(1) NOT NULL DEFAULT 0,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_pos_role_perm` (`role`, `permissionKey`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_staff_permission_override` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `staffId` INT(10) UNSIGNED NOT NULL,
          `permissionKey` VARCHAR(64) NOT NULL,
          `overrideState` VARCHAR(8) NOT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_staff_perm` (`staffId`, `permissionKey`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_devices` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `organization_id` INT(11) NOT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `deviceId` VARCHAR(191) NOT NULL,
          `deviceName` VARCHAR(191) DEFAULT NULL,
          `platform` VARCHAR(16) NOT NULL DEFAULT 'ANDROID',
          `appVersion` VARCHAR(32) DEFAULT NULL,
          `osVersion` VARCHAR(64) DEFAULT NULL,
          `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
          `isPrintHost` TINYINT(1) NOT NULL DEFAULT 0,
          `lastSeenAt` DATETIME DEFAULT NULL,
          `lastSyncAt` DATETIME DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_pos_device_store` (`licenseId`, `deviceId`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_sessions` (
          `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
          `sessionId` CHAR(36) NOT NULL,
          `staffId` INT(10) UNSIGNED DEFAULT NULL,
          `deviceId` VARCHAR(191) DEFAULT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `loginAt` DATETIME NOT NULL,
          `lastActivityAt` DATETIME DEFAULT NULL,
          `logoutAt` DATETIME DEFAULT NULL,
          `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_pos_session` (`sessionId`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `store_printers` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `organization_id` INT(11) NOT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `deviceId` VARCHAR(191) DEFAULT NULL,
          `printerName` VARCHAR(120) NOT NULL,
          `printerType` VARCHAR(32) NOT NULL DEFAULT 'THERMAL',
          `connectionType` VARCHAR(16) NOT NULL,
          `ipAddress` VARCHAR(64) DEFAULT NULL,
          `port` INT(11) DEFAULT 9100,
          `bluetoothAddress` VARCHAR(64) DEFAULT NULL,
          `usbIdentifier` VARCHAR(191) DEFAULT NULL,
          `usbName` VARCHAR(128) DEFAULT NULL,
          `paperSize` VARCHAR(16) NOT NULL DEFAULT '2-Inch',
          `purpose` VARCHAR(32) NOT NULL DEFAULT 'KOT',
          `area` VARCHAR(32) NOT NULL DEFAULT 'KITCHEN',
          `status` VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
          `enabled` TINYINT(1) NOT NULL DEFAULT 1,
          `isDefault` TINYINT(1) NOT NULL DEFAULT 0,
          `isBackup` TINYINT(1) NOT NULL DEFAULT 0,
          `primaryPrinterId` INT(10) UNSIGNED DEFAULT NULL,
          `lastHeartbeatAt` DATETIME DEFAULT NULL,
          `lastPrintAt` DATETIME DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          KEY `idx_store_printer_license` (`licenseId`, `enabled`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        pos_schema_add_column($con, 'store_printers', 'bluetoothAddress', "`bluetoothAddress` VARCHAR(64) DEFAULT NULL");
        pos_schema_add_column($con, 'store_printers', 'usbIdentifier', "`usbIdentifier` VARCHAR(191) DEFAULT NULL");
        pos_schema_add_column($con, 'store_printers', 'usbName', "`usbName` VARCHAR(128) DEFAULT NULL");
        pos_schema_add_column($con, 'store_printers', 'paperSize', "`paperSize` VARCHAR(16) NOT NULL DEFAULT '2-Inch'");
        pos_schema_add_column($con, 'store_printers', 'connectionType', "`connectionType` VARCHAR(16) NOT NULL DEFAULT 'BLUETOOTH'");
        pos_schema_add_column($con, 'store_printers', 'printerType', "`printerType` VARCHAR(32) NOT NULL DEFAULT 'THERMAL'");
        pos_schema_add_column($con, 'store_printers', 'purpose', "`purpose` VARCHAR(32) NOT NULL DEFAULT 'KOT'");
        pos_schema_add_column($con, 'store_printers', 'area', "`area` VARCHAR(32) NOT NULL DEFAULT 'KITCHEN'");
        pos_schema_add_column($con, 'store_printers', 'ipAddress', "`ipAddress` VARCHAR(64) DEFAULT NULL");
        pos_schema_add_column($con, 'store_printers', 'port', "`port` INT(11) DEFAULT 9100");
        pos_schema_add_column($con, 'store_printers', 'deviceId', "`deviceId` VARCHAR(191) DEFAULT NULL");

        pos_schema_add_column($con, 'company_printer_setting', 'paperSize', "`paperSize` VARCHAR(16) NOT NULL DEFAULT '2-Inch'");
        pos_schema_add_column($con, 'company_printer_setting', 'kotPaperSize', "`kotPaperSize` VARCHAR(16) NOT NULL DEFAULT '2-Inch'");
        pos_schema_add_column($con, 'company_printer_setting', 'billConnectionType', "`billConnectionType` VARCHAR(16) NOT NULL DEFAULT 'BLUETOOTH'");
        pos_schema_add_column($con, 'company_printer_setting', 'kotConnectionType', "`kotConnectionType` VARCHAR(16) NOT NULL DEFAULT 'BLUETOOTH'");
        pos_schema_add_column($con, 'company_printer_setting', 'billUsbIdentifier', "`billUsbIdentifier` VARCHAR(191) DEFAULT NULL");
        pos_schema_add_column($con, 'company_printer_setting', 'billUsbName', "`billUsbName` VARCHAR(128) DEFAULT NULL");
        pos_schema_add_column($con, 'company_printer_setting', 'kotUsbIdentifier', "`kotUsbIdentifier` VARCHAR(191) DEFAULT NULL");
        pos_schema_add_column($con, 'company_printer_setting', 'kotUsbName', "`kotUsbName` VARCHAR(128) DEFAULT NULL");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `printer_routes` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `printerId` INT(10) UNSIGNED NOT NULL,
          `documentType` VARCHAR(16) NOT NULL DEFAULT 'KOT',
          `foodTypeCode` VARCHAR(32) DEFAULT NULL,
          `categoryId` INT(11) DEFAULT NULL,
          `subcategoryId` INT(11) DEFAULT NULL,
          `productId` INT(11) DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          KEY `idx_printer_route_license` (`licenseId`, `documentType`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `print_hosts` (
          `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
          `organization_id` INT(11) NOT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `deviceId` VARCHAR(191) NOT NULL,
          `platform` VARCHAR(16) NOT NULL DEFAULT 'ANDROID',
          `status` VARCHAR(16) NOT NULL DEFAULT 'ONLINE',
          `lastHeartbeatAt` DATETIME DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_print_host_device` (`licenseId`, `deviceId`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `print_jobs` (
          `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
          `organization_id` INT(11) NOT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `createdByStaffId` INT(10) UNSIGNED DEFAULT NULL,
          `createdByDeviceId` VARCHAR(191) DEFAULT NULL,
          `printerId` INT(10) UNSIGNED NOT NULL,
          `hostDeviceId` VARCHAR(191) DEFAULT NULL,
          `documentType` VARCHAR(16) NOT NULL,
          `payload` LONGTEXT NOT NULL,
          `status` VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
          `priority` INT(11) NOT NULL DEFAULT 0,
          `retryCount` INT(11) NOT NULL DEFAULT 0,
          `errorMessage` VARCHAR(255) DEFAULT NULL,
          `idempotencyKey` VARCHAR(191) NOT NULL,
          `documentId` VARCHAR(64) DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `printedAt` DATETIME DEFAULT NULL,
          `failedAt` DATETIME DEFAULT NULL,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_print_job_idem` (`licenseId`, `idempotencyKey`),
          KEY `idx_print_job_queue` (`licenseId`, `status`, `printerId`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        @mysqli_query($con, "CREATE TABLE IF NOT EXISTS `pos_audit_log` (
          `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
          `organization_id` INT(11) NOT NULL,
          `licenseId` INT(10) UNSIGNED NOT NULL,
          `actorStaffId` INT(10) UNSIGNED DEFAULT NULL,
          `actorType` VARCHAR(16) DEFAULT 'pos',
          `action` VARCHAR(64) NOT NULL,
          `entityType` VARCHAR(32) DEFAULT NULL,
          `entityId` VARCHAR(64) DEFAULT NULL,
          `metaJson` TEXT DEFAULT NULL,
          `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY (`id`),
          KEY `idx_pos_audit_license` (`licenseId`, `createdAt`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    }
}

if (!function_exists('pos_licence_um_row')) {
    function pos_licence_um_row($con, $licenseId)
    {
        pos_schema_ensure($con);
        $row = db_stmt_fetch_one(
            $con,
            'SELECT `id`, `userId`, `android_device_id`, `android_device_name`, `mpin`,
                    COALESCE(`userManagementEnabled`,0) AS userManagementEnabled,
                    COALESCE(`maxUsers`,10) AS maxUsers,
                    COALESCE(`maxDevices`,5) AS maxDevices,
                    COALESCE(`maxPrinters`,0) AS maxPrinters,
                    COALESCE(`permissionVersion`,1) AS permissionVersion
             FROM `licenses` WHERE `id`=? LIMIT 1',
            'i',
            (int) $licenseId
        );
        if ($row === null) {
            return array(
                'id' => (int) $licenseId,
                'userId' => 0,
                'android_device_id' => '',
                'userManagementEnabled' => 0,
                'maxUsers' => 10,
                'maxDevices' => 5,
                'maxPrinters' => 0,
                'permissionVersion' => 1,
            );
        }
        $row['userManagementEnabled'] = (int) $row['userManagementEnabled'];
        $row['maxUsers'] = (int) $row['maxUsers'];
        $row['maxDevices'] = (int) $row['maxDevices'];
        $row['maxPrinters'] = (int) $row['maxPrinters'];
        $row['permissionVersion'] = (int) $row['permissionVersion'];
        return $row;
    }
}

if (!function_exists('pos_um_enabled')) {
    function pos_um_enabled($con, $licenseId)
    {
        $row = pos_licence_um_row($con, $licenseId);
        return !empty($row['userManagementEnabled']);
    }
}

if (!function_exists('licence_append_user_management_response')) {
    function licence_append_user_management_response($con, array $response, $licenseId)
    {
        $row = pos_licence_um_row($con, $licenseId);
        $response['userManagementEnabled'] = (string) $row['userManagementEnabled'];
        $response['maxUsers'] = (string) $row['maxUsers'];
        $response['maxDevices'] = (string) $row['maxDevices'];
        $response['maxPrinters'] = (string) $row['maxPrinters'];
        $response['permissionVersion'] = (string) $row['permissionVersion'];
        return $response;
    }
}

if (!function_exists('pos_normalize_platform')) {
    function pos_normalize_platform($platform)
    {
        $p = strtoupper(trim((string) $platform));
        if (in_array($p, array('ANDROID', 'IOS', 'WEB'), true)) {
            return $p;
        }
        return 'ANDROID';
    }
}

if (!function_exists('pos_posted_staff_id')) {
    function pos_posted_staff_id()
    {
        if (isset($_POST['staffId']) && trim((string) $_POST['staffId']) !== '') {
            return (int) $_POST['staffId'];
        }
        if (isset($_SERVER['HTTP_X_POS_STAFF_ID']) && trim((string) $_SERVER['HTTP_X_POS_STAFF_ID']) !== '') {
            return (int) $_SERVER['HTTP_X_POS_STAFF_ID'];
        }
        return 0;
    }
}
