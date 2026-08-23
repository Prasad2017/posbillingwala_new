-- =============================================================================
-- POS Billingwala — single-file FRESH database install (Aug 2026)
-- File: API/schema/posbill_install.sql
--
-- Use this for a NEW empty database (no customer / licence / invoice data).
-- Includes all features: food types, subcategories, portions, bill snapshots,
-- API tokens, production licensing columns, multi-branch scope.
--
-- HOW TO RUN (phpMyAdmin):
--   1. Create empty database (e.g. spllmgkn_posbill)
--   2. Select database → Import → choose this file → Go
--
-- HOW TO RUN (CLI):
--   mysql -u USER -p -e "CREATE DATABASE IF NOT EXISTS spllmgkn_posbill CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--   mysql -u USER -p spllmgkn_posbill < API/schema/posbill_install.sql
--
-- RESTORE OLD PRODUCTION DATA:
--   Use spllmgkn_posbill_complete.sql (old dump + upgrade) instead of this file.
-- =============================================================================

SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ---------------------------------------------------------------------------
-- Core catalog
-- ---------------------------------------------------------------------------

CREATE TABLE `food_types` (
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

CREATE TABLE `categories` (
  `categoryId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `dealerId` int(10) DEFAULT NULL,
  `categoryName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `foodTypeId` int(10) UNSIGNED DEFAULT NULL,
  `categoryNetworkStatus` text DEFAULT NULL,
  `categoryStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`categoryId`),
  KEY `idx_category_user_food` (`userId`, `foodTypeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_subcategories` (
  `subcategoryId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `categoryId` int(10) UNSIGNED NOT NULL,
  `subcategoryName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `subcategoryNetworkStatus` text DEFAULT NULL,
  `subcategoryStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`subcategoryId`),
  KEY `idx_subcategory_category` (`categoryId`),
  KEY `idx_subcategory_user` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `products` (
  `productId` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `dealerId` int(10) DEFAULT NULL,
  `categoryId` int(11) NOT NULL,
  `subcategoryId` int(10) UNSIGNED DEFAULT NULL,
  `productCode` text NOT NULL,
  `productName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `productUnit` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `productPrice` float(16,2) NOT NULL,
  `productCGST` int(11) DEFAULT NULL,
  `productSGST` int(11) DEFAULT NULL,
  `productStatus` text NOT NULL DEFAULT 'active',
  `productNetworkStatus` text NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`productId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_portions` (
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
  KEY `idx_portion_product` (`productId`),
  KEY `idx_portion_user` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `units` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` text DEFAULT NULL,
  `is_active` int(11) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Users, licences, auth
-- ---------------------------------------------------------------------------

CREATE TABLE `users` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` int(11) NOT NULL DEFAULT 3,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `email_verified_at` timestamp NULL DEFAULT NULL,
  `contact_number` varchar(255) DEFAULT NULL,
  `aadhar_number` text DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `reportPin` int(10) NOT NULL DEFAULT 9082,
  `address` text DEFAULT NULL,
  `is_active` int(11) NOT NULL DEFAULT 1,
  `remember_token` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp(),
  `shopName` text DEFAULT NULL,
  `shopImage` text DEFAULT NULL,
  `dealerId` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `contact_number` (`contact_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `licenses` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `licenseKey` text NOT NULL,
  `licenseValidity` int(10) NOT NULL DEFAULT 0,
  `licenseType` text NOT NULL,
  `android_device_name` text DEFAULT NULL,
  `android_device_id` text DEFAULT NULL,
  `mpin` text NOT NULL,
  `licenseStatus` text NOT NULL,
  `expiryDate` date NOT NULL,
  `trialStartedAt` datetime DEFAULT NULL,
  `trialConsumed` tinyint(1) NOT NULL DEFAULT 0,
  `deviceBoundAt` datetime DEFAULT NULL,
  `paymentStatus` text NOT NULL,
  `amount` int(11) NOT NULL,
  `userType` text NOT NULL DEFAULT 'owner',
  `userName` text DEFAULT NULL,
  `fastBilling` int(10) NOT NULL DEFAULT 1,
  `takeAway` int(10) NOT NULL DEFAULT 1,
  `dineIn` int(10) NOT NULL DEFAULT 1,
  `mess` int(11) NOT NULL DEFAULT 0,
  `total_sale_data` int(11) NOT NULL DEFAULT 1,
  `today_sale_data` int(11) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `api_tokens` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `token_hash` char(64) NOT NULL,
  `actor_type` enum('pos_licence','owner','dealer','admin') NOT NULL,
  `actor_id` int(10) UNSIGNED NOT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `expires_at` datetime NOT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_actor` (`actor_type`, `actor_id`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `branch_access_grants` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) NOT NULL,
  `source_branch_id` int(10) UNSIGNED NOT NULL COMMENT 'licenses.id receiving access',
  `target_branch_id` int(10) UNSIGNED NOT NULL COMMENT 'licenses.id whose data may be read',
  `granted_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `granted_by` varchar(50) NOT NULL DEFAULT 'dealer',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_branch_grant` (`source_branch_id`, `target_branch_id`),
  KEY `idx_org` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Company / printer settings
-- ---------------------------------------------------------------------------

CREATE TABLE `companys` (
  `companyId` int(11) NOT NULL AUTO_INCREMENT,
  `licenseId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `companyLogo` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `paymentLogo` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `companyName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `cashierName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `companyMobile` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `companyAddress` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `currencyName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `tableStatus` text NOT NULL,
  `noOfTable` int(11) NOT NULL DEFAULT 0,
  `countryName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stateName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `gstStatus` text NOT NULL,
  `gstNumber` text NOT NULL,
  `shopCGST` float(5,2) NOT NULL,
  `shopSGST` float(5,2) NOT NULL,
  `panNumber` text NOT NULL,
  `companyFssis` text NOT NULL,
  `companyStatus` text NOT NULL,
  PRIMARY KEY (`companyId`),
  UNIQUE KEY `licenseId` (`licenseId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `company_printer_setting` (
  `settingId` int(11) NOT NULL AUTO_INCREMENT,
  `licenseId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `printerName` text NOT NULL,
  `KOTPrinterName` text NOT NULL DEFAULT '',
  `invoicePrefix` text NOT NULL,
  `invoiceTitle` text NOT NULL,
  `logoUse` longtext NOT NULL,
  `paymentUse` longtext NOT NULL,
  `customerUse` longtext CHARACTER SET utf16 COLLATE utf16_general_ci NOT NULL,
  `productQuantityUpdate` text NOT NULL,
  `bluetoothAddress` longtext NOT NULL,
  `bluetoothKOTAddress` longtext NOT NULL,
  `printerFeedLines` varchar(5) NOT NULL,
  `KotPrinterFeedLines` varchar(5) NOT NULL,
  `invoiceTermsCondition` longtext NOT NULL,
  `settingStatus` text NOT NULL,
  PRIMARY KEY (`settingId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- Billing
-- ---------------------------------------------------------------------------

CREATE TABLE `invoice` (
  `invoiceId` int(11) NOT NULL AUTO_INCREMENT,
  `licenseId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `noOfTable` text NOT NULL,
  `invoiceType` text NOT NULL,
  `invoiceNumber` text NOT NULL,
  `customerName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `customerMobile` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `customerEmail` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `customerAddress` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `subTotal` float(16,2) NOT NULL,
  `totalGSTAmount` float(16,2) NOT NULL,
  `discount` float(16,2) NOT NULL,
  `discountType` enum('Percentage','Amount') NOT NULL,
  `totalAmount` float(16,2) NOT NULL,
  `paymentMode` text NOT NULL,
  `invoiceDate` datetime NOT NULL,
  `invoiceOrderStatus` text NOT NULL,
  `invoiceNetworkStatus` text NOT NULL,
  PRIMARY KEY (`invoiceId`),
  KEY `idx_invoice_branch_date` (`branch_id`, `invoiceDate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `invoice_final_product` (
  `invoiceProductId` int(11) NOT NULL AUTO_INCREMENT,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `invoiceNumber` text NOT NULL,
  `productName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `portionId` int(10) UNSIGNED DEFAULT NULL,
  `portionName` varchar(64) DEFAULT NULL,
  `snapshotProductName` text DEFAULT NULL,
  `snapshotLinePrice` decimal(16,2) DEFAULT NULL,
  `productPrice` float(16,2) NOT NULL,
  `productUnit` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `productCGST` float(16,2) NOT NULL,
  `productSGST` float(16,2) NOT NULL,
  `productQuantity` float(16,2) NOT NULL,
  `productStatus` text NOT NULL,
  `invoiceProductNetworkStatus` text NOT NULL,
  PRIMARY KEY (`invoiceProductId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- Inventory, expenses, mess
-- ---------------------------------------------------------------------------

CREATE TABLE `inventory` (
  `inventoryId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `productId` int(11) NOT NULL,
  `productInventoryQuantity` int(11) NOT NULL,
  `afterSaleInventoryQuantity` int(11) NOT NULL,
  `saleInventoryQuantity` int(11) NOT NULL,
  `inventoryDate` text NOT NULL,
  `inventoryNetworkStatus` text NOT NULL,
  `inventoryStatus` text NOT NULL,
  PRIMARY KEY (`inventoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `expenses` (
  `expensesId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `expensesName` text NOT NULL,
  `expensesAmount` text NOT NULL,
  `expensesDate` text NOT NULL,
  `expensesNetworkStatus` text NOT NULL,
  `expensesStatus` text NOT NULL,
  PRIMARY KEY (`expensesId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `mess_member` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `member_name` text NOT NULL,
  `member_mobile_number` text NOT NULL,
  `member_altenet_mobile_number` text NOT NULL,
  `member_address` longtext NOT NULL,
  `member_status` text NOT NULL,
  `member_network_status` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `mess_member_payment` (
  `payment_id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `memberId` int(11) NOT NULL,
  `memberName` text NOT NULL,
  `paymentMessAmount` float(16,2) NOT NULL,
  `paymentPaidAmount` float(16,2) NOT NULL,
  `messTotalDays` text NOT NULL,
  `paymentDate` text NOT NULL,
  `paymentNetworkStatus` text NOT NULL,
  `paymentStatus` text NOT NULL,
  PRIMARY KEY (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `mess_invoice` (
  `invoiceId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `memberName` text NOT NULL,
  `messType` text NOT NULL,
  `messInvoiceDate` text NOT NULL,
  `messInvoiceNetworkStatus` text NOT NULL,
  `messInvoiceStatus` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`invoiceId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- Laravel / framework helpers
-- ---------------------------------------------------------------------------

CREATE TABLE `failed_jobs` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) NOT NULL,
  `connection` text NOT NULL,
  `queue` text NOT NULL,
  `payload` longtext NOT NULL,
  `exception` longtext NOT NULL,
  `failed_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `failed_jobs_uuid_unique` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `migrations` (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `migration` varchar(255) NOT NULL,
  `batch` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `password_resets` (
  `email` varchar(255) NOT NULL,
  `token` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  KEY `password_resets_email_index` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `personal_access_tokens` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `tokenable_type` varchar(255) NOT NULL,
  `tokenable_id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `token` varchar(64) NOT NULL,
  `abilities` text DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `personal_access_tokens_token_unique` (`token`),
  KEY `personal_access_tokens_tokenable_type_tokenable_id_index` (`tokenable_type`, `tokenable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Seed data (minimal — change admin password after first login)
-- ---------------------------------------------------------------------------

INSERT INTO `food_types` (`foodTypeName`, `foodTypeCode`, `foodTypeSortOrder`, `foodTypeStatus`, `created_at`) VALUES
('Food', 'food', 1, 1, NOW()),
('Beverage', 'beverage', 2, 1, NOW());

INSERT INTO `units` (`id`, `name`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'GRAM ', 1, NULL, NULL),
(2, 'KG', 1, NULL, NULL),
(3, 'MiliLitre', 1, NULL, NULL),
(4, 'Litre', 1, NULL, NULL),
(5, 'CM', 1, NULL, NULL),
(6, 'Meter', 1, NULL, NULL),
(7, 'Inch', 1, NULL, NULL),
(8, 'Feet', 1, NULL, NULL),
(9, 'Pic', 1, NULL, NULL),
(10, 'Plate', 1, NULL, NULL),
(11, 'Cup', 1, NULL, NULL);

-- Default admin (password: admin123 — CHANGE IMMEDIATELY on production)
INSERT INTO `users` (`id`, `role_id`, `name`, `email`, `password`, `reportPin`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 1, 'POS Billing Admin', 'admin@gmail.com', '$2y$10$8IPcX8Znhdt6fcvtdISLQufvWcF2Inn0.6.eKUL0VgIP07iSxAn6S', 9082, 1, NOW(), NOW());

SELECT 'POS Billingwala fresh install complete' AS status;
