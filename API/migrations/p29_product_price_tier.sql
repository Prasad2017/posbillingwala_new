-- p29: product price tiers (wholesale) — POS upload target for insertProductPriceTier.php
-- Safe / additive. Run on server before relying on cloud sync for wholesale tiers.

CREATE TABLE IF NOT EXISTS `product_price_tiers` (
  `tierId` INT NOT NULL AUTO_INCREMENT,
  `userId` VARCHAR(64) NOT NULL,
  `organization_id` INT NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `local_tier_id` VARCHAR(64) NOT NULL,
  `productId` VARCHAR(64) NULL DEFAULT NULL,
  `productNetworkStatus` VARCHAR(128) NULL DEFAULT NULL,
  `minQty` VARCHAR(64) NOT NULL DEFAULT '1',
  `tierPrice` VARCHAR(64) NOT NULL,
  `tierLabel` VARCHAR(255) NULL DEFAULT NULL,
  `tierDeletedStatus` VARCHAR(8) NOT NULL DEFAULT '0',
  `tierNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tierId`),
  UNIQUE KEY `uq_price_tier_local` (`userId`, `local_tier_id`),
  KEY `idx_price_tier_branch` (`branch_id`),
  KEY `idx_price_tier_product` (`productNetworkStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
