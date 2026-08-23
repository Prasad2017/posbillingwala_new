-- P3-2: Optional product subcategory (MySQL) — additive only, no DROP

CREATE TABLE IF NOT EXISTS `product_subcategories` (
  `subcategoryId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `categoryId` int(10) UNSIGNED NOT NULL,
  `subcategoryName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `subcategoryNetworkStatus` text DEFAULT NULL,
  `subcategoryStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`subcategoryId`),
  KEY `idx_subcategory_category` (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nullable on products — existing rows keep working with NULL
-- SHOW COLUMNS FROM `products` LIKE 'subcategoryId';
ALTER TABLE `products`
  ADD COLUMN `subcategoryId` int(10) UNSIGNED DEFAULT NULL AFTER `categoryId`;
