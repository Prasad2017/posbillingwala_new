-- =============================================================================
-- POS Billingwala — FINAL single-file server upgrade (Aug 2026)
-- File: API/migrations/server_upgrade_all.sql
--
-- Includes ALL features for online sync across POS / Owner / Dealer / Admin:
--   • Food types (Food / Beverage) on categories
--   • Product subcategories + products.subcategoryId
--   • Product portions (Half / Full / Kg prices)
--   • Bill line snapshots (portion + price on invoice lines)
--   • Beverage auto-mapping for drink-like category names
--   • API auth tokens (admin / dealer / owner / pos_licence)
--   • Production licensing (trialStartedAt, trialConsumed, deviceBoundAt)
--   • Multi-branch scope (organization_id, branch_id, device_id + grants)
--   • Sync indexes for catalog pull by customer (userId)
--   • Portion Master (name only) + product_portions.portionMasterId
--   • Mess QR tokens (walk-in / scan verify) — mess_token table
--   • Combo items (separate combo master + components + bill snapshots)
--   • Crash & Error Logs inbox (error_logs) — POS → Admin
-- =============================================================================
-- HOW TO RUN (phpMyAdmin — easiest):
--   1. Open phpMyAdmin → select your POS database
--   2. Click "Import" (or "SQL" tab)
--   3. Choose this file / paste all SQL below
--   4. Click Go / Import
--
-- HOW TO RUN (command line):
--   mysql -u YOUR_USER -p YOUR_DATABASE < server_upgrade_all.sql
--
-- =============================================================================
-- EXISTING DATA — KEEP EVERYTHING (old shops keep working)
-- =============================================================================
-- This script NEVER deletes or drops:
--   users, licenses, categories, products, invoice, invoice_final_product,
--   companys, or any other existing shop data.
--
-- What happens to OLD rows:
--   • categories  → stay; get foodTypeId = Food (drink-like names → Beverage)
--   • products    → stay; subcategoryId stays NULL until shop adds one
--   • products    → keep productPrice; portions are optional (0..N new rows)
--   • old bills   → stay; new snapshot columns are NULL on past lines
--   • login/sync  → still works; api_tokens is a new table only
--
-- Old app / new app:
--   • Old APK without portions/subcategories keeps working on same DB
--   • New APK can use Food/Beverage, subcategories, portions on same data
--
-- SAFE to run more than once (skips columns/tables already present).
-- =============================================================================

SET NAMES utf8mb4;

-- Snapshot of current data (for your peace of mind — nothing is deleted)
SELECT
  (SELECT COUNT(*) FROM `categories`) AS categories_before,
  (SELECT COUNT(*) FROM `products`) AS products_before,
  (SELECT COUNT(*) FROM `invoice`) AS invoices_before,
  (SELECT COUNT(*) FROM `invoice_final_product`) AS invoice_lines_before,
  (SELECT COUNT(*) FROM `licenses`) AS licenses_before,
  (SELECT COUNT(*) FROM `users`) AS users_before;


-- =============================================================================
-- STEP 1 of 9 — Food types table + seed Food / Beverage
-- =============================================================================

CREATE TABLE IF NOT EXISTS `food_types` (
  `foodTypeId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `foodTypeName` varchar(64) NOT NULL,
  `foodTypeCode` varchar(32) NOT NULL,
  `foodTypeSortOrder` int(11) NOT NULL DEFAULT 0,
  `foodTypeStatus` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`foodTypeId`),
  UNIQUE KEY `idx_food_type_code` (`foodTypeCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `food_types` (`foodTypeName`, `foodTypeCode`, `foodTypeSortOrder`, `foodTypeStatus`, `created_at`)
SELECT 'Food', 'food', 1, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `food_types` WHERE `foodTypeCode` = 'food');

INSERT INTO `food_types` (`foodTypeName`, `foodTypeCode`, `foodTypeSortOrder`, `foodTypeStatus`, `created_at`)
SELECT 'Beverage', 'beverage', 2, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `food_types` WHERE `foodTypeCode` = 'beverage');

