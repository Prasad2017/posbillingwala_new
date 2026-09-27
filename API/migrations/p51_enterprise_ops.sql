-- p51_enterprise_ops.sql
-- Warehouses, transfers, brands, offers, approvals, stock lots, returns, serials

CREATE TABLE IF NOT EXISTS `pos_ops_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `organization_id` INT NOT NULL DEFAULT 0,
  `licenseId` INT UNSIGNED NOT NULL,
  `entityType` VARCHAR(32) NOT NULL,
  `clientId` VARCHAR(64) NOT NULL,
  `title` VARCHAR(191) NOT NULL DEFAULT '',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `payloadJson` LONGTEXT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ops_client` (`licenseId`, `entityType`, `clientId`),
  KEY `idx_ops_license_entity` (`licenseId`, `entityType`, `updatedAt`),
  KEY `idx_ops_status` (`licenseId`, `entityType`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
