-- Optional dedicated takeaway open-parcel store (web POS).
-- Web client falls back to dining_session (waiterName=TAKEAWAY) until
-- insertTakeawayOpen.php / getTakeawayOpenList.php are deployed to androidApp/.

CREATE TABLE IF NOT EXISTS `takeaway_open_parcels` (
    `parcelId` INT NOT NULL AUTO_INCREMENT,
    `licenseId` VARCHAR(64) NOT NULL,
    `organization_id` VARCHAR(64) NULL,
    `branch_id` VARCHAR(64) NULL,
    `device_id` VARCHAR(128) NULL,
    `parcelNetworkStatus` VARCHAR(128) NOT NULL,
    `customerName` VARCHAR(255) NOT NULL DEFAULT '',
    `customerMobile` VARCHAR(64) NOT NULL DEFAULT '',
    `parcelStatus` VARCHAR(32) NOT NULL DEFAULT 'open',
    `createdAt` DATETIME NOT NULL,
    `updatedAt` DATETIME NOT NULL,
    PRIMARY KEY (`parcelId`),
    UNIQUE KEY `uq_parcel_net` (`licenseId`, `parcelNetworkStatus`),
    KEY `idx_parcel_status` (`licenseId`, `parcelStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
