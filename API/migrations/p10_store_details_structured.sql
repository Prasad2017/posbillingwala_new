-- =============================================================================
-- P10 — Structured Store Details on companys (additive, non-destructive)
-- shopName1 / shopName2 / addressLine1-3 / phoneNo1 / phoneNo2
-- Legacy companyName, companyAddress, companyMobile are KEPT.
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

-- Safe one-time copy from legacy fields (only when structured fields empty)
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