-- Add categories.foodTypeId (skip if already there)
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'categories'
       AND COLUMN_NAME = 'foodTypeId') > 0,
    'SELECT ''OK: categories.foodTypeId already exists'' AS msg',
    'ALTER TABLE `categories` ADD COLUMN `foodTypeId` int(10) UNSIGNED DEFAULT NULL AFTER `categoryName`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Default existing categories to Food
UPDATE `categories` c
INNER JOIN `food_types` ft ON ft.foodTypeCode = 'food'
SET c.foodTypeId = ft.foodTypeId
WHERE c.foodTypeId IS NULL;

-- =============================================================================
-- STEP 2 of 9 — Product subcategories
-- =============================================================================

CREATE TABLE IF NOT EXISTS `product_subcategories` (
  `subcategoryId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `categoryId` int(10) UNSIGNED NOT NULL,
  `subcategoryName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `subcategoryNetworkStatus` text DEFAULT NULL,
  `subcategoryStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`subcategoryId`),
  KEY `idx_subcategory_category` (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'products'
       AND COLUMN_NAME = 'subcategoryId') > 0,
    'SELECT ''OK: products.subcategoryId already exists'' AS msg',
    'ALTER TABLE `products` ADD COLUMN `subcategoryId` int(10) UNSIGNED DEFAULT NULL AFTER `categoryId`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Open price: cashier enters unit price at billing (on/off)
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'products'
       AND COLUMN_NAME = 'openPrice') > 0,
    'SELECT ''OK: products.openPrice already exists'' AS msg',
    'ALTER TABLE `products` ADD COLUMN `openPrice` VARCHAR(10) NOT NULL DEFAULT ''off'' AFTER `productPrice`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Packing charge on invoice (same pattern as discount: value + Percentage/Amount)
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice'
       AND COLUMN_NAME = 'packingCharge') > 0,
    'SELECT ''OK: invoice.packingCharge already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `packingCharge` VARCHAR(50) NOT NULL DEFAULT ''0'' AFTER `discountType`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice'
       AND COLUMN_NAME = 'packingChargeType') > 0,
    'SELECT ''OK: invoice.packingChargeType already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `packingChargeType` VARCHAR(50) NULL DEFAULT ''Percentage'' AFTER `packingCharge`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- STEP 3 of 9 — Product portions (Half / Full / Kg prices)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `product_portions` (
  `portionId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `productId` int(10) UNSIGNED NOT NULL,
  `portionName` varchar(64) NOT NULL,
  `portionPrice` decimal(16,2) NOT NULL,
  `portionSortOrder` int(11) NOT NULL DEFAULT 0,
  `portionNetworkStatus` varchar(64) DEFAULT NULL,
  `portionStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`portionId`),
  UNIQUE KEY `idx_portion_network_status` (`portionNetworkStatus`),
  KEY `idx_portion_product` (`productId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- STEP 4 of 9 — Bill line snapshots (portion name/price on invoice lines)
-- =============================================================================

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'portionId') > 0,
    'SELECT ''OK: invoice_final_product.portionId already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `portionId` int(10) UNSIGNED DEFAULT NULL AFTER `productName`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'portionName') > 0,
    'SELECT ''OK: invoice_final_product.portionName already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `portionName` varchar(64) DEFAULT NULL AFTER `portionId`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'snapshotProductName') > 0,
    'SELECT ''OK: invoice_final_product.snapshotProductName already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `snapshotProductName` text DEFAULT NULL AFTER `portionName`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice_final_product'
       AND COLUMN_NAME = 'snapshotLinePrice') > 0,
    'SELECT ''OK: invoice_final_product.snapshotLinePrice already exists'' AS msg',
    'ALTER TABLE `invoice_final_product` ADD COLUMN `snapshotLinePrice` decimal(16,2) DEFAULT NULL AFTER `snapshotProductName`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- STEP 5 of 9 — Map drink-like categories to Beverage food type
-- =============================================================================

