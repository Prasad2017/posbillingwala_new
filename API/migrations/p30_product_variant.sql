-- p30: product variants (fashion/jewellery) — POS upload target for insertProductVariant.php
-- Safe / additive. Run on server before relying on cloud sync for variants.

CREATE TABLE IF NOT EXISTS `product_variants` (
  `variantId` INT NOT NULL AUTO_INCREMENT,
  `userId` VARCHAR(64) NOT NULL,
  `organization_id` INT NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `local_variant_id` VARCHAR(64) NOT NULL,
  `productId` VARCHAR(64) NULL DEFAULT NULL,
  `productNetworkStatus` VARCHAR(128) NULL DEFAULT NULL,
  `variantSize` VARCHAR(128) NULL DEFAULT NULL,
  `variantColor` VARCHAR(128) NULL DEFAULT NULL,
  `variantSku` VARCHAR(128) NULL DEFAULT NULL,
  `variantPrice` VARCHAR(64) NULL DEFAULT NULL,
  `variantDeletedStatus` VARCHAR(8) NOT NULL DEFAULT '0',
  `variantSortOrder` INT NOT NULL DEFAULT 0,
  `variantNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`variantId`),
  UNIQUE KEY `uq_product_variant_local` (`userId`, `local_variant_id`),
  KEY `idx_product_variant_branch` (`branch_id`),
  KEY `idx_product_variant_product` (`productNetworkStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
