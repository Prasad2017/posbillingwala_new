-- P34 — Inventory purchase/waste metadata + product priceIncludesGst

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'inventory'
       AND COLUMN_NAME = 'movementType') > 0,
    'SELECT ''OK: inventory.movementType already exists'' AS msg',
    'ALTER TABLE `inventory` ADD COLUMN `movementType` VARCHAR(20) NOT NULL DEFAULT ''purchase'' AFTER `saleInventoryQuantity`, ADD COLUMN `inventoryNote` VARCHAR(255) NOT NULL DEFAULT '''' AFTER `movementType`, ADD COLUMN `unitCost` DECIMAL(16,2) NOT NULL DEFAULT 0 AFTER `inventoryNote`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'products'
       AND COLUMN_NAME = 'priceIncludesGst') > 0,
    'SELECT ''OK: products.priceIncludesGst already exists'' AS msg',
    'ALTER TABLE `products` ADD COLUMN `priceIncludesGst` VARCHAR(10) NOT NULL DEFAULT ''0'' AFTER `openPrice`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
