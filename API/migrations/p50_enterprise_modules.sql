-- P50 — Enterprise multi-business modules
-- Vendors, Purchase docs, CRM customers, Hotel rooms/bookings,
-- Product variants (size/color), businessType on licences.
-- SAFE to re-run (IF NOT EXISTS / conditional column add).

SET NAMES utf8mb4;

-- licences.businessType
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'licenses'
       AND COLUMN_NAME = 'businessType') > 0,
    'SELECT ''OK: licenses.businessType already exists'' AS msg',
    'ALTER TABLE `licenses` ADD COLUMN `businessType` VARCHAR(64) NOT NULL DEFAULT '''' AFTER `mess`'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `pos_vendors` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `name` VARCHAR(191) NOT NULL,
  `gstin` VARCHAR(32) NOT NULL DEFAULT '',
  `contactName` VARCHAR(120) NOT NULL DEFAULT '',
  `mobile` VARCHAR(32) NOT NULL DEFAULT '',
  `email` VARCHAR(120) NOT NULL DEFAULT '',
  `address` TEXT NULL,
  `paymentTerms` VARCHAR(64) NOT NULL DEFAULT 'Net 30',
  `creditLimit` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `openingBalance` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `bankDetails` TEXT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vendor_client` (`licenseId`, `clientId`),
  KEY `idx_vendor_org` (`organization_id`),
  KEY `idx_vendor_name` (`licenseId`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_purchase_documents` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `docNo` VARCHAR(64) NOT NULL,
  `docType` VARCHAR(16) NOT NULL,
  `docStatus` VARCHAR(16) NOT NULL DEFAULT 'draft',
  `vendorClientId` VARCHAR(64) NOT NULL DEFAULT '',
  `vendorName` VARCHAR(191) NOT NULL DEFAULT '',
  `notes` TEXT NULL,
  `referenceNo` VARCHAR(64) NOT NULL DEFAULT '',
  `parentClientId` VARCHAR(64) NOT NULL DEFAULT '',
  `createdBy` VARCHAR(120) NOT NULL DEFAULT '',
  `linesJson` LONGTEXT NOT NULL,
  `subTotal` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `receivedAt` DATETIME NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_client` (`licenseId`, `clientId`),
  KEY `idx_purchase_type` (`licenseId`, `docType`, `updatedAt`),
  KEY `idx_purchase_vendor` (`licenseId`, `vendorClientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_customers` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `name` VARCHAR(191) NOT NULL,
  `mobile` VARCHAR(32) NOT NULL DEFAULT '',
  `email` VARCHAR(120) NOT NULL DEFAULT '',
  `address` TEXT NULL,
  `gstin` VARCHAR(32) NOT NULL DEFAULT '',
  `creditLimit` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `walletBalance` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `loyaltyPoints` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `membership` VARCHAR(64) NOT NULL DEFAULT '',
  `birthday` DATE NULL,
  `anniversary` DATE NULL,
  `totalPurchase` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `lastPurchaseAt` DATETIME NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `notes` TEXT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_client` (`licenseId`, `clientId`),
  KEY `idx_customer_mobile` (`licenseId`, `mobile`),
  KEY `idx_customer_name` (`licenseId`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_hotel_room_types` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `baseRate` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `weekendRate` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `capacity` INT NOT NULL DEFAULT 2,
  `amenities` TEXT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_type_client` (`licenseId`, `clientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_hotel_rooms` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `roomNumber` VARCHAR(32) NOT NULL,
  `floor` VARCHAR(32) NOT NULL DEFAULT '',
  `wing` VARCHAR(64) NOT NULL DEFAULT '',
  `roomTypeClientId` VARCHAR(64) NOT NULL DEFAULT '',
  `roomTypeName` VARCHAR(120) NOT NULL DEFAULT '',
  `roomStatus` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  `notes` TEXT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_client` (`licenseId`, `clientId`),
  UNIQUE KEY `uk_room_number` (`licenseId`, `roomNumber`),
  KEY `idx_room_status` (`licenseId`, `roomStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_hotel_bookings` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `bookingNo` VARCHAR(64) NOT NULL,
  `roomClientId` VARCHAR(64) NOT NULL DEFAULT '',
  `roomNumber` VARCHAR(32) NOT NULL DEFAULT '',
  `guestName` VARCHAR(191) NOT NULL,
  `guestMobile` VARCHAR(32) NOT NULL DEFAULT '',
  `guestIdProof` VARCHAR(120) NOT NULL DEFAULT '',
  `checkInAt` DATETIME NULL,
  `checkOutAt` DATETIME NULL,
  `expectedCheckIn` DATE NULL,
  `expectedCheckOut` DATE NULL,
  `bookingStatus` VARCHAR(32) NOT NULL DEFAULT 'RESERVED',
  `advancePaid` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `roomCharges` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `extraCharges` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `notes` TEXT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_client` (`licenseId`, `clientId`),
  KEY `idx_booking_status` (`licenseId`, `bookingStatus`),
  KEY `idx_booking_room` (`licenseId`, `roomClientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pos_product_variants` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `productId` INT NOT NULL DEFAULT 0,
  `productName` VARCHAR(191) NOT NULL DEFAULT '',
  `brand` VARCHAR(120) NOT NULL DEFAULT '',
  `sizeLabel` VARCHAR(64) NOT NULL DEFAULT '',
  `colorLabel` VARCHAR(64) NOT NULL DEFAULT '',
  `sku` VARCHAR(64) NOT NULL DEFAULT '',
  `barcode` VARCHAR(64) NOT NULL DEFAULT '',
  `fabric` VARCHAR(120) NOT NULL DEFAULT '',
  `stockQty` DECIMAL(16,3) NOT NULL DEFAULT 0,
  `sellingPrice` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `mrp` DECIMAL(16,2) NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_variant_client` (`licenseId`, `clientId`),
  KEY `idx_variant_product` (`licenseId`, `productId`),
  KEY `idx_variant_barcode` (`licenseId`, `barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'P50 enterprise modules applied' AS msg;
