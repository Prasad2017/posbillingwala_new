-- =============================================================================
-- Compact AUTO_INCREMENT ids on ALL tables and update FOREIGN KEYS to match
-- File: API/migrations/resequence_all_table_ids.sql
--
-- Example:
--   products.productId  1, 4  →  1, 2
--   inventory.productId 4     →  2   (same change on the related column)
--
-- Also covers users, licenses, categories, invoices, combos, mess, tickets, ...
--
-- BEFORE YOU RUN:
--   1. Full database backup
--   2. POS / Owner / Dealer / Admin apps must log in again after this
--   3. phpMyAdmin → database → SQL tab
--   4. Run PREVIEW first. Then run APPLY.
--
-- Needs MySQL 8.0 or MariaDB 10.2+ (ROW_NUMBER).
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- PREVIEW (safe)
-- ---------------------------------------------------------------------------

SELECT 'users' AS tbl, id AS old_id, ROW_NUMBER() OVER (ORDER BY id) AS new_id FROM `users`
UNION ALL
SELECT 'licenses', id, ROW_NUMBER() OVER (ORDER BY id) FROM `licenses`
UNION ALL
SELECT 'categories', categoryId, ROW_NUMBER() OVER (ORDER BY categoryId) FROM `categories`
UNION ALL
SELECT 'products', productId, ROW_NUMBER() OVER (ORDER BY productId) FROM `products`
UNION ALL
SELECT 'invoice', invoiceId, ROW_NUMBER() OVER (ORDER BY invoiceId) FROM `invoice`
ORDER BY tbl, old_id;

-- STOP after preview. Run APPLY to compact PKs and rewrite related columns.
-- ---------------------------------------------------------------------------
-- APPLY
-- ---------------------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;
SET @posbill_off = 10000000;

DROP TABLE IF EXISTS `posbill_id_map`;
DROP TABLE IF EXISTS `posbill_ai_tables`;
DROP TABLE IF EXISTS `posbill_fk_plan`;

