-- =============================================================================
-- Compact users.id only.
-- For EVERY table (users, licenses, products, invoices, ...), use instead:
--   API/migrations/resequence_all_table_ids.sql
--
-- Compact users.id to 1, 2, 3, ... (fill gaps after deletes)
-- File: API/migrations/resequence_user_ids.sql
--
-- Example:
--   users now:  1, 4, 9
--   after run:  1, 2, 3
--   next create user id = 4
--
-- Also updates every related column (licenses.userId, dealerId, organization_id,
-- catalog, invoices org, tokens, logs, ...).
-- Does NOT change licenses.id (POS devices use licence id).
--
-- BEFORE YOU RUN:
--   1. Full database backup
--   2. Owner / Dealer / Admin apps must log in again after this
--      (they store the old user id on the phone)
--   3. phpMyAdmin → database → SQL tab
--   4. Run PREVIEW first. Then run the rest.
--
-- Needs MySQL 8.0 or MariaDB 10.2+ (ROW_NUMBER).
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- PREVIEW (safe) — old id → new id
-- ---------------------------------------------------------------------------

SELECT
  u.id AS old_id,
  ROW_NUMBER() OVER (ORDER BY u.id) AS new_id,
  u.role_id,
  u.name,
  u.shopName,
  u.contact_number
FROM `users` u
ORDER BY u.id;

