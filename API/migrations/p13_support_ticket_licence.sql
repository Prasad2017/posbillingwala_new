-- Link support tickets to POS licence / shop / device
SET @db := DATABASE();

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