UPDATE `categories` c
INNER JOIN `food_types` ft_food ON ft_food.foodTypeCode = 'food'
INNER JOIN `food_types` ft_bev ON ft_bev.foodTypeCode = 'beverage'
SET c.foodTypeId = ft_bev.foodTypeId
WHERE (c.foodTypeId IS NULL OR c.foodTypeId = ft_food.foodTypeId)
  AND c.categoryStatus = 'active'
  AND (
    LOWER(c.categoryName) LIKE '%beverage%'
    OR LOWER(c.categoryName) LIKE '%drink%'
    OR LOWER(c.categoryName) LIKE '%juice%'
    OR LOWER(c.categoryName) LIKE '%mocktail%'
    OR LOWER(c.categoryName) LIKE '%cocktail%'
    OR LOWER(c.categoryName) LIKE '%tea%'
    OR LOWER(c.categoryName) LIKE '%coffee%'
    OR LOWER(c.categoryName) LIKE '%shake%'
    OR LOWER(c.categoryName) LIKE '%lassi%'
    OR LOWER(c.categoryName) LIKE '%soda%'
    OR LOWER(c.categoryName) LIKE '%soft%'
    OR LOWER(c.categoryName) LIKE '%cold%'
    OR LOWER(c.categoryName) LIKE '%water%'
    OR LOWER(c.categoryName) LIKE '%milk%'
  );

-- =============================================================================
-- STEP 6 of 9 — API auth tokens (all app roles)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `api_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `token_hash` CHAR(64) NOT NULL,
  `actor_type` ENUM('pos_licence','owner','dealer','admin') NOT NULL,
  `actor_id` INT UNSIGNED NOT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `expires_at` DATETIME NOT NULL,
  `last_used_at` DATETIME NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_actor` (`actor_type`, `actor_id`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- STEP 7 of 9 — Production licensing (signed offline payload, trial anti-restart)
-- =============================================================================

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'trialStartedAt') > 0,
    'SELECT ''OK: licenses.trialStartedAt already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `trialStartedAt` datetime DEFAULT NULL AFTER `expiryDate`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'trialConsumed') > 0,
    'SELECT ''OK: licenses.trialConsumed already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `trialConsumed` tinyint(1) NOT NULL DEFAULT 0 AFTER `trialStartedAt`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'deviceBoundAt') > 0,
    'SELECT ''OK: licenses.deviceBoundAt already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `deviceBoundAt` datetime DEFAULT NULL AFTER `trialConsumed`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- STEP 8 of 9 — Multi-branch / franchise scope (P7)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `branch_access_grants` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL,
  `source_branch_id` INT UNSIGNED NOT NULL COMMENT 'licenses.id receiving access',
  `target_branch_id` INT UNSIGNED NOT NULL COMMENT 'licenses.id whose data may be read',
  `granted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `granted_by` VARCHAR(50) NOT NULL DEFAULT 'dealer',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_branch_grant` (`source_branch_id`, `target_branch_id`),
  KEY `idx_org` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- invoice
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK invoice.organization_id'' AS msg',
  'ALTER TABLE `invoice` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `licenseId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK invoice.branch_id'' AS msg',
  'ALTER TABLE `invoice` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK invoice.device_id'' AS msg',
  'ALTER TABLE `invoice` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- invoice_final_product (branch scope columns — snapshot columns added in step 4)
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_final_product' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `invoice_final_product` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `invoiceProductId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_final_product' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `invoice_final_product` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_final_product' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `invoice_final_product` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- inventory, expenses (userId column stores licence id in POS sync)
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventory' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `inventory` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventory' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `inventory` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inventory' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `inventory` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'expenses' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `expenses` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'expenses' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `expenses` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'expenses' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `expenses` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- mess_*
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_member` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_member` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_member` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member_payment' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_member_payment` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member_payment' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_member_payment` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_member_payment' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_member_payment` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_invoice' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_invoice` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `userId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_invoice' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_invoice` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_invoice' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `mess_invoice` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- companys, company_printer_setting
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companys' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `companys` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `licenseId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companys' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `companys` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companys' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `companys` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'organization_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `organization_id` INT NULL DEFAULT NULL AFTER `licenseId`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'branch_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `branch_id` INT NULL DEFAULT NULL AFTER `organization_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'device_id') > 0,
  'SELECT ''OK'' AS msg',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT NULL AFTER `branch_id`'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Backfill from licenses (single-branch rows get org + branch + device automatically)
UPDATE `invoice` i
INNER JOIN `licenses` l ON l.`id` = i.`licenseId`
SET i.`organization_id` = l.`userId`,
    i.`branch_id` = l.`id`,
    i.`device_id` = COALESCE(l.`android_device_id`, i.`device_id`)