-- If old_id already equals new_id for every row, you can stop. Nothing to compact.
-- ---------------------------------------------------------------------------
-- APPLY
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS `tmp_user_id_map`;
CREATE TABLE `tmp_user_id_map` (
  `old_id` INT UNSIGNED NOT NULL PRIMARY KEY,
  `new_id` INT UNSIGNED NOT NULL,
  UNIQUE KEY `uk_new_id` (`new_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_user_id_map` (`old_id`, `new_id`)
SELECT `id`, ROW_NUMBER() OVER (ORDER BY `id`)
FROM `users`;

-- Nothing to do if already sequential
SELECT
  SUM(old_id <> new_id) AS rows_that_will_change,
  COUNT(*) AS total_users
FROM `tmp_user_id_map`;

DROP PROCEDURE IF EXISTS `posbill_remap_user_col`;

DELIMITER $$

CREATE PROCEDURE `posbill_remap_user_col`(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_skip_if_license_id TINYINT
)
BEGIN
  DECLARE col_exists INT DEFAULT 0;

  SELECT COUNT(*) INTO col_exists
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table
    AND COLUMN_NAME = p_column;

  IF col_exists > 0 THEN
    IF p_skip_if_license_id = 1 THEN
      SET @sql = CONCAT(
        'UPDATE `', p_table, '` t ',
        'INNER JOIN `tmp_user_id_map` m ON m.old_id = t.`', p_column, '` ',
        'LEFT JOIN `licenses` lic ON lic.`id` = t.`', p_column, '` ',
        'SET t.`', p_column, '` = m.new_id ',
        'WHERE lic.`id` IS NULL'
      );
    ELSE
      SET @sql = CONCAT(
        'UPDATE `', p_table, '` t ',
        'INNER JOIN `tmp_user_id_map` m ON m.old_id = t.`', p_column, '` ',
        'SET t.`', p_column, '` = m.new_id'
      );
    END IF;
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `posbill_remap_user_col_str`(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64)
)
BEGIN
  DECLARE col_exists INT DEFAULT 0;

  SELECT COUNT(*) INTO col_exists
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table
    AND COLUMN_NAME = p_column;

  IF col_exists > 0 THEN
    SET @sql = CONCAT(
      'UPDATE `', p_table, '` t ',
      'INNER JOIN `tmp_user_id_map` m ',
      '  ON t.`', p_column, '` REGEXP ''^[0-9]+$'' ',
      ' AND CAST(t.`', p_column, '` AS UNSIGNED) = m.old_id ',
      'SET t.`', p_column, '` = CAST(m.new_id AS CHAR)'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

-- Move users.id out of the way so 4 → 2 cannot clash with an existing 2
SET @off = (SELECT IFNULL(MAX(`id`), 0) + 1000000 FROM `users`);
SET @sql = CONCAT('UPDATE `users` SET `id` = `id` + ', @off);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Child rows still have the OLD user ids. Map them to 1, 2, 3, ...
CALL `posbill_remap_user_col`('users', 'dealerId', 0);
CALL `posbill_remap_user_col`('licenses', 'userId', 0);

CALL `posbill_remap_user_col`('categories', 'userId', 0);
CALL `posbill_remap_user_col`('categories', 'dealerId', 0);
CALL `posbill_remap_user_col`('product_subcategories', 'userId', 0);
CALL `posbill_remap_user_col`('products', 'userId', 0);
CALL `posbill_remap_user_col`('products', 'dealerId', 0);
CALL `posbill_remap_user_col`('portion_master', 'userId', 0);
CALL `posbill_remap_user_col`('product_portions', 'userId', 0);
CALL `posbill_remap_user_col`('combos', 'userId', 0);
CALL `posbill_remap_user_col`('combo_items', 'userId', 0);

CALL `posbill_remap_user_col`('branch_access_grants', 'organization_id', 0);
CALL `posbill_remap_user_col`('companys', 'organization_id', 0);
CALL `posbill_remap_user_col`('company_printer_setting', 'organization_id', 0);
CALL `posbill_remap_user_col`('invoice', 'organization_id', 0);
CALL `posbill_remap_user_col`('invoice_final_product', 'organization_id', 0);

-- These userId columns are often a licence id. Only change when it is not a licence id.
CALL `posbill_remap_user_col`('inventory', 'userId', 1);
CALL `posbill_remap_user_col`('inventory', 'organization_id', 0);
CALL `posbill_remap_user_col`('expenses', 'userId', 1);
CALL `posbill_remap_user_col`('expenses', 'organization_id', 0);
CALL `posbill_remap_user_col`('mess_member', 'userId', 1);
CALL `posbill_remap_user_col`('mess_member', 'organization_id', 0);
CALL `posbill_remap_user_col`('mess_member_payment', 'userId', 1);
CALL `posbill_remap_user_col`('mess_member_payment', 'organization_id', 0);
CALL `posbill_remap_user_col`('mess_invoice', 'userId', 1);
CALL `posbill_remap_user_col`('mess_invoice', 'organization_id', 0);
CALL `posbill_remap_user_col`('mess_token', 'userId', 1);
CALL `posbill_remap_user_col`('mess_token', 'organization_id', 0);

CALL `posbill_remap_user_col`('catalog_import_sessions', 'customerId', 0);
CALL `posbill_remap_user_col`('catalog_import_sessions', 'actorId', 0);
CALL `posbill_remap_user_col`('catalog_export_history', 'customerId', 0);
CALL `posbill_remap_user_col`('catalog_export_history', 'actorId', 0);

CALL `posbill_remap_user_col`('admin_support_tickets', 'user_id', 0);
CALL `posbill_remap_user_col`('personal_access_tokens', 'tokenable_id', 0);

-- Owner / dealer / admin tokens (not POS licence tokens)
UPDATE `api_tokens` t
INNER JOIN `tmp_user_id_map` m ON m.old_id = t.`actor_id`
SET t.`actor_id` = m.new_id
WHERE t.`actor_type` IN ('owner', 'dealer', 'admin');

CALL `posbill_remap_user_col_str`('error_logs', 'customer_id');
CALL `posbill_remap_user_col_str`('admin_crash_logs', 'user_id');

-- Put users.id onto 1, 2, 3, ...
SET @sql = CONCAT(
  'UPDATE `users` u ',
  'INNER JOIN `tmp_user_id_map` m ON u.`id` = m.old_id + ', @off, ' ',
  'SET u.`id` = m.new_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @users_next = (SELECT IFNULL(MAX(`id`), 0) + 1 FROM `users`);
SET @sql = CONCAT('ALTER TABLE `users` AUTO_INCREMENT = ', @users_next);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- VERIFY
-- ---------------------------------------------------------------------------

SELECT id, role_id, name, shopName, dealerId, contact_number
FROM `users`
ORDER BY id;

SELECT
  (SELECT IFNULL(MAX(`id`), 0) FROM `users`) AS users_max_id,
  (SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') AS next_new_user_id;

SELECT l.id AS license_id, l.userId AS owner_user_id, l.userName, u.name AS user_name
FROM `licenses` l
LEFT JOIN `users` u ON u.id = l.userId
ORDER BY l.userId, l.id;

SELECT COUNT(*) AS licenses_with_missing_user
FROM `licenses` l
LEFT JOIN `users` u ON u.id = l.userId
WHERE u.id IS NULL;

DROP PROCEDURE IF EXISTS `posbill_remap_user_col`;
DROP PROCEDURE IF EXISTS `posbill_remap_user_col_str`;
DROP TABLE IF EXISTS `tmp_user_id_map`;
