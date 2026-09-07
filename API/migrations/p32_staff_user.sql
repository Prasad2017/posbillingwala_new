-- p32: staff users (device roster) — POS upload target for insertStaffUser.php
-- Safe / additive. PINs sync for multi-device staff switch (same as local storage).

CREATE TABLE IF NOT EXISTS `staff_users` (
  `staffId` INT NOT NULL AUTO_INCREMENT,
  `userId` VARCHAR(64) NOT NULL,
  `organization_id` INT NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `local_staff_id` VARCHAR(64) NOT NULL,
  `staffName` VARCHAR(255) NOT NULL,
  `staffRole` VARCHAR(64) NOT NULL DEFAULT 'cashier',
  `staffPin` VARCHAR(64) NULL DEFAULT NULL,
  `staffActive` VARCHAR(8) NOT NULL DEFAULT '1',
  `staffDeletedStatus` VARCHAR(8) NOT NULL DEFAULT '0',
  `staffNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
  `createdAtLocal` VARCHAR(64) NULL DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`staffId`),
  UNIQUE KEY `uq_staff_user_local` (`userId`, `local_staff_id`),
  KEY `idx_staff_user_branch` (`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
