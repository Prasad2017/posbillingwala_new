-- Ensure users table has profile columns used by web admin Settings and mobile APIs.
-- Safe to run multiple times on MySQL 8+ / MariaDB.

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'address') = 0,
    'ALTER TABLE `users` ADD COLUMN `address` TEXT NULL AFTER `contact_number`',
    'SELECT ''OK: users.address exists'' AS msg'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'shopName') = 0,
    'ALTER TABLE `users` ADD COLUMN `shopName` VARCHAR(255) NULL AFTER `address`',
    'SELECT ''OK: users.shopName exists'' AS msg'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'dealerId') = 0,
    'ALTER TABLE `users` ADD COLUMN `dealerId` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `role_id`',
    'SELECT ''OK: users.dealerId exists'' AS msg'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
