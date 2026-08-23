-- P6: Production licensing — trial anti-restart, device bind tracking
-- Safe to run more than once (checks INFORMATION_SCHEMA).

SET NAMES utf8mb4;

-- trialStartedAt: set on first device bind for Demo/Trial licences
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'trialStartedAt') > 0,
    'SELECT ''OK: licenses.trialStartedAt already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `trialStartedAt` datetime DEFAULT NULL AFTER `expiryDate`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- trialConsumed: once trial ends (expiry or bill cap), licence cannot restart trial
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'trialConsumed') > 0,
    'SELECT ''OK: licenses.trialConsumed already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `trialConsumed` tinyint(1) NOT NULL DEFAULT 0 AFTER `trialStartedAt`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- deviceBoundAt: first successful device bind timestamp
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'deviceBoundAt') > 0,
    'SELECT ''OK: licenses.deviceBoundAt already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `deviceBoundAt` datetime DEFAULT NULL AFTER `trialConsumed`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
