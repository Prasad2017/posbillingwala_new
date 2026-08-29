-- P20: Invoice packing charge (MySQL) — additive only, no DROP
-- Same pattern as discount: raw value + type (Percentage | Amount).
-- Safe to run more than once (skips columns already present).
-- #1060 Duplicate column means packingCharge is already on `invoice` — nothing to do.

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
