-- P32 — Invoice billed-by staff + staff salary tracking.

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'invoice'
       AND COLUMN_NAME = 'createdByStaffId') > 0,
    'SELECT ''OK: invoice.createdByStaffId already exists'' AS msg',
    'ALTER TABLE `invoice` ADD COLUMN `createdByStaffId` INT NULL DEFAULT NULL AFTER `device_id`, ADD COLUMN `createdByStaffName` VARCHAR(120) NOT NULL DEFAULT \'\' AFTER `createdByStaffId`, ADD KEY `idx_invoice_staff` (`licenseId`, `createdByStaffId`)'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'pos_staff'
       AND COLUMN_NAME = 'monthlySalary') > 0,
    'SELECT ''OK: pos_staff.monthlySalary already exists'' AS msg',
    'ALTER TABLE `pos_staff` ADD COLUMN `monthlySalary` DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER `status`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `pos_salary_payment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `licenseId` int(11) NOT NULL,
  `staffId` int(11) NOT NULL,
  `salaryMonth` char(7) NOT NULL COMMENT 'YYYY-MM',
  `amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `paidOn` date NOT NULL,
  `note` varchar(255) NOT NULL DEFAULT '',
  `createdAt` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_salary_staff_month` (`staffId`, `salaryMonth`),
  KEY `idx_salary_license_month` (`licenseId`, `salaryMonth`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
