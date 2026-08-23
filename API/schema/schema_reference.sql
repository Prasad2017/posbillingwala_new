-- P5-5: Schema-only reference (no customer/licence data).
-- Generated from production dump structure; use API/migrations/ for additive upgrades.
-- Do NOT commit full database dumps to the repo.

SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET NAMES utf8mb4;
CREATE TABLE `categories` (
  `categoryId` int(10) UNSIGNED NOT NULL,
  `userId` int(11) NOT NULL,
  `dealerId` int(10) DEFAULT NULL,
  `categoryName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `categoryNetworkStatus` text DEFAULT NULL,
  `categoryStatus` text NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `companys` (
  `companyId` int(11) NOT NULL,
  `licenseId` int(11) NOT NULL,
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
  `companyStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `company_printer_setting` (
  `settingId` int(11) NOT NULL,
  `licenseId` int(11) NOT NULL,
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
  `settingStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `expenses` (
  `expensesId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `expensesName` text NOT NULL,
  `expensesAmount` text NOT NULL,
  `expensesDate` text NOT NULL,
  `expensesNetworkStatus` text NOT NULL,
  `expensesStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `failed_jobs` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `connection` text NOT NULL,
  `queue` text NOT NULL,
  `payload` longtext NOT NULL,
  `exception` longtext NOT NULL,
  `failed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `inventory` (
  `inventoryId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `productId` int(11) NOT NULL,
  `productInventoryQuantity` int(11) NOT NULL,
  `afterSaleInventoryQuantity` int(11) NOT NULL,
  `saleInventoryQuantity` int(11) NOT NULL,
  `inventoryDate` text NOT NULL,
  `inventoryNetworkStatus` text NOT NULL,
  `inventoryStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `invoice` (
  `invoiceId` int(11) NOT NULL,
  `licenseId` int(11) NOT NULL,
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
  `invoiceNetworkStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `invoice_final_product` (
  `invoiceProductId` int(11) NOT NULL,
  `invoiceNumber` text NOT NULL,
  `productName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `productPrice` float(16,2) NOT NULL,
  `productUnit` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `productCGST` float(16,2) NOT NULL,
  `productSGST` float(16,2) NOT NULL,
  `productQuantity` float(16,2) NOT NULL,
  `productStatus` text NOT NULL,
  `invoiceProductNetworkStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `licenses` (
  `id` int(10) UNSIGNED NOT NULL,
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
  `updated_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mess_invoice` (
  `invoiceId` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `memberName` text NOT NULL,
  `messType` text NOT NULL,
  `messInvoiceDate` text NOT NULL,
  `messInvoiceNetworkStatus` text NOT NULL,
  `messInvoiceStatus` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `mess_member` (
  `id` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `member_name` text NOT NULL,
  `member_mobile_number` text NOT NULL,
  `member_altenet_mobile_number` text NOT NULL,
  `member_address` longtext NOT NULL,
  `member_status` text NOT NULL,
  `member_network_status` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `mess_member_payment` (
  `payment_id` int(11) NOT NULL,
  `userId` int(11) NOT NULL,
  `memberId` int(11) NOT NULL,
  `memberName` text NOT NULL,
  `paymentMessAmount` float(16,2) NOT NULL,
  `paymentPaidAmount` float(16,2) NOT NULL,
  `messTotalDays` text NOT NULL,
  `paymentDate` text NOT NULL,
  `paymentNetworkStatus` text NOT NULL,
  `paymentStatus` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

CREATE TABLE `migrations` (
  `id` int(10) UNSIGNED NOT NULL,
  `migration` varchar(255) NOT NULL,
  `batch` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `password_resets` (
  `email` varchar(255) NOT NULL,
  `token` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `personal_access_tokens` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `tokenable_type` varchar(255) NOT NULL,
  `tokenable_id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `token` varchar(64) NOT NULL,
  `abilities` text DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `products` (
  `productId` int(10) UNSIGNED NOT NULL,
  `userId` int(11) NOT NULL,
  `dealerId` int(10) DEFAULT NULL,
  `categoryId` int(11) NOT NULL,
  `productCode` text NOT NULL,
  `productName` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `productUnit` text CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `productPrice` float(16,2) NOT NULL,
  `productCGST` int(11) DEFAULT NULL,
  `productSGST` int(11) DEFAULT NULL,
  `productStatus` text NOT NULL DEFAULT 'active',
  `productNetworkStatus` text NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `units` (
  `id` int(10) UNSIGNED NOT NULL,
  `name` text DEFAULT NULL,
  `is_active` int(11) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `id` int(10) UNSIGNED NOT NULL,
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
  `dealerId` int(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