WHERE i.`organization_id` IS NULL OR i.`branch_id` IS NULL;

UPDATE `invoice_final_product` fp
INNER JOIN `invoice` i ON i.`invoiceNumber` = fp.`invoiceNumber`
SET fp.`organization_id` = i.`organization_id`,
    fp.`branch_id` = i.`branch_id`,
    fp.`device_id` = i.`device_id`
WHERE fp.`organization_id` IS NULL OR fp.`branch_id` IS NULL;

UPDATE `inventory` inv
INNER JOIN `licenses` l ON l.`id` = inv.`userId`
SET inv.`organization_id` = l.`userId`,
    inv.`branch_id` = l.`id`,
    inv.`device_id` = COALESCE(l.`android_device_id`, inv.`device_id`)
WHERE inv.`organization_id` IS NULL OR inv.`branch_id` IS NULL;

UPDATE `expenses` e
INNER JOIN `licenses` l ON l.`id` = e.`userId`
SET e.`organization_id` = l.`userId`,
    e.`branch_id` = l.`id`,
    e.`device_id` = COALESCE(l.`android_device_id`, e.`device_id`)
WHERE e.`organization_id` IS NULL OR e.`branch_id` IS NULL;

UPDATE `mess_member` m
INNER JOIN `licenses` l ON l.`id` = m.`userId`
SET m.`organization_id` = l.`userId`,
    m.`branch_id` = l.`id`,
    m.`device_id` = COALESCE(l.`android_device_id`, m.`device_id`)
WHERE m.`organization_id` IS NULL OR m.`branch_id` IS NULL;

UPDATE `mess_member_payment` mp
INNER JOIN `licenses` l ON l.`id` = mp.`userId`
SET mp.`organization_id` = l.`userId`,
    mp.`branch_id` = l.`id`,
    mp.`device_id` = COALESCE(l.`android_device_id`, mp.`device_id`)
WHERE mp.`organization_id` IS NULL OR mp.`branch_id` IS NULL;

UPDATE `mess_invoice` mi
INNER JOIN `licenses` l ON l.`id` = mi.`userId`
SET mi.`organization_id` = l.`userId`,
    mi.`branch_id` = l.`id`,
    mi.`device_id` = COALESCE(l.`android_device_id`, mi.`device_id`)
WHERE mi.`organization_id` IS NULL OR mi.`branch_id` IS NULL;

UPDATE `companys` c
INNER JOIN `licenses` l ON l.`id` = c.`licenseId`
SET c.`organization_id` = l.`userId`,
    c.`branch_id` = l.`id`,
    c.`device_id` = COALESCE(l.`android_device_id`, c.`device_id`)
WHERE c.`organization_id` IS NULL OR c.`branch_id` IS NULL;

UPDATE `company_printer_setting` ps
INNER JOIN `licenses` l ON l.`id` = ps.`licenseId`
SET ps.`organization_id` = l.`userId`,
    ps.`branch_id` = l.`id`,
    ps.`device_id` = COALESCE(l.`android_device_id`, ps.`device_id`)
WHERE ps.`organization_id` IS NULL OR ps.`branch_id` IS NULL;

-- =============================================================================
-- STEP 9 of 9 — Sync indexes (customer-wise catalog pull)
-- =============================================================================

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_subcategories' AND INDEX_NAME = 'idx_subcategory_user') > 0,
  'SELECT ''OK idx_subcategory_user'' AS msg',
  'ALTER TABLE `product_subcategories` ADD KEY `idx_subcategory_user` (`userId`)'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_portions' AND INDEX_NAME = 'idx_portion_user') > 0,
  'SELECT ''OK idx_portion_user'' AS msg',
  'ALTER TABLE `product_portions` ADD KEY `idx_portion_user` (`userId`)'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND INDEX_NAME = 'idx_category_user_food') > 0,
  'SELECT ''OK idx_category_user_food'' AS msg',
  'ALTER TABLE `categories` ADD KEY `idx_category_user_food` (`userId`, `foodTypeId`)'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND INDEX_NAME = 'idx_invoice_branch_date') > 0,
  'SELECT ''OK idx_invoice_branch_date'' AS msg',
  'ALTER TABLE `invoice` ADD KEY `idx_invoice_branch_date` (`branch_id`, `invoiceDate`)'
)); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =============================================================================
-- STEP 10 — Portion Master (name only) + Product+Portion price link
-- =============================================================================

