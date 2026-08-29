-- =============================================================================
-- KEEP ONLY users 1 (admin), 6, 63, 70, 77 — delete everyone else and their data
-- File: API/migrations/purge_keep_users_6_63_70_77.sql
--
-- KEPT:
--   users.id IN (1, 6, 63, 70, 77)
--   licenses belonging to those users
--   shop / invoice / catalog / mess / tokens / logs for those users only
--
-- NOT DELETED (system / global, not customer accounts):
--   food_types, units, migrations, website_pages, website_clients,
--   website_testimonials, website_contact_messages
--
-- BEFORE YOU RUN:
--   1. Full database backup
--   2. phpMyAdmin → select database → SQL tab
--   3. Run PREVIEW first. Only run PURGE when the counts look correct.
--
-- This cannot be undone.
-- =============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ---------------------------------------------------------------------------
-- PREVIEW (safe — run this first)
-- ---------------------------------------------------------------------------

SELECT id, role_id, name, shopName, dealerId, contact_number, email
FROM `users`
WHERE id IN (1, 6, 63, 70, 77)
ORDER BY id;

SELECT 'users that WILL BE DELETED' AS info, COUNT(*) AS cnt
FROM `users`
WHERE id NOT IN (1, 6, 63, 70, 77);

SELECT 'licenses that WILL BE DELETED' AS info, COUNT(*) AS cnt
FROM `licenses`
WHERE userId NOT IN (1, 6, 63, 70, 77);

SELECT 'invoices that WILL BE DELETED' AS info, COUNT(*) AS cnt
FROM `invoice`
WHERE licenseId NOT IN (
  SELECT id FROM `licenses` WHERE userId IN (1, 6, 63, 70, 77)
);

SELECT 'products that WILL BE DELETED' AS info, COUNT(*) AS cnt
FROM `products`
WHERE userId NOT IN (1, 6, 63, 70, 77);

-- STOP HERE after preview. Run the PURGE block only when counts look correct.
-- ---------------------------------------------------------------------------
-- PURGE (destructive)
-- ---------------------------------------------------------------------------

START TRANSACTION;

SET FOREIGN_KEY_CHECKS = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_keep_users`;
DROP TEMPORARY TABLE IF EXISTS `tmp_keep_licenses`;
DROP TEMPORARY TABLE IF EXISTS `tmp_keep_any_id`;
DROP TEMPORARY TABLE IF EXISTS `tmp_drop_invoices`;

CREATE TEMPORARY TABLE `tmp_keep_users` (
  `id` INT NOT NULL PRIMARY KEY
);

INSERT IGNORE INTO `tmp_keep_users` (`id`) VALUES (1), (6), (63), (70), (77);

CREATE TEMPORARY TABLE `tmp_keep_licenses` (
  `id` INT NOT NULL PRIMARY KEY
);

INSERT IGNORE INTO `tmp_keep_licenses` (`id`)
SELECT `id` FROM `licenses`
WHERE `userId` IN (SELECT `id` FROM `tmp_keep_users`);

-- POS often stores licence id in userId on inventory / expenses / mess
CREATE TEMPORARY TABLE `tmp_keep_any_id` (
  `id` INT NOT NULL PRIMARY KEY
);

INSERT IGNORE INTO `tmp_keep_any_id` (`id`)
SELECT `id` FROM `tmp_keep_users`;

INSERT IGNORE INTO `tmp_keep_any_id` (`id`)
SELECT `id` FROM `tmp_keep_licenses`;

CREATE TEMPORARY TABLE `tmp_drop_invoices` (
  `invoiceNumber` VARCHAR(191) NOT NULL,
  `organization_id` INT NULL,
  `branch_id` INT NULL,
  KEY `idx_num` (`invoiceNumber`(64))
);

INSERT INTO `tmp_drop_invoices` (`invoiceNumber`, `organization_id`, `branch_id`)
SELECT `invoiceNumber`, `organization_id`, `branch_id`
FROM `invoice`
WHERE `licenseId` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

-- Bill line items
DELETE ici FROM `invoice_combo_items` ici
INNER JOIN `tmp_drop_invoices` d
  ON d.`invoiceNumber` = ici.`invoiceNumber`;

DELETE ici FROM `invoice_combo_items` ici
INNER JOIN `invoice_final_product` ifp
  ON ifp.`invoiceProductNetworkStatus` = ici.`invoiceProductNetworkStatus`
INNER JOIN `tmp_drop_invoices` d
  ON d.`invoiceNumber` = ifp.`invoiceNumber`;

DELETE ifp FROM `invoice_final_product` ifp
INNER JOIN `tmp_drop_invoices` d
  ON d.`invoiceNumber` = ifp.`invoiceNumber`
 AND (ifp.`organization_id` IS NULL OR d.`organization_id` IS NULL OR ifp.`organization_id` = d.`organization_id`);

DELETE FROM `invoice_final_product`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `invoice_final_product`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `invoice`
WHERE `licenseId` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `invoice`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `companys`
WHERE `licenseId` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `company_printer_setting`
WHERE `licenseId` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `branch_access_grants`
WHERE `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`)
   OR `source_branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`)
   OR `target_branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

