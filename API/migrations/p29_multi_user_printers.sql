-- P29: Multi-user, multi-device, multi-printer, remote print, permissions, audit.
-- Additive only. Existing licences keep userManagementEnabled = 0.

SET NAMES utf8mb4;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'userManagementEnabled') > 0,
  'SELECT 1',
  'ALTER TABLE `licenses` ADD COLUMN `userManagementEnabled` TINYINT(1) NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'maxUsers') > 0,
  'SELECT 1',
  'ALTER TABLE `licenses` ADD COLUMN `maxUsers` INT(11) NOT NULL DEFAULT 10'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'maxDevices') > 0,
  'SELECT 1',
  'ALTER TABLE `licenses` ADD COLUMN `maxDevices` INT(11) NOT NULL DEFAULT 5'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'maxPrinters') > 0,
  'SELECT 1',
  'ALTER TABLE `licenses` ADD COLUMN `maxPrinters` INT(11) NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'permissionVersion') > 0,
  'SELECT 1',
  'ALTER TABLE `licenses` ADD COLUMN `permissionVersion` INT(11) NOT NULL DEFAULT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `pos_staff` (
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
  `lastLoginAt` DATETIME DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pos_staff_store_mobile` (`licenseId`, `mobileNumber`),
  KEY `idx_pos_staff_org` (`organization_id`),
  KEY `idx_pos_staff_status` (`licenseId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_role_permission` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `role` VARCHAR(32) NOT NULL,
  `permissionKey` VARCHAR(64) NOT NULL,
  `allowed` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pos_role_perm` (`role`, `permissionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_staff_permission_override` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `staffId` INT(10) UNSIGNED NOT NULL,
  `permissionKey` VARCHAR(64) NOT NULL,
  `overrideState` VARCHAR(8) NOT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_perm` (`staffId`, `permissionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_devices` (
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
  UNIQUE KEY `uk_pos_device_store` (`licenseId`, `deviceId`),
  KEY `idx_pos_device_org` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_sessions` (
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
  UNIQUE KEY `uk_pos_session` (`sessionId`),
  KEY `idx_pos_session_staff` (`staffId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `store_printers` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Additive columns if an older store_printers table already exists.
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'store_printers' AND COLUMN_NAME = 'bluetoothAddress') > 0,
  'SELECT 1',
  'ALTER TABLE `store_printers` ADD COLUMN `bluetoothAddress` VARCHAR(64) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'store_printers' AND COLUMN_NAME = 'usbIdentifier') > 0,
  'SELECT 1',
  'ALTER TABLE `store_printers` ADD COLUMN `usbIdentifier` VARCHAR(191) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'store_printers' AND COLUMN_NAME = 'usbName') > 0,
  'SELECT 1',
  'ALTER TABLE `store_printers` ADD COLUMN `usbName` VARCHAR(128) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'store_printers' AND COLUMN_NAME = 'paperSize') > 0,
  'SELECT 1',
  'ALTER TABLE `store_printers` ADD COLUMN `paperSize` VARCHAR(16) NOT NULL DEFAULT ''2-Inch'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `printer_routes` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `print_hosts` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `print_jobs` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT(11) NOT NULL,
  `licenseId` INT(10) UNSIGNED NOT NULL,
  `createdByStaffId` INT(10) UNSIGNED DEFAULT NULL,
  `createdByDeviceId` VARCHAR(191) DEFAULT NULL,
  `printerId` INT(10) UNSIGNED NOT NULL,
  `hostDeviceId` VARCHAR(191) DEFAULT NULL,
  `documentType` VARCHAR(16) NOT NULL,
  `documentId` VARCHAR(64) DEFAULT NULL,
  `payload` LONGTEXT NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
  `priority` INT(11) NOT NULL DEFAULT 0,
  `retryCount` INT(11) NOT NULL DEFAULT 0,
  `errorMessage` VARCHAR(255) DEFAULT NULL,
  `idempotencyKey` VARCHAR(191) NOT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `printedAt` DATETIME DEFAULT NULL,
  `failedAt` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_job_idem` (`licenseId`, `idempotencyKey`),
  KEY `idx_print_job_queue` (`licenseId`, `status`, `printerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_audit_log` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bill/KOT company printer: paper size + Bluetooth/USB (keep existing bluetoothAddress).
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'paperSize') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `paperSize` VARCHAR(16) NOT NULL DEFAULT ''2-Inch'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotPaperSize') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `kotPaperSize` VARCHAR(16) NOT NULL DEFAULT ''2-Inch'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'billConnectionType') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `billConnectionType` VARCHAR(16) NOT NULL DEFAULT ''BLUETOOTH'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotConnectionType') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `kotConnectionType` VARCHAR(16) NOT NULL DEFAULT ''BLUETOOTH'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'billUsbIdentifier') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `billUsbIdentifier` VARCHAR(191) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'billUsbName') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `billUsbName` VARCHAR(128) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotUsbIdentifier') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `kotUsbIdentifier` VARCHAR(191) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotUsbName') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `kotUsbName` VARCHAR(128) DEFAULT NULL'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

