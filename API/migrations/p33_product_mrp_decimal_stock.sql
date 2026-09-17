-- P33 — Product MRP + decimal inventory quantities

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'products'
       AND COLUMN_NAME = 'productMrp') > 0,
    'SELECT ''OK: products.productMrp already exists'' AS msg',
    'ALTER TABLE `products` ADD COLUMN `productMrp` DECIMAL(16,2) NOT NULL DEFAULT 0 AFTER `productPrice`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Inventory qty columns → decimal (KG / Gram stock)
SET @sql = (
  SELECT IF(
    (SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'inventory'
       AND COLUMN_NAME = 'productInventoryQuantity') = 'decimal',
    'SELECT ''OK: inventory.productInventoryQuantity already decimal'' AS msg',
    'ALTER TABLE `inventory` MODIFY COLUMN `productInventoryQuantity` DECIMAL(16,3) NOT NULL DEFAULT 0, MODIFY COLUMN `afterSaleInventoryQuantity` DECIMAL(16,3) NOT NULL DEFAULT 0, MODIFY COLUMN `saleInventoryQuantity` DECIMAL(16,3) NOT NULL DEFAULT 0'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
