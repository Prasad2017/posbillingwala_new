-- =============================================================================
-- Reset AUTO_INCREMENT after user (or other) deletes
-- File: API/migrations/reset_auto_increment_after_delete.sql
--
-- Problem:
--   After DELETE, MySQL / MariaDB still hands out the old high AUTO_INCREMENT.
--   Example: last user id was 520, you delete shops, next create still gets 521.
--
-- After this script:
--   Next INSERT uses MAX(id) + 1.
--   If the table is empty, next id is 1.
--
-- This does NOT change existing ids (1, 6, 63, 70, 77 stay as they are).
-- Remapping those would break licenses, invoices, products, and the apps.
--
-- phpMyAdmin: select the database → SQL tab → run PREVIEW, then RESET.
-- Backup first.
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- PREVIEW (safe)
-- ---------------------------------------------------------------------------

SELECT id, role_id, name, shopName, contact_number
FROM `users`
ORDER BY id;

SELECT
  (SELECT IFNULL(MAX(`id`), 0) FROM `users`) AS users_max_id,
  (SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') AS users_current_next_id,
  (SELECT IFNULL(MAX(`id`), 0) FROM `licenses`) AS licenses_max_id,
  (SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses') AS licenses_current_next_id;

-- STOP after preview. Run RESET when ready.
-- ---------------------------------------------------------------------------
-- RESET — users + licenses (run this after deleting users)
-- ---------------------------------------------------------------------------

SET @users_next = (SELECT IFNULL(MAX(`id`), 0) + 1 FROM `users`);
SET @sql = CONCAT('ALTER TABLE `users` AUTO_INCREMENT = ', @users_next);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @licenses_next = (SELECT IFNULL(MAX(`id`), 0) + 1 FROM `licenses`);
SET @sql = CONCAT('ALTER TABLE `licenses` AUTO_INCREMENT = ', @licenses_next);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT
  'users' AS tbl,
  (SELECT IFNULL(MAX(`id`), 0) FROM `users`) AS max_id,
  (SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') AS next_create_id
UNION ALL
SELECT
  'licenses',
  (SELECT IFNULL(MAX(`id`), 0) FROM `licenses`),
  (SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'licenses');

-- ---------------------------------------------------------------------------
-- OPTIONAL: reset AUTO_INCREMENT on EVERY table in this database
-- Run separately if phpMyAdmin complains about DELIMITER.
-- After it exists, later deletes only need:
--   CALL `posbill_reset_auto_increments`();
-- ---------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS `posbill_reset_auto_increments`;

DELIMITER $$

CREATE PROCEDURE `posbill_reset_auto_increments`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE tbl_name VARCHAR(64);
  DECLARE col_name VARCHAR(64);
  DECLARE next_id BIGINT UNSIGNED;

  DECLARE cur CURSOR FOR
    SELECT c.TABLE_NAME, c.COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS c
    INNER JOIN INFORMATION_SCHEMA.TABLES t
      ON t.TABLE_SCHEMA = c.TABLE_SCHEMA
     AND t.TABLE_NAME = c.TABLE_NAME
    WHERE c.TABLE_SCHEMA = DATABASE()
      AND c.EXTRA LIKE '%auto_increment%'
      AND t.TABLE_TYPE = 'BASE TABLE';

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;

  reset_loop: LOOP
    FETCH cur INTO tbl_name, col_name;
    IF done = 1 THEN
      LEAVE reset_loop;
    END IF;

    SET @max_sql = CONCAT(
      'SELECT IFNULL(MAX(`', col_name, '`), 0) + 1 INTO @next_id FROM `', tbl_name, '`'
    );
    PREPARE stmt_max FROM @max_sql;
    EXECUTE stmt_max;
    DEALLOCATE PREPARE stmt_max;

    SET next_id = @next_id;
    IF next_id < 1 THEN
      SET next_id = 1;
    END IF;

    SET @alter_sql = CONCAT(
      'ALTER TABLE `', tbl_name, '` AUTO_INCREMENT = ', next_id
    );
    PREPARE stmt_alter FROM @alter_sql;
    EXECUTE stmt_alter;
    DEALLOCATE PREPARE stmt_alter;
  END LOOP;

  CLOSE cur;
END$$

DELIMITER ;

CALL `posbill_reset_auto_increments`();
