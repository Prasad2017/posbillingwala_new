-- p28: custom order deposits (bakery) — POS upload target for insertCustomOrderDeposit.php
-- Safe / additive. Run on server before relying on cloud sync for deposits.
-- Photo binaries stay on device; only photoFile name is synced.

CREATE TABLE IF NOT EXISTS `custom_order_deposits` (
  `depositId` INT NOT NULL AUTO_INCREMENT,
  `userId` VARCHAR(64) NOT NULL,
  `organization_id` INT NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `local_deposit_id` VARCHAR(64) NOT NULL,
  `productName` VARCHAR(255) NULL DEFAULT NULL,
  `orderNote` TEXT NULL,
  `depositAmount` VARCHAR(64) NULL DEFAULT NULL,
  `dueDate` VARCHAR(64) NULL DEFAULT NULL,
  `photoFile` VARCHAR(512) NULL DEFAULT NULL,
  `depositStatus` VARCHAR(32) NOT NULL DEFAULT 'open',
  `depositNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
  `createdAtLocal` VARCHAR(64) NULL DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`depositId`),
  UNIQUE KEY `uq_custom_deposit_local` (`userId`, `local_deposit_id`),
  KEY `idx_custom_deposit_branch` (`branch_id`),
  KEY `idx_custom_deposit_status` (`depositStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
