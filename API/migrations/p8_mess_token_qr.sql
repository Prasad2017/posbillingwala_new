-- P8: Mess QR token support (walk-in + scan verify). Additive only.
-- Run on production MySQL alongside existing mess_invoice (paper coupons unchanged).

CREATE TABLE IF NOT EXISTS `mess_token` (
  `tokenId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `organization_id` int(11) DEFAULT NULL,
  `branch_id` int(11) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `tokenCode` varchar(64) NOT NULL,
  `memberId` varchar(64) DEFAULT NULL,
  `memberName` text NOT NULL,
  `memberMobile` varchar(32) DEFAULT NULL,
  `memberType` varchar(16) NOT NULL DEFAULT 'walk_in',
  `messType` varchar(32) NOT NULL,
  `tokenAmount` varchar(32) DEFAULT '0',
  `tokenDate` datetime NOT NULL,
  `verifiedDate` datetime DEFAULT NULL,
  `tokenNetworkStatus` varchar(64) NOT NULL,
  `tokenStatus` varchar(16) NOT NULL DEFAULT 'active',
  `verifyNetworkStatus` varchar(64) DEFAULT NULL,
  `syncStatus` varchar(8) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`tokenId`),
  UNIQUE KEY `uq_mess_token_code` (`tokenCode`),
  UNIQUE KEY `uq_mess_token_network` (`tokenNetworkStatus`),
  KEY `idx_mess_token_user_date` (`userId`, `tokenDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
