-- Split settlement: cashAmount + upiAmount on invoice (additive only).
-- Safe to run more than once.

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice'
       AND COLUMN_NAME = 'cashAmount') > 0,
    'SELECT ''OK: invoice.cashAmount already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `cashAmount` VARCHAR(50) NOT NULL DEFAULT ''0'' AFTER `paymentMode`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice'
       AND COLUMN_NAME = 'upiAmount') > 0,
    'SELECT ''OK: invoice.upiAmount already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `upiAmount` VARCHAR(50) NOT NULL DEFAULT ''0'' AFTER `cashAmount`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
