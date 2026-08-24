-- P9: Fixed Combo Items (MySQL) — additive only, no DROP
-- Combo is a separate master (not a Product column). Deploy before POS clients upload combo rows.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `combos` (
  `comboId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `comboName` varchar(191) NOT NULL,
  `comboCode` varchar(64) DEFAULT NULL,
  `comboPrice` varchar(32) NOT NULL,
  `comboCGST` varchar(16) DEFAULT NULL,
  `comboSGST` varchar(16) DEFAULT NULL,
  `comboWithGSTPrice` varchar(32) DEFAULT NULL,
  `comboActiveStatus` varchar(8) NOT NULL DEFAULT '1',
  `comboNetworkStatus` varchar(64) DEFAULT NULL,
  `comboStatus` text NOT NULL DEFAULT 'active',
  `comboSortOrder` int(11) NOT NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`comboId`),
  UNIQUE KEY `idx_combo_network` (`comboNetworkStatus`),
  KEY `idx_combo_user` (`userId`),
  KEY `idx_combo_user_name` (`userId`, `comboName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `combo_items` (
  `comboItemId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `comboId` int(10) UNSIGNED NOT NULL,
  `productId` int(11) DEFAULT NULL,
  `portionId` int(11) DEFAULT NULL,
  `comboItemQuantity` varchar(16) NOT NULL DEFAULT '1',
  `comboItemSortOrder` int(11) NOT NULL DEFAULT 0,
  `comboItemNetworkStatus` varchar(64) DEFAULT NULL,
  `comboItemStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`comboItemId`),
  UNIQUE KEY `idx_combo_item_network` (`comboItemNetworkStatus`),
  KEY `idx_combo_item_combo` (`comboId`),
  KEY `idx_combo_item_product` (`productId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `invoice_combo_items` (
  `invoiceComboItemId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `invoiceNumber` varchar(64) DEFAULT NULL,
  `invoiceProductNetworkStatus` varchar(64) DEFAULT NULL,
  `comboNetworkStatus` varchar(64) DEFAULT NULL,
  `productId` int(11) DEFAULT NULL,
  `productNameSnapshot` text DEFAULT NULL,
  `portionId` int(11) DEFAULT NULL,
  `portionNameSnapshot` varchar(64) DEFAULT NULL,
  `quantity` varchar(16) NOT NULL DEFAULT '1',
  `sortOrder` int(11) NOT NULL DEFAULT 0,
  `invoiceComboItemNetworkStatus` varchar(64) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`invoiceComboItemId`),
  UNIQUE KEY `idx_invoice_combo_item_network` (`invoiceComboItemNetworkStatus`),
  KEY `idx_invoice_combo_item_invoice` (`invoiceNumber`),
  KEY `idx_invoice_combo_item_line` (`invoiceProductNetworkStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'invoiceItemType') > 0,
    'SELECT ''OK: invoice_final_product.invoiceItemType already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `invoiceItemType` varchar(16) NOT NULL DEFAULT ''PRODUCT'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'comboNetworkStatus') > 0,
    'SELECT ''OK: invoice_final_product.comboNetworkStatus already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `comboNetworkStatus` varchar(64) DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'snapshotComboComponents') > 0,
    'SELECT ''OK: invoice_final_product.snapshotComboComponents already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `snapshotComboComponents` text DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'combos') AS combos_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'combo_items') AS combo_items_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_combo_items') AS invoice_combo_items_ok;
