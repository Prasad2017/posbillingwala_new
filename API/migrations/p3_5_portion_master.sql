-- P3-5: Portion Master (name only) + Product Portion junction pricing
-- Additive / idempotent. Existing product_portions rows are linked to master by name.
-- Rule: Portion Master = name only. Selling price = Product + Portion only.

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1) Portion Master — name only (no price)
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 2) Link product_portions → portion_master
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 3) Migrate existing name+price rows into Portion Master (per shop + name)
-- -----------------------------------------------------------------------------
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

-- Keep one active Product+Portion row; deactivate duplicate same master on same product
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

-- Unique Product + Portion (NULLs allowed for legacy unlinked rows)
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

SELECT
  (SELECT COUNT(*) FROM `portion_master`) AS portion_master_count,
  (SELECT COUNT(*) FROM `product_portions` WHERE `portionMasterId` IS NOT NULL AND `portionMasterId` > 0) AS product_portions_linked,
  (SELECT COUNT(*) FROM `product_portions` WHERE `portionMasterId` IS NULL OR `portionMasterId` = 0) AS product_portions_unlinked;
