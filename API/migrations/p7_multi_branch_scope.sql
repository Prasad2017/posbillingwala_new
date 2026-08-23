-- P7: Multi-branch / franchise — additive scope columns + cross-branch grants
-- Safe for existing single-branch customers: backfills from licenses; no DROP.

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- branch_access_grants — explicit cross-branch read authorization
-- -----------------------------------------------------------------------------
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

-- Helper: add column if missing (MySQL 5.7+ compatible)
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

-- invoice_final_product
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

-- -----------------------------------------------------------------------------
-- Backfill from licenses (single-branch rows get org + branch + device automatically)
-- -----------------------------------------------------------------------------
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

SELECT 'P7 multi-branch scope migration complete' AS status;
