-- POS home banners uploaded from the admin panel (one or many).
CREATE TABLE IF NOT EXISTS `pos_home_banners` (
    `bannerId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `image_path` VARCHAR(255) NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0,
    `is_active` TINYINT UNSIGNED NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`bannerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
