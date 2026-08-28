-- POS device presence: last login / last seen on licenses
SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'licenses' AND COLUMN_NAME = 'lastLoginAt') > 0,
    'SELECT ''OK: licenses.lastLoginAt already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `lastLoginAt` datetime DEFAULT NULL AFTER `deviceBoundAt`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'licenses' AND INDEX_NAME = 'idx_licenses_last_login') > 0,
    'SELECT ''OK: idx_licenses_last_login already exists'' AS msg',
    'ALTER TABLE `licenses` ADD KEY `idx_licenses_last_login` (`lastLoginAt`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
