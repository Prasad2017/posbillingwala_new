-- p27: service appointments (salon family) — POS upload target for insertServiceAppointment.php
-- Safe / additive. Run on server before relying on cloud sync for appointments.

CREATE TABLE IF NOT EXISTS `service_appointments` (
  `appointmentId` INT NOT NULL AUTO_INCREMENT,
  `userId` VARCHAR(64) NOT NULL,
  `organization_id` INT NULL DEFAULT NULL,
  `branch_id` INT NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `local_appointment_id` VARCHAR(64) NOT NULL,
  `productId` VARCHAR(64) NULL DEFAULT NULL,
  `productName` VARCHAR(255) NULL DEFAULT NULL,
  `customerName` VARCHAR(255) NULL DEFAULT NULL,
  `customerMobile` VARCHAR(64) NULL DEFAULT NULL,
  `appointmentAt` VARCHAR(64) NULL DEFAULT NULL,
  `notes` TEXT NULL,
  `appointmentStatus` VARCHAR(32) NOT NULL DEFAULT 'booked',
  `staffId` VARCHAR(64) NULL DEFAULT NULL,
  `staffName` VARCHAR(255) NULL DEFAULT NULL,
  `appointmentNetworkStatus` VARCHAR(64) NULL DEFAULT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`appointmentId`),
  UNIQUE KEY `uq_service_appt_local` (`userId`, `local_appointment_id`),
  KEY `idx_service_appt_branch` (`branch_id`),
  KEY `idx_service_appt_at` (`appointmentAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
