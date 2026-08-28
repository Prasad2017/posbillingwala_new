-- Website CMS tables (privacy policy, client showcase, testimonials)
-- Safe to re-run.

CREATE TABLE IF NOT EXISTS `website_pages` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `slug` varchar(80) NOT NULL,
  `title` varchar(255) NOT NULL,
  `body_html` mediumtext NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `website_pages_slug_unique` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_clients` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `business_name` varchar(255) NOT NULL,
  `subtitle` varchar(255) NOT NULL DEFAULT '',
  `description` text,
  `logo_path` varchar(500) NOT NULL DEFAULT '',
  `photo_path` varchar(500) NOT NULL DEFAULT '',
  `cta_url` varchar(500) NOT NULL DEFAULT '',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_testimonials` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `author_name` varchar(255) NOT NULL,
  `business_name` varchar(255) NOT NULL DEFAULT '',
  `quote` text NOT NULL,
  `rating` tinyint unsigned NOT NULL DEFAULT 5,
  `photo_path` varchar(500) NOT NULL DEFAULT '',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
