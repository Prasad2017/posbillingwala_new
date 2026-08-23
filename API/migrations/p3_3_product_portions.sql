-- P3-3: Product portions (MySQL) — additive only, no DROP
-- 0..N portions per product; products with no rows keep using products.productPrice

CREATE TABLE IF NOT EXISTS `product_portions` (
  `portionId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `productId` int(10) UNSIGNED NOT NULL,
  `portionName` varchar(64) NOT NULL,
  `portionPrice` decimal(16,2) NOT NULL,
  `portionSortOrder` int(11) NOT NULL DEFAULT 0,
  `portionNetworkStatus` varchar(64) DEFAULT NULL,
  `portionStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`portionId`),
  UNIQUE KEY `idx_portion_network_status` (`portionNetworkStatus`),
  KEY `idx_portion_product` (`productId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
