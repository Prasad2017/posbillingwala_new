-- P19: Category / subcategory display order for POS billing screen
-- Safe to run more than once (skips columns already present).

SET NAMES utf8mb4;

-- categories.categorySortOrder
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND COLUMN_NAME = 'categorySortOrder'
);
SET @sql := IF(
  @col_exists > 0,
  'SELECT ''OK: categories.categorySortOrder already exists'' AS msg',
  'ALTER TABLE `categories` ADD COLUMN `categorySortOrder` int(11) NOT NULL DEFAULT 0 AFTER `categoryName`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill existing categories (stable by categoryId)
UPDATE `categories`
SET `categorySortOrder` = `categoryId`
WHERE IFNULL(`categorySortOrder`, 0) = 0;

-- product_subcategories.subcategorySortOrder
SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'product_subcategories'
    AND COLUMN_NAME = 'subcategorySortOrder'
);
SET @sql := IF(
  @col_exists > 0,
  'SELECT ''OK: product_subcategories.subcategorySortOrder already exists'' AS msg',
  'ALTER TABLE `product_subcategories` ADD COLUMN `subcategorySortOrder` int(11) NOT NULL DEFAULT 0 AFTER `subcategoryName`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `product_subcategories`
SET `subcategorySortOrder` = `subcategoryId`
WHERE IFNULL(`subcategorySortOrder`, 0) = 0;