CREATE TABLE `posbill_id_map` (
  `src_table` VARCHAR(64) NOT NULL,
  `old_id` BIGINT UNSIGNED NOT NULL,
  `new_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`src_table`, `old_id`),
  KEY `idx_new` (`src_table`, `new_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `posbill_ai_tables` (
  `table_name` VARCHAR(64) NOT NULL PRIMARY KEY,
  `pk_column` VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- child_col on child_table stores parent_table's primary key
CREATE TABLE `posbill_fk_plan` (
  `child_table` VARCHAR(64) NOT NULL,
  `child_col` VARCHAR(64) NOT NULL,
  `parent_table` VARCHAR(64) NOT NULL,
  `col_is_string` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`child_table`, `child_col`, `parent_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `posbill_ai_tables` (`table_name`, `pk_column`)
SELECT c.TABLE_NAME, c.COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS c
INNER JOIN INFORMATION_SCHEMA.TABLES t
  ON t.TABLE_SCHEMA = c.TABLE_SCHEMA
 AND t.TABLE_NAME = c.TABLE_NAME
WHERE c.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND c.EXTRA LIKE '%auto_increment%'
  AND c.TABLE_NAME NOT IN ('posbill_id_map', 'posbill_ai_tables', 'posbill_fk_plan');

-- 1) Real MySQL / MariaDB FOREIGN KEY constraints (if any exist on the server)
INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
SELECT k.TABLE_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_NAME, 0
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
WHERE k.TABLE_SCHEMA = DATABASE()
  AND k.REFERENCED_TABLE_NAME IS NOT NULL
  AND k.REFERENCED_COLUMN_NAME IS NOT NULL;

-- 2) Same column name as a UNIQUE primary-key name on another table
--    e.g. productId, categoryId, portionId, comboId, foodTypeId, ...
INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
SELECT c.TABLE_NAME, c.COLUMN_NAME, a.table_name, 0
FROM INFORMATION_SCHEMA.COLUMNS c
INNER JOIN `posbill_ai_tables` a
  ON a.pk_column = c.COLUMN_NAME
INNER JOIN (
  SELECT `pk_column`
  FROM `posbill_ai_tables`
  GROUP BY `pk_column`
  HAVING COUNT(*) = 1
) uniq ON uniq.pk_column = a.pk_column
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.TABLE_NAME <> a.table_name
  AND c.TABLE_NAME NOT IN ('posbill_id_map', 'posbill_ai_tables', 'posbill_fk_plan')
  AND c.COLUMN_NAME NOT IN ('id');

-- 3) Logical FKs that use a different column name than the parent PK (users.id, licenses.id, ...)
INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
SELECT c.TABLE_NAME, c.COLUMN_NAME, 'users', 0
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.COLUMN_NAME IN (
    'userId', 'dealerId', 'organization_id', 'customerId', 'actorId',
    'tokenable_id', 'user_id'
  )
  AND c.TABLE_NAME NOT IN ('posbill_id_map', 'posbill_ai_tables', 'posbill_fk_plan')
  AND NOT (
    c.TABLE_NAME IN (
      'inventory', 'expenses', 'mess_member', 'mess_member_payment',
      'mess_invoice', 'mess_token'
    ) AND c.COLUMN_NAME = 'userId'
  );

INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
SELECT c.TABLE_NAME, c.COLUMN_NAME, 'licenses', 0
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.COLUMN_NAME IN (
    'licenseId', 'licence_id', 'licenceId', 'branch_id',
    'source_branch_id', 'target_branch_id'
  )
  AND c.TABLE_NAME NOT IN ('posbill_id_map', 'posbill_ai_tables', 'posbill_fk_plan');

INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
SELECT c.TABLE_NAME, c.COLUMN_NAME, 'admin_support_tickets', 0
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.COLUMN_NAME IN ('ticket_id', 'ticketId')
  AND c.TABLE_NAME <> 'admin_support_tickets'
  AND c.TABLE_NAME NOT IN ('posbill_id_map', 'posbill_ai_tables', 'posbill_fk_plan');

INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
SELECT c.TABLE_NAME, c.COLUMN_NAME, 'mess_member', 0
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.COLUMN_NAME IN ('memberId', 'member_id')
  AND c.TABLE_NAME <> 'mess_member'
  AND c.TABLE_NAME NOT IN ('posbill_id_map', 'posbill_ai_tables', 'posbill_fk_plan')
  AND c.DATA_TYPE NOT IN ('varchar', 'char', 'text', 'tinytext', 'mediumtext', 'longtext');

INSERT IGNORE INTO `posbill_fk_plan` (`child_table`, `child_col`, `parent_table`, `col_is_string`)
VALUES
  ('error_logs', 'customer_id', 'users', 1),
  ('admin_crash_logs', 'user_id', 'users', 1),
  ('mess_token', 'memberId', 'mess_member', 1);

-- Show planned FK rewrites (safe)
SELECT child_table, child_col, parent_table, col_is_string
FROM `posbill_fk_plan`
ORDER BY parent_table, child_table, child_col;

DROP PROCEDURE IF EXISTS `posbill_build_map`;
DROP PROCEDURE IF EXISTS `posbill_offset_pk`;
DROP PROCEDURE IF EXISTS `posbill_apply_pk`;
DROP PROCEDURE IF EXISTS `posbill_reset_ai`;
DROP PROCEDURE IF EXISTS `posbill_remap_fk`;
DROP PROCEDURE IF EXISTS `posbill_remap_fk_str`;
DROP PROCEDURE IF EXISTS `posbill_remap_mixed_user_or_license`;
DROP PROCEDURE IF EXISTS `posbill_loop_ai`;
DROP PROCEDURE IF EXISTS `posbill_remap_fk_plan`;
DROP PROCEDURE IF EXISTS `posbill_remap_all_fks`;

DELIMITER $$

CREATE PROCEDURE `posbill_build_map`(IN p_table VARCHAR(64), IN p_pk VARCHAR(64))
BEGIN
  SET @sql = CONCAT(
    'INSERT INTO `posbill_id_map` (`src_table`, `old_id`, `new_id`) ',
    'SELECT ''', p_table, ''', `', p_pk, '`, ROW_NUMBER() OVER (ORDER BY `', p_pk, '`) ',
    'FROM `', p_table, '`'
  );
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END$$

CREATE PROCEDURE `posbill_offset_pk`(IN p_table VARCHAR(64), IN p_pk VARCHAR(64))
BEGIN
  SET @sql = CONCAT(
    'UPDATE `', p_table, '` SET `', p_pk, '` = `', p_pk, '` + ', @posbill_off
  );
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END$$

CREATE PROCEDURE `posbill_apply_pk`(IN p_table VARCHAR(64), IN p_pk VARCHAR(64))
BEGIN
  SET @sql = CONCAT(
    'UPDATE `', p_table, '` t ',
    'INNER JOIN `posbill_id_map` m ',
    '  ON m.src_table = ''', p_table, ''' ',
    ' AND t.`', p_pk, '` = m.old_id + ', @posbill_off, ' ',
    'SET t.`', p_pk, '` = m.new_id'
  );
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END$$

CREATE PROCEDURE `posbill_reset_ai`(IN p_table VARCHAR(64), IN p_pk VARCHAR(64))
BEGIN
  SET @sql = CONCAT(
    'SELECT IFNULL(MAX(`', p_pk, '`), 0) + 1 INTO @next_id FROM `', p_table, '`'
  );
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
  SET @sql = CONCAT('ALTER TABLE `', p_table, '` AUTO_INCREMENT = ', @next_id);
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;
END$$

CREATE PROCEDURE `posbill_remap_fk`(
  IN p_table VARCHAR(64),
  IN p_col VARCHAR(64),
  IN p_src VARCHAR(64)
)
BEGIN
  DECLARE col_ok INT DEFAULT 0;
  SELECT COUNT(*) INTO col_ok
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table
    AND COLUMN_NAME = p_col;
  IF col_ok > 0 THEN
    SET @sql = CONCAT(
      'UPDATE `', p_table, '` t ',
      'INNER JOIN `posbill_id_map` m ',
      '  ON m.src_table = ''', p_src, ''' AND m.old_id = t.`', p_col, '` ',
      'SET t.`', p_col, '` = m.new_id'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `posbill_remap_fk_str`(
  IN p_table VARCHAR(64),
  IN p_col VARCHAR(64),
  IN p_src VARCHAR(64)
)
BEGIN
  DECLARE col_ok INT DEFAULT 0;
  SELECT COUNT(*) INTO col_ok
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table
    AND COLUMN_NAME = p_col;
  IF col_ok > 0 THEN
    SET @sql = CONCAT(
      'UPDATE `', p_table, '` t ',
      'INNER JOIN `posbill_id_map` m ',
      '  ON m.src_table = ''', p_src, ''' ',
      ' AND t.`', p_col, '` REGEXP ''^[0-9]+$'' ',
      ' AND CAST(t.`', p_col, '` AS UNSIGNED) = m.old_id ',
      'SET t.`', p_col, '` = CAST(m.new_id AS CHAR)'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `posbill_remap_mixed_user_or_license`(
  IN p_table VARCHAR(64),
  IN p_col VARCHAR(64)
)
BEGIN
  DECLARE col_ok INT DEFAULT 0;
  SELECT COUNT(*) INTO col_ok
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table
    AND COLUMN_NAME = p_col;
  IF col_ok > 0 THEN
    SET @sql = CONCAT(
      'UPDATE `', p_table, '` t ',
      'LEFT JOIN `posbill_id_map` lic ON lic.src_table = ''licenses'' AND lic.old_id = t.`', p_col, '` ',
      'LEFT JOIN `posbill_id_map` usr ON usr.src_table = ''users'' AND usr.old_id = t.`', p_col, '` ',
      'SET t.`', p_col, '` = COALESCE(lic.new_id, usr.new_id) ',
      'WHERE lic.old_id IS NOT NULL OR usr.old_id IS NOT NULL'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `posbill_loop_ai`(IN p_action VARCHAR(16))
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE t VARCHAR(64);
  DECLARE c VARCHAR(64);
  DECLARE cur CURSOR FOR SELECT `table_name`, `pk_column` FROM `posbill_ai_tables`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  ai_loop: LOOP
    FETCH cur INTO t, c;
    IF done = 1 THEN
      LEAVE ai_loop;
    END IF;
    IF p_action = 'map' THEN
      CALL `posbill_build_map`(t, c);
    ELSEIF p_action = 'offset' THEN
      CALL `posbill_offset_pk`(t, c);
    ELSEIF p_action = 'apply' THEN
      CALL `posbill_apply_pk`(t, c);
    ELSEIF p_action = 'reset' THEN
      CALL `posbill_reset_ai`(t, c);
    END IF;
  END LOOP;
  CLOSE cur;
END$$

CREATE PROCEDURE `posbill_remap_fk_plan`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE t VARCHAR(64);
  DECLARE c VARCHAR(64);
  DECLARE p VARCHAR(64);
  DECLARE is_str TINYINT;
  DECLARE cur CURSOR FOR
    SELECT `child_table`, `child_col`, `parent_table`, `col_is_string`
    FROM `posbill_fk_plan`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  fk_loop: LOOP
    FETCH cur INTO t, c, p, is_str;
    IF done = 1 THEN
      LEAVE fk_loop;
    END IF;
    IF is_str = 1 THEN
      CALL `posbill_remap_fk_str`(t, c, p);
    ELSE
      CALL `posbill_remap_fk`(t, c, p);
    END IF;
  END LOOP;
  CLOSE cur;
END$$

CREATE PROCEDURE `posbill_remap_all_fks`()
BEGIN
  CALL `posbill_remap_fk_plan`();

  -- userId here is often a licence id — update to the new licence id (else user id)
  CALL `posbill_remap_mixed_user_or_license`('inventory', 'userId');
  CALL `posbill_remap_mixed_user_or_license`('expenses', 'userId');
  CALL `posbill_remap_mixed_user_or_license`('mess_member', 'userId');
  CALL `posbill_remap_mixed_user_or_license`('mess_member_payment', 'userId');
  CALL `posbill_remap_mixed_user_or_license`('mess_invoice', 'userId');
  CALL `posbill_remap_mixed_user_or_license`('mess_token', 'userId');

  IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'api_tokens') > 0 THEN
    UPDATE `api_tokens` t
    INNER JOIN `posbill_id_map` m ON m.src_table = 'licenses' AND m.old_id = t.`actor_id`
    SET t.`actor_id` = m.new_id
    WHERE t.`actor_type` = 'pos_licence';

    UPDATE `api_tokens` t
    INNER JOIN `posbill_id_map` m ON m.src_table = 'users' AND m.old_id = t.`actor_id`
    SET t.`actor_id` = m.new_id
    WHERE t.`actor_type` IN ('owner', 'dealer', 'admin');
  END IF;
END$$

DELIMITER ;

CALL `posbill_loop_ai`('map');
CALL `posbill_loop_ai`('offset');
CALL `posbill_remap_all_fks`();
CALL `posbill_loop_ai`('apply');
CALL `posbill_loop_ai`('reset');

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- VERIFY — related columns should point at the new ids
-- ---------------------------------------------------------------------------

SELECT `src_table`,
       SUM(old_id <> new_id) AS ids_changed,
       COUNT(*) AS row_count,
       MAX(new_id) AS new_max_id
FROM `posbill_id_map`
GROUP BY `src_table`
ORDER BY `src_table`;

SELECT id, role_id, name, shopName, dealerId FROM `users` ORDER BY id;
SELECT id AS license_id, userId AS owner_user_id, userName FROM `licenses` ORDER BY id;

SELECT 'licenses.userId missing users.id' AS chk, COUNT(*) AS bad
FROM `licenses` l LEFT JOIN `users` u ON u.id = l.userId WHERE u.id IS NULL
UNION ALL
SELECT 'invoice.licenseId missing licenses.id', COUNT(*)
FROM `invoice` i LEFT JOIN `licenses` l ON l.id = i.licenseId WHERE l.id IS NULL
UNION ALL
SELECT 'products.userId missing users.id', COUNT(*)
FROM `products` p LEFT JOIN `users` u ON u.id = p.userId WHERE u.id IS NULL
UNION ALL
SELECT 'products.categoryId missing categories', COUNT(*)
FROM `products` p LEFT JOIN `categories` c ON c.categoryId = p.categoryId WHERE c.categoryId IS NULL
UNION ALL
SELECT 'product_portions.productId missing products', COUNT(*)
FROM `product_portions` pp LEFT JOIN `products` p ON p.productId = pp.productId WHERE p.productId IS NULL
UNION ALL
SELECT 'inventory.productId missing products', COUNT(*)
FROM `inventory` inv LEFT JOIN `products` p ON p.productId = inv.productId WHERE p.productId IS NULL
UNION ALL
SELECT 'combo_items.comboId missing combos', COUNT(*)
FROM `combo_items` ci LEFT JOIN `combos` co ON co.comboId = ci.comboId WHERE co.comboId IS NULL;

DROP PROCEDURE IF EXISTS `posbill_build_map`;
DROP PROCEDURE IF EXISTS `posbill_offset_pk`;
DROP PROCEDURE IF EXISTS `posbill_apply_pk`;
DROP PROCEDURE IF EXISTS `posbill_reset_ai`;
DROP PROCEDURE IF EXISTS `posbill_remap_fk`;
DROP PROCEDURE IF EXISTS `posbill_remap_fk_str`;
DROP PROCEDURE IF EXISTS `posbill_remap_mixed_user_or_license`;
DROP PROCEDURE IF EXISTS `posbill_loop_ai`;
DROP PROCEDURE IF EXISTS `posbill_remap_fk_plan`;
DROP PROCEDURE IF EXISTS `posbill_remap_all_fks`;
DROP TABLE IF EXISTS `posbill_id_map`;
DROP TABLE IF EXISTS `posbill_ai_tables`;
DROP TABLE IF EXISTS `posbill_fk_plan`;