-- Catalog / shop masters (userId = organisation / owner id)
DELETE FROM `combo_items`
WHERE `userId` IS NOT NULL
  AND `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `combos`
WHERE `userId` IS NOT NULL
  AND `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `product_portions`
WHERE `userId` IS NOT NULL
  AND `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `portion_master`
WHERE `userId` IS NOT NULL
  AND `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `products`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `product_subcategories`
WHERE `userId` IS NOT NULL
  AND `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `categories`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

-- POS sync tables: userId may be owner id OR licence id
DELETE FROM `inventory`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_any_id`);

DELETE FROM `inventory`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `inventory`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `expenses`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_any_id`);

DELETE FROM `expenses`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `expenses`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `mess_token`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_any_id`);

DELETE FROM `mess_token`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `mess_token`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `mess_member_payment`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_any_id`);

DELETE FROM `mess_member_payment`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `mess_member_payment`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `mess_invoice`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_any_id`);

DELETE FROM `mess_invoice`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `mess_invoice`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `mess_member`
WHERE `userId` NOT IN (SELECT `id` FROM `tmp_keep_any_id`);

DELETE FROM `mess_member`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `mess_member`
WHERE `branch_id` IS NOT NULL
  AND `branch_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

-- Catalog import/export history
DELETE FROM `catalog_import_sessions`
WHERE `customerId` NOT IN (SELECT `id` FROM `tmp_keep_users`)
   OR `actorId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `catalog_export_history`
WHERE `customerId` NOT IN (SELECT `id` FROM `tmp_keep_users`)
   OR `actorId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

-- Auth tokens
DELETE FROM `api_tokens`
WHERE (`actor_type` = 'pos_licence' AND `actor_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`))
   OR (`actor_type` IN ('owner', 'dealer', 'admin') AND `actor_id` NOT IN (SELECT `id` FROM `tmp_keep_users`));

DELETE FROM `personal_access_tokens`
WHERE `tokenable_id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `password_resets`;
DELETE FROM `failed_jobs`;

-- Support / crash / error logs for other shops
DELETE FROM `admin_support_messages`
WHERE `ticket_id` IN (
  SELECT `id` FROM `admin_support_tickets`
  WHERE (`user_id` IS NOT NULL AND `user_id` NOT IN (SELECT `id` FROM `tmp_keep_users`))
     OR (`licence_id` IS NOT NULL AND `licence_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`))
);

DELETE FROM `admin_support_tickets`
WHERE (`user_id` IS NOT NULL AND `user_id` NOT IN (SELECT `id` FROM `tmp_keep_users`))
   OR (`licence_id` IS NOT NULL AND `licence_id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`));

DELETE FROM `admin_crash_logs`
WHERE `user_id` IS NOT NULL
  AND TRIM(`user_id`) <> ''
  AND CAST(`user_id` AS UNSIGNED) NOT IN (SELECT `id` FROM `tmp_keep_users`);

DELETE FROM `error_logs`
WHERE `customer_id` IS NOT NULL
  AND TRIM(`customer_id`) <> ''
  AND CAST(`customer_id` AS UNSIGNED) NOT IN (SELECT `id` FROM `tmp_keep_users`);

-- Licences then users
DELETE FROM `licenses`
WHERE `id` NOT IN (SELECT `id` FROM `tmp_keep_licenses`);

DELETE FROM `users`
WHERE `id` NOT IN (SELECT `id` FROM `tmp_keep_users`);

-- Clear dealerId on kept shops if the dealer row was removed
UPDATE `users`
SET `dealerId` = NULL
WHERE `dealerId` IS NOT NULL
  AND `dealerId` NOT IN (SELECT `id` FROM `tmp_keep_users`);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kept users' AS info, id, role_id, name, shopName, dealerId, contact_number
FROM `users`
ORDER BY id;

SELECT 'kept licenses' AS info, id, userId, userName, licenseStatus, expiryDate
FROM `licenses`
ORDER BY userId, id;

SELECT 'remaining invoices' AS info, COUNT(*) AS cnt FROM `invoice`;
SELECT 'remaining products' AS info, COUNT(*) AS cnt FROM `products`;
SELECT 'remaining categories' AS info, COUNT(*) AS cnt FROM `categories`;

COMMIT;

DROP TEMPORARY TABLE IF EXISTS `tmp_drop_invoices`;
DROP TEMPORARY TABLE IF EXISTS `tmp_keep_any_id`;
DROP TEMPORARY TABLE IF EXISTS `tmp_keep_licenses`;
DROP TEMPORARY TABLE IF EXISTS `tmp_keep_users`;
