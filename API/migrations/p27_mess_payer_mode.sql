-- P27: Mess-level payer mode (user vs institute). Additive only.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mess_shop_setting` (
  `userId` INT NOT NULL,
  `payer_mode` VARCHAR(16) NOT NULL DEFAULT 'user',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
