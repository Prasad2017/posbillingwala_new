-- =============================================================================
-- Clear shop data that does NOT belong to users 1, 6, 63, 70, 77
-- File: API/migrations/clear_shop_data_keep_1_6_63_70_77.sql
--
-- Deletes for everyone else:
--   invoices + bill lines, categories, subcategories, portions, products,
--   combos, shop details (companys), printer settings, extra licences
--
-- Does NOT delete the user rows 1 / 6 / 63 / 70 / 77.
-- Does NOT delete licences owned by those users.
--
-- Backup first. Cannot be undone.
-- =============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;

-- Preview
SELECT 'keep licences' AS info, id, userId, userName FROM `licenses`
WHERE `userId` IN (1, 6, 63, 70, 77) ORDER BY userId, id;

SELECT 'licences to delete' AS info, COUNT(*) AS cnt FROM `licenses`
WHERE `userId` NOT IN (1, 6, 63, 70, 77);

SELECT 'invoices to delete' AS info, COUNT(*) AS cnt FROM `invoice`
WHERE `licenseId` NOT IN (SELECT `id` FROM `licenses` WHERE `userId` IN (1, 6, 63, 70, 77));

-- ---------------------------------------------------------------------------
-- Invoices
-- ---------------------------------------------------------------------------

DELETE ici FROM `invoice_combo_items` ici
INNER JOIN `invoice` i
  ON CONVERT(i.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
   = CONVERT(ici.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
WHERE i.`licenseId` NOT IN (
  SELECT `id` FROM (SELECT `id` FROM `licenses` WHERE `userId` IN (1, 6, 63, 70, 77)) k
);

DELETE ici FROM `invoice_combo_items` ici
LEFT JOIN `invoice` i
  ON CONVERT(i.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
   = CONVERT(ici.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
WHERE i.`invoiceId` IS NULL;

DELETE ifp FROM `invoice_final_product` ifp
INNER JOIN `invoice` i
  ON CONVERT(i.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
   = CONVERT(ifp.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
WHERE i.`licenseId` NOT IN (
  SELECT `id` FROM (SELECT `id` FROM `licenses` WHERE `userId` IN (1, 6, 63, 70, 77)) k
);

DELETE ifp FROM `invoice_final_product` ifp
LEFT JOIN `invoice` i
  ON CONVERT(i.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
   = CONVERT(ifp.`invoiceNumber` USING utf8mb4) COLLATE utf8mb4_unicode_ci
WHERE i.`invoiceId` IS NULL;

DELETE FROM `invoice`
WHERE `licenseId` NOT IN (
  SELECT `id` FROM (SELECT `id` FROM `licenses` WHERE `userId` IN (1, 6, 63, 70, 77)) k
);

DELETE FROM `invoice`
WHERE `organization_id` IS NOT NULL
  AND `organization_id` NOT IN (1, 6, 63, 70, 77);

-- ---------------------------------------------------------------------------
-- Catalog: combos, portions, products, subcategories, categories
-- ---------------------------------------------------------------------------

DELETE FROM `combo_items`
WHERE `userId` IS NOT NULL AND `userId` NOT IN (1, 6, 63, 70, 77);

DELETE FROM `combos`
WHERE `userId` IS NOT NULL AND `userId` NOT IN (1, 6, 63, 70, 77);

DELETE FROM `product_portions`
WHERE `userId` IS NOT NULL AND `userId` NOT IN (1, 6, 63, 70, 77);

DELETE FROM `portion_master`
WHERE `userId` IS NOT NULL AND `userId` NOT IN (1, 6, 63, 70, 77);

DELETE FROM `products`
WHERE `userId` NOT IN (1, 6, 63, 70, 77);

DELETE FROM `product_subcategories`
WHERE `userId` IS NOT NULL AND `userId` NOT IN (1, 6, 63, 70, 77);

DELETE FROM `categories`
WHERE `userId` NOT IN (1, 6, 63, 70, 77);

-- ---------------------------------------------------------------------------
-- Shop details + printer details
-- ---------------------------------------------------------------------------

DELETE FROM `companys`
WHERE `licenseId` NOT IN (
  SELECT `id` FROM (SELECT `id` FROM `licenses` WHERE `userId` IN (1, 6, 63, 70, 77)) k
);

DELETE FROM `company_printer_setting`
WHERE `licenseId` NOT IN (
  SELECT `id` FROM (SELECT `id` FROM `licenses` WHERE `userId` IN (1, 6, 63, 70, 77)) k
);

-- ---------------------------------------------------------------------------
-- Licences (keep only licences of users 1, 6, 63, 70, 77)
-- ---------------------------------------------------------------------------

DELETE FROM `licenses`
WHERE `userId` NOT IN (1, 6, 63, 70, 77);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'remaining invoices' AS info, COUNT(*) AS cnt FROM `invoice`;
SELECT 'remaining categories' AS info, COUNT(*) AS cnt FROM `categories`;
SELECT 'remaining subcategories' AS info, COUNT(*) AS cnt FROM `product_subcategories`;
SELECT 'remaining portions' AS info, COUNT(*) AS cnt FROM `product_portions`;
SELECT 'remaining portion_master' AS info, COUNT(*) AS cnt FROM `portion_master`;
SELECT 'remaining products' AS info, COUNT(*) AS cnt FROM `products`;
SELECT 'remaining shops' AS info, COUNT(*) AS cnt FROM `companys`;
SELECT 'remaining printer settings' AS info, COUNT(*) AS cnt FROM `company_printer_setting`;
SELECT 'remaining licences' AS info, id, userId, userName FROM `licenses` ORDER BY userId, id;
