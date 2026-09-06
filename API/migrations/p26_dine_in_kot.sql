-- P26: Dine-In table management + KOT printer settings + settlement fields (additive only).
-- Safe to run more than once.

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1) company_printer_setting — Dine-In / KOT options
-- ---------------------------------------------------------------------------
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'duplicateBillUse') > 0,
    'SELECT ''OK: company_printer_setting.duplicateBillUse already exists'' AS msg',
    'ALTER TABLE `company_printer_setting` ADD COLUMN `duplicateBillUse` VARCHAR(10) NULL DEFAULT ''off'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotEnable') > 0,
    'SELECT ''OK: company_printer_setting.kotEnable already exists'' AS msg',
    'ALTER TABLE `company_printer_setting` ADD COLUMN `kotEnable` VARCHAR(10) NULL DEFAULT ''on'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotPrefix') > 0,
    'SELECT ''OK: company_printer_setting.kotPrefix already exists'' AS msg',
    'ALTER TABLE `company_printer_setting` ADD COLUMN `kotPrefix` VARCHAR(32) NULL DEFAULT ''KOT-'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotCopies') > 0,
    'SELECT ''OK: company_printer_setting.kotCopies already exists'' AS msg',
    'ALTER TABLE `company_printer_setting` ADD COLUMN `kotCopies` VARCHAR(8) NULL DEFAULT ''1'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotAutoPrint') > 0,
    'SELECT ''OK: company_printer_setting.kotAutoPrint already exists'' AS msg',
    'ALTER TABLE `company_printer_setting` ADD COLUMN `kotAutoPrint` VARCHAR(10) NULL DEFAULT ''off'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'kotPreview') > 0,
    'SELECT ''OK: company_printer_setting.kotPreview already exists'' AS msg',
    'ALTER TABLE `company_printer_setting` ADD COLUMN `kotPreview` VARCHAR(10) NULL DEFAULT ''on'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) invoice — dining session link + bill print retry status
-- ---------------------------------------------------------------------------
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'diningSessionId') > 0,
    'SELECT ''OK: invoice.diningSessionId already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `diningSessionId` VARCHAR(64) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'billPrintStatus') > 0,
    'SELECT ''OK: invoice.billPrintStatus already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `billPrintStatus` VARCHAR(32) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3) Dine-In masters + session / KOT tables
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dining_area` (
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
  KEY `idx_dining_area_license` (`licenseId`, `branch_id`),
  KEY `idx_dining_area_network` (`areaNetworkStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `table_type` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_table` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `dining_session` (
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
  KEY `idx_dining_session_status` (`sessionStatus`),
  KEY `idx_dining_session_local` (`localSessionId`),
  KEY `idx_dining_session_network` (`sessionNetworkStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_round` (
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
  KEY `idx_order_round_session` (`sessionId`),
  KEY `idx_order_round_license` (`licenseId`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `kot` (
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
  KEY `idx_kot_license` (`licenseId`, `branch_id`),
  KEY `idx_kot_number` (`kotNumber`),
  KEY `idx_kot_session` (`sessionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `kot_item` (
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
  KEY `idx_kot_item_kot` (`kotId`),
  KEY `idx_kot_item_license` (`licenseId`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
