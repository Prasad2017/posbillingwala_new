-- P3-1: Food Type foundation (MySQL) — additive only, no DROP
-- Apply on server when ready for P3-6 sync.

CREATE TABLE IF NOT EXISTS `food_types` (
  `foodTypeId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `foodTypeName` varchar(64) NOT NULL,
  `foodTypeCode` varchar(32) NOT NULL,
  `foodTypeSortOrder` int(11) NOT NULL DEFAULT 0,
  `foodTypeStatus` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`foodTypeId`),
  UNIQUE KEY `idx_food_type_code` (`foodTypeCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `food_types` (`foodTypeName`, `foodTypeCode`, `foodTypeSortOrder`, `foodTypeStatus`, `created_at`)
SELECT 'Food', 'food', 1, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `food_types` WHERE `foodTypeCode` = 'food');

INSERT INTO `food_types` (`foodTypeName`, `foodTypeCode`, `foodTypeSortOrder`, `foodTypeStatus`, `created_at`)
SELECT 'Beverage', 'beverage', 2, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `food_types` WHERE `foodTypeCode` = 'beverage');

-- Add nullable FK on categories (skip if column already exists):
-- SHOW COLUMNS FROM `categories` LIKE 'foodTypeId';
ALTER TABLE `categories`
  ADD COLUMN `foodTypeId` int(10) UNSIGNED DEFAULT NULL AFTER `categoryName`;

UPDATE `categories` c
INNER JOIN `food_types` ft ON ft.foodTypeCode = 'food'
SET c.foodTypeId = ft.foodTypeId
WHERE c.foodTypeId IS NULL;
