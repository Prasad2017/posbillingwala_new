-- p31: company business template — POS upload target for insertBusinessTemplate.php
-- Safe / additive. One row per licence (userId). Syncs businessType / templateId / optional JSON.

CREATE TABLE IF NOT EXISTS `company_business_templates` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `userId` VARCHAR(64) NOT NULL,
  `organization_id` INT NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `businessType` VARCHAR(64) NOT NULL DEFAULT 'restaurant',
  `businessTemplateId` VARCHAR(128) NOT NULL DEFAULT 'restaurant_default',
  `businessTemplateJson` MEDIUMTEXT NULL,
  `templateNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_company_biz_template_user` (`userId`),
  KEY `idx_company_biz_template_branch` (`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
