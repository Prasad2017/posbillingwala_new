-- =============================================================================
-- P22 — Shop opening / closing minutes on companys (additive, non-destructive)
-- Values are minutes from midnight (0–1439), same as the POS app.
-- =============================================================================

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'openingMinutes') > 0,
    'SELECT ''OK: companys.openingMinutes already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `openingMinutes` VARCHAR(8) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'companys'
       AND COLUMN_NAME = 'closingMinutes') > 0,
    'SELECT ''OK: companys.closingMinutes already exists'' AS msg',
    'ALTER TABLE `companys` ADD COLUMN `closingMinutes` VARCHAR(8) NULL DEFAULT NULL'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
