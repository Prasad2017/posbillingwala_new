-- P30: Bluetooth / USB printer connection + 2-Inch / 3-Inch paper size.
-- Additive only. Safe to re-run. Existing bill/KOT rows keep bluetoothAddress.

SET NAMES utf8mb4;

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
