-- P42: Print Fast Bill — when ON, cashier picks billing date for save/print/numbering.
-- Additive only. Safe to re-run. Default OFF preserves current flow.

SET NAMES utf8mb4;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'company_printer_setting' AND COLUMN_NAME = 'printFastBill') > 0,
  'SELECT 1',
  'ALTER TABLE `company_printer_setting` ADD COLUMN `printFastBill` VARCHAR(10) NULL DEFAULT ''off'''
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