CREATE TABLE IF NOT EXISTS `portion_master` (
  `portionMasterId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `portionName` varchar(64) NOT NULL,
  `portionMasterNetworkStatus` varchar(64) DEFAULT NULL,
  `portionMasterStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`portionMasterId`),
  UNIQUE KEY `idx_portion_master_network` (`portionMasterNetworkStatus`),
  KEY `idx_portion_master_user` (`userId`),
  KEY `idx_portion_master_user_name` (`userId`, `portionName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'product_portions'
       AND COLUMN_NAME = 'portionMasterId') > 0,
    'SELECT ''OK: product_portions.portionMasterId already exists'' AS msg',
    'ALTER TABLE `product_portions` ADD COLUMN `portionMasterId` int(10) UNSIGNED DEFAULT NULL AFTER `productId`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'product_portions'
       AND INDEX_NAME = 'idx_product_portion_master') > 0,
    'SELECT ''OK: idx_product_portion_master already exists'' AS msg',
    'ALTER TABLE `product_portions` ADD KEY `idx_product_portion_master` (`portionMasterId`)'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO `portion_master` (`userId`, `portionName`, `portionMasterNetworkStatus`, `portionMasterStatus`, `created_at`, `updated_at`)
SELECT
  pp.`userId`,
  MIN(TRIM(pp.`portionName`)),
  CONCAT('mig-', pp.`userId`, '-', MD5(LOWER(TRIM(pp.`portionName`)))),
  'active',
  NOW(),
  NOW()
FROM `product_portions` pp
WHERE pp.`portionName` IS NOT NULL
  AND TRIM(pp.`portionName`) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM `portion_master` pm
    WHERE pm.`userId` <=> pp.`userId`
      AND LOWER(TRIM(pm.`portionName`)) = LOWER(TRIM(pp.`portionName`))
  )
GROUP BY pp.`userId`, LOWER(TRIM(pp.`portionName`));

UPDATE `product_portions` pp
INNER JOIN `portion_master` pm
  ON pm.`userId` <=> pp.`userId`
 AND LOWER(TRIM(pm.`portionName`)) = LOWER(TRIM(pp.`portionName`))
SET pp.`portionMasterId` = pm.`portionMasterId`
WHERE pp.`portionMasterId` IS NULL
  OR pp.`portionMasterId` = 0;

UPDATE `product_portions` pp
INNER JOIN (
  SELECT `productId`, `portionMasterId`, MIN(`portionId`) AS keepId
  FROM `product_portions`
  WHERE `portionMasterId` IS NOT NULL AND `portionMasterId` > 0
  GROUP BY `productId`, `portionMasterId`
  HAVING COUNT(*) > 1
) d ON d.`productId` = pp.`productId` AND d.`portionMasterId` = pp.`portionMasterId`
SET pp.`portionStatus` = 'deactive',
    pp.`portionMasterId` = NULL
WHERE pp.`portionId` <> d.keepId;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'product_portions'
       AND INDEX_NAME = 'uniq_product_portion_master') > 0,
    'SELECT ''OK: uniq_product_portion_master already exists'' AS msg',
    'ALTER TABLE `product_portions` ADD UNIQUE KEY `uniq_product_portion_master` (`productId`, `portionMasterId`)'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- STEP 11 — Mess QR tokens (walk-in + scan verify)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `mess_token` (
  `tokenId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `tokenCode` varchar(64) NOT NULL,
  `memberId` varchar(64) DEFAULT NULL,
  `memberName` text NOT NULL,
  `memberMobile` varchar(32) DEFAULT NULL,
  `memberType` varchar(16) NOT NULL DEFAULT 'walk_in',
  `messType` varchar(32) NOT NULL,
  `tokenAmount` varchar(32) DEFAULT '0',
  `tokenDate` datetime NOT NULL,
  `verifiedDate` datetime DEFAULT NULL,
  `tokenNetworkStatus` varchar(64) NOT NULL,
  `tokenStatus` varchar(16) NOT NULL DEFAULT 'active',
  `verifyNetworkStatus` varchar(64) DEFAULT NULL,
  `syncStatus` varchar(8) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`tokenId`),
  UNIQUE KEY `uq_mess_token_code` (`tokenCode`),
  UNIQUE KEY `uq_mess_token_network` (`tokenNetworkStatus`),
  KEY `idx_mess_token_user_date` (`userId`, `tokenDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- STEP 12 — Fixed Combo Items (separate master, not Product columns)
-- =============================================================================

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

-- =============================================================================
-- STEP 13 — Structured Store Details (companys)
-- Additive only. Legacy companyName / companyAddress / companyMobile kept.
-- =============================================================================

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'shopName1') > 0,
    'SELECT ''OK: companys.shopName1 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `shopName1` VARCHAR(255) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'shopName2') > 0,
    'SELECT ''OK: companys.shopName2 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `shopName2` VARCHAR(255) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'addressLine1') > 0,
    'SELECT ''OK: companys.addressLine1 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `addressLine1` TEXT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'addressLine2') > 0,
    'SELECT ''OK: companys.addressLine2 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `addressLine2` TEXT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'addressLine3') > 0,
    'SELECT ''OK: companys.addressLine3 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `addressLine3` TEXT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'phoneNo1') > 0,
    'SELECT ''OK: companys.phoneNo1 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `phoneNo1` VARCHAR(32) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'phoneNo2') > 0,
    'SELECT ''OK: companys.phoneNo2 already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `phoneNo2` VARCHAR(32) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `companys`
SET `shopName1` = `companyName`
WHERE (`shopName1` IS NULL OR TRIM(`shopName1`) = '')
  AND `companyName` IS NOT NULL
  AND TRIM(`companyName`) <> '';

UPDATE `companys`
SET `addressLine1` = `companyAddress`
WHERE (`addressLine1` IS NULL OR TRIM(`addressLine1`) = '')
  AND (`addressLine2` IS NULL OR TRIM(`addressLine2`) = '')
  AND (`addressLine3` IS NULL OR TRIM(`addressLine3`) = '')
  AND `companyAddress` IS NOT NULL
  AND TRIM(`companyAddress`) <> '';

UPDATE `companys`
SET `phoneNo1` = `companyMobile`
WHERE (`phoneNo1` IS NULL OR TRIM(`phoneNo1`) = '')
  AND `companyMobile` IS NOT NULL
  AND TRIM(`companyMobile`) <> '';

-- =============================================================================
-- P11 — Crash & Error Logs (POS → Admin inbox)
-- =============================================================================

CREATE TABLE IF NOT EXISTS `error_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `fingerprint` CHAR(64) NOT NULL,
  `occurrence_count` INT UNSIGNED NOT NULL DEFAULT 1,
  `first_seen_at` DATETIME NOT NULL,
  `last_seen_at` DATETIME NOT NULL,
  `error_type` VARCHAR(32) NOT NULL DEFAULT 'APPLICATION',
  `severity` VARCHAR(16) NOT NULL DEFAULT 'ERROR',
  `error_category` VARCHAR(64) NULL DEFAULT NULL,
  `summary` VARCHAR(512) NOT NULL DEFAULT '',
  `app_type` VARCHAR(32) NOT NULL DEFAULT 'POS',
  `app_version` VARCHAR(32) NULL DEFAULT NULL,
  `customer_id` VARCHAR(64) NULL DEFAULT NULL,
  `shop_name` VARCHAR(255) NULL DEFAULT NULL,
  `branch_label` VARCHAR(255) NULL DEFAULT NULL,
  `device_name` VARCHAR(255) NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `user_label` VARCHAR(255) NULL DEFAULT NULL,
  `screen_name` VARCHAR(255) NULL DEFAULT NULL,
  `activity_name` VARCHAR(255) NULL DEFAULT NULL,
  `fragment_name` VARCHAR(255) NULL DEFAULT NULL,
  `user_action` VARCHAR(512) NULL DEFAULT NULL,
  `what_happened` TEXT NULL,
  `user_flow` TEXT NULL,
  `breadcrumbs` MEDIUMTEXT NULL,
  `api_method` VARCHAR(16) NULL DEFAULT NULL,
  `api_url` VARCHAR(1024) NULL DEFAULT NULL,
  `http_status` INT NULL DEFAULT NULL,
  `request_body` MEDIUMTEXT NULL,
  `response_body` MEDIUMTEXT NULL,
  `request_size` INT UNSIGNED NULL DEFAULT NULL,
  `response_size` INT UNSIGNED NULL DEFAULT NULL,
  `request_duration_ms` INT UNSIGNED NULL DEFAULT NULL,
  `printer_type` VARCHAR(64) NULL DEFAULT NULL,
  `printer_model` VARCHAR(128) NULL DEFAULT NULL,
  `printer_connection` VARCHAR(64) NULL DEFAULT NULL,
  `print_operation` VARCHAR(128) NULL DEFAULT NULL,
  `original_error_message` MEDIUMTEXT NULL,
  `original_exception_class` VARCHAR(512) NULL DEFAULT NULL,
  `original_stack_trace` MEDIUMTEXT NULL,
  `original_error_code` VARCHAR(128) NULL DEFAULT NULL,
  `original_api_response` MEDIUMTEXT NULL,
  `resolution_notes` TEXT NULL,
  `resolved_at` DATETIME NULL DEFAULT NULL,
  `resolved_by` VARCHAR(128) NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_error_logs_fingerprint` (`fingerprint`),
  KEY `idx_error_logs_last_seen` (`last_seen_at`),
  KEY `idx_error_logs_severity` (`severity`),
  KEY `idx_error_logs_type` (`error_type`),
  KEY `idx_error_logs_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- p12: POS live presence (lastLoginAt on licenses)
SET @db := DATABASE();
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'lastLoginAt') > 0,
    'SELECT ''OK: licenses.lastLoginAt already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `lastLoginAt` datetime DEFAULT NULL AFTER `deviceBoundAt`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'licenses' AND INDEX_NAME = 'idx_licenses_last_login') > 0,
    'SELECT ''OK: idx_licenses_last_login already exists'' AS msg',
    'ALTER TABLE `licenses` ADD KEY `idx_licenses_last_login` (`lastLoginAt`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- p13: POS support tickets linked to licence / shop / device
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'admin_support_tickets' AND COLUMN_NAME = 'licence_id') > 0,
    'SELECT ''OK: admin_support_tickets.licence_id already exists'' AS msg',
    'ALTER TABLE `admin_support_tickets` ADD COLUMN `licence_id` INT UNSIGNED NULL DEFAULT NULL AFTER `status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'admin_support_tickets' AND COLUMN_NAME = 'user_id') > 0,
    'SELECT ''OK: admin_support_tickets.user_id already exists'' AS msg',
    'ALTER TABLE `admin_support_tickets` ADD COLUMN `user_id` INT UNSIGNED NULL DEFAULT NULL AFTER `licence_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'admin_support_tickets' AND COLUMN_NAME = 'shop_name') > 0,
    'SELECT ''OK: admin_support_tickets.shop_name already exists'' AS msg',
    'ALTER TABLE `admin_support_tickets` ADD COLUMN `shop_name` VARCHAR(255) NULL DEFAULT '''' AFTER `user_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'admin_support_tickets' AND COLUMN_NAME = 'device_name') > 0,
    'SELECT ''OK: admin_support_tickets.device_name already exists'' AS msg',
    'ALTER TABLE `admin_support_tickets` ADD COLUMN `device_name` VARCHAR(120) NULL DEFAULT '''' AFTER `shop_name`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'admin_support_tickets' AND COLUMN_NAME = 'device_id') > 0,
    'SELECT ''OK: admin_support_tickets.device_id already exists'' AS msg',
    'ALTER TABLE `admin_support_tickets` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT '''' AFTER `device_name`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- p14: Website CMS (privacy policy, client showcase, testimonials)
CREATE TABLE IF NOT EXISTS `website_pages` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `slug` varchar(80) NOT NULL,
  `title` varchar(255) NOT NULL,
  `body_html` mediumtext NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `website_pages_slug_unique` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_clients` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `business_name` varchar(255) NOT NULL,
  `subtitle` varchar(255) NOT NULL DEFAULT '',
  `description` text,
  `logo_path` varchar(500) NOT NULL DEFAULT '',
  `photo_path` varchar(500) NOT NULL DEFAULT '',
  `cta_url` varchar(500) NOT NULL DEFAULT '',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_testimonials` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `author_name` varchar(255) NOT NULL,
  `business_name` varchar(255) NOT NULL DEFAULT '',
  `quote` text NOT NULL,
  `rating` tinyint unsigned NOT NULL DEFAULT 5,
  `photo_path` varchar(500) NOT NULL DEFAULT '',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_contact_messages` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `subject` varchar(255) NOT NULL DEFAULT '',
  `message` text NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'New',
  `source_ip` varchar(64) NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- Category / subcategory display order (POS billing drag-reorder)
-- =============================================================================

SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND COLUMN_NAME = 'categorySortOrder'
);
SET @sql := IF(
  @col_exists > 0,
  'SELECT ''OK: categories.categorySortOrder already exists'' AS msg',
  'ALTER TABLE `categories` ADD COLUMN `categorySortOrder` int(11) NOT NULL DEFAULT 0 AFTER `categoryName`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `categories`
SET `categorySortOrder` = `categoryId`
WHERE IFNULL(`categorySortOrder`, 0) = 0;

SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'product_subcategories'
    AND COLUMN_NAME = 'subcategorySortOrder'
);
SET @sql := IF(
  @col_exists > 0,
  'SELECT ''OK: product_subcategories.subcategorySortOrder already exists'' AS msg',
  'ALTER TABLE `product_subcategories` ADD COLUMN `subcategorySortOrder` int(11) NOT NULL DEFAULT 0 AFTER `subcategoryName`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `product_subcategories`
SET `subcategorySortOrder` = `subcategoryId`
WHERE IFNULL(`subcategorySortOrder`, 0) = 0;

-- =============================================================================
-- DONE — verify upgrade + existing data still present
-- =============================================================================

SELECT 'Upgrade finished — existing shop data kept' AS status;

-- Schema flags (each should be 1)
SELECT
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_types') AS food_types_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_subcategories') AS subcategories_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_portions') AS portions_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portion_master') AS portion_master_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_portions' AND COLUMN_NAME = 'portionMasterId') AS product_portion_master_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mess_token') AS mess_token_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'combos') AS combos_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'combo_items') AS combo_items_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_combo_items') AS invoice_combo_items_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_final_product' AND COLUMN_NAME = 'invoiceItemType') AS invoice_item_type_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'api_tokens') AS api_tokens_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'branch_access_grants') AS branch_grants_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'foodTypeId') AS categories_foodTypeId_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'categorySortOrder') AS categories_sort_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_subcategories' AND COLUMN_NAME = 'subcategorySortOrder') AS subcategories_sort_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'subcategoryId') AS products_subcategoryId_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'openPrice') AS products_openPrice_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_final_product' AND COLUMN_NAME = 'portionId') AS invoice_portionId_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice_final_product' AND COLUMN_NAME = 'snapshotLinePrice') AS invoice_snapshot_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'trialStartedAt') AS licenses_trial_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'branch_id') AS invoice_branch_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companys' AND COLUMN_NAME = 'shopName1') AS companys_shopName1_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companys' AND COLUMN_NAME = 'addressLine1') AS companys_addressLine1_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'companys' AND COLUMN_NAME = 'phoneNo1') AS companys_phoneNo1_ok,
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'error_logs') AS error_logs_ok;

-- Business data still there (compare to first SELECT — counts must match)
SELECT
  (SELECT COUNT(*) FROM `categories`) AS categories_after,
  (SELECT COUNT(*) FROM `products`) AS products_after,
  (SELECT COUNT(*) FROM `invoice`) AS invoices_after,
  (SELECT COUNT(*) FROM `invoice_final_product`) AS invoice_lines_after,
  (SELECT COUNT(*) FROM `licenses`) AS licenses_after,
  (SELECT COUNT(*) FROM `users`) AS users_after;
-- before and after counts for categories/products/invoice/licenses/users must be EQUAL
