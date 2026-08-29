-- P16: Catalog Excel import/export sessions and history
-- Run on production DB after backup.

CREATE TABLE IF NOT EXISTS `catalog_import_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sessionId` VARCHAR(64) NOT NULL,
  `actorType` ENUM('admin','dealer','owner') NOT NULL,
  `actorId` INT UNSIGNED NOT NULL,
  `customerId` INT UNSIGNED NOT NULL,
  `importType` ENUM('products','categories','subcategories','portions') NOT NULL DEFAULT 'products',
  `fileName` VARCHAR(255) DEFAULT NULL,
  `storedFilePath` VARCHAR(512) DEFAULT NULL,
  `totalRows` INT UNSIGNED NOT NULL DEFAULT 0,
  `validRows` INT UNSIGNED NOT NULL DEFAULT 0,
  `newRows` INT UNSIGNED NOT NULL DEFAULT 0,
  `updateRows` INT UNSIGNED NOT NULL DEFAULT 0,
  `errorRows` INT UNSIGNED NOT NULL DEFAULT 0,
  `createdCount` INT UNSIGNED NOT NULL DEFAULT 0,
  `updatedCount` INT UNSIGNED NOT NULL DEFAULT 0,
  `failedCount` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` ENUM('pending','validated','imported','failed','expired') NOT NULL DEFAULT 'pending',
  `previewJson` MEDIUMTEXT DEFAULT NULL,
  `errorsJson` MEDIUMTEXT DEFAULT NULL,
  `expiresAt` DATETIME NOT NULL,
  `confirmedAt` DATETIME DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_session_id` (`sessionId`),
  KEY `idx_catalog_session_customer` (`customerId`, `importType`),
  KEY `idx_catalog_session_actor` (`actorType`, `actorId`),
  KEY `idx_catalog_session_status` (`status`, `expiresAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `catalog_export_history` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actorType` ENUM('admin','dealer','owner') NOT NULL,
  `actorId` INT UNSIGNED NOT NULL,
  `customerId` INT UNSIGNED NOT NULL,
  `exportType` ENUM('products','categories','subcategories','portions') NOT NULL,
  `fileName` VARCHAR(255) DEFAULT NULL,
  `rowCount` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` ENUM('completed','failed') NOT NULL DEFAULT 'completed',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_catalog_export_customer` (`customerId`, `exportType`),
  KEY `idx_catalog_export_actor` (`actorType`, `actorId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
