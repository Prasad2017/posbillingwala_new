-- p23: Website catalog CMS (dealers, pricing, products, company settings)
-- Safe to re-run. Does NOT change POS billing tables (users, licenses, invoice, etc.).
-- Run on the same database as admin.posbillingwala.com (Laravel admin).

-- -----------------------------------------------------------------------------
-- website_clients: city + business category for trusted customer showcase
-- -----------------------------------------------------------------------------

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'website_clients' AND COLUMN_NAME = 'city') = 0,
    'ALTER TABLE `website_clients` ADD COLUMN `city` varchar(120) NOT NULL DEFAULT '''' AFTER `subtitle`',
    'SELECT ''OK: website_clients.city exists'' AS msg'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'website_clients' AND COLUMN_NAME = 'business_category') = 0,
    'ALTER TABLE `website_clients` ADD COLUMN `business_category` varchar(120) NOT NULL DEFAULT '''' AFTER `city`',
    'SELECT ''OK: website_clients.business_category exists'' AS msg'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- New website CMS tables
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `website_dealers` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `area` varchar(120) NOT NULL,
  `dealer_name` varchar(255) NOT NULL,
  `contact_person` varchar(255) NOT NULL DEFAULT '',
  `role_title` varchar(255) NOT NULL DEFAULT '',
  `mobile` varchar(32) NOT NULL DEFAULT '',
  `whatsapp` varchar(32) NOT NULL DEFAULT '',
  `address` text,
  `map_url` varchar(500) NOT NULL DEFAULT '',
  `dealer_type` varchar(32) NOT NULL DEFAULT 'authorized_dealer',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `website_dealers_area_idx` (`area`),
  KEY `website_dealers_published_sort_idx` (`is_published`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_pricing_plans` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `plan_type` varchar(32) NOT NULL,
  `validity_label` varchar(64) NOT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `gst_note` varchar(120) NOT NULL DEFAULT 'GST included',
  `description` varchar(500) NOT NULL DEFAULT '',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `website_pricing_published_sort_idx` (`is_published`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_products` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `category` varchar(64) NOT NULL,
  `description` text,
  `icon` varchar(16) NOT NULL DEFAULT '',
  `sort_order` int unsigned NOT NULL DEFAULT 0,
  `is_published` tinyint unsigned NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `website_products_category_idx` (`category`),
  KEY `website_products_published_sort_idx` (`is_published`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `website_settings` (
  `setting_key` varchar(80) NOT NULL,
  `setting_value` text,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Sample data (safe to re-run — skips rows that already exist)
-- -----------------------------------------------------------------------------

-- Company settings (key-value)
INSERT IGNORE INTO `website_settings` (`setting_key`, `setting_value`) VALUES
('legal_company_name', 'CANA Tech Solutions Private Limited'),
('brand_tagline', 'Smart Billing. Trusted Support. Better Business.'),
('gstin', ''),
('office_address', 'Pune, Maharashtra, India'),
('support_phone', '9325901176'),
('support_whatsapp', '9325901176'),
('support_email', 'support@posbillingwala.com'),
('sales_email', 'hello@posbillingwala.com'),
('business_hours', 'Mon–Sat, 10:00 AM – 7:00 PM IST'),
('play_store_url', 'https://play.google.com/store/apps/details?id=com.pos_billingwala'),
('apk_download_url', ''),
('app_latest_version', '');

-- Products catalog (only if table is empty)
INSERT INTO `website_products` (`name`, `category`, `description`, `icon`, `sort_order`, `is_published`)
SELECT `name`, `category`, `description`, `icon`, `sort_order`, 1 FROM (
  SELECT 'POS Billing Software' AS `name`, 'software' AS `category`, 'Android mobile & tablet billing with offline sync, licensing, and thermal print.' AS `description`, '📱' AS `icon`, 10 AS `sort_order`
  UNION ALL SELECT 'Android Mobile & Tablet Billing', 'software', 'Counter-ready POS app for restaurants, retail, mess, and takeaway.', '📲', 20
  UNION ALL SELECT 'POS Machine', 'hardware', 'Counter POS hardware compatible with POS Billingwala workflows.', '🖥️', 30
  UNION ALL SELECT 'Bluetooth / Thermal Printer', 'hardware', '57mm and 80mm Bluetooth thermal printers for fast receipt printing.', '🖨️', 40
  UNION ALL SELECT '57mm Billing Rolls', 'consumables', 'Thermal billing rolls for compact receipt printers.', '🧾', 50
  UNION ALL SELECT '80mm Billing Rolls', 'consumables', 'Standard-width thermal rolls for restaurant and retail counters.', '🧾', 60
  UNION ALL SELECT 'Barcode Labels & Ribbons', 'consumables', 'Labels and ribbons for inventory and retail tagging.', '🏷️', 70
  UNION ALL SELECT 'Accessories', 'accessories', 'Cables, stands, and billing counter accessories.', '🔌', 80
) AS seed
WHERE (SELECT COUNT(*) FROM `website_products`) = 0;

-- Pricing plans (only if table is empty)
INSERT INTO `website_pricing_plans` (`plan_type`, `validity_label`, `price`, `gst_note`, `description`, `sort_order`, `is_published`)
SELECT `plan_type`, `validity_label`, `price`, `gst_note`, `description`, `sort_order`, 1 FROM (
  SELECT 'subscription' AS `plan_type`, '6 Months' AS `validity_label`, 0.00 AS `price`, 'GST included' AS `gst_note`, 'New customer — first purchase' AS `description`, 10 AS `sort_order`
  UNION ALL SELECT 'subscription', '1 Year', 0.00, 'GST included', 'New customer — best value annual plan', 20
  UNION ALL SELECT 'renewal', '6 Months', 0.00, 'GST included', 'Existing customer — extend licence', 30
  UNION ALL SELECT 'renewal', '1 Year', 0.00, 'GST included', 'Existing customer — annual renewal', 40
) AS seed
WHERE (SELECT COUNT(*) FROM `website_pricing_plans`) = 0;

-- Area dealers (insert per area if missing)
INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Pune', 'Pune Office', 'Santosh Dixit', 'Sales & Marketing Manager', '9325901176', '9325901176', 'Pune, Maharashtra, India', '', 'head_office', 10, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Pune');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Pandharpur', 'Authorized POS Billingwala Dealer', '', 'Authorized Dealer', '', '', 'Pandharpur, Maharashtra, India', '', 'authorized_dealer', 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Pandharpur');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Satara', 'Authorized POS Billingwala Dealer — Satara', '', 'Authorized Dealer', '', '', 'Satara, Maharashtra, India', '', 'authorized_dealer', 30, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Satara');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Solapur', 'Authorized POS Billingwala Dealer — Solapur', '', 'Authorized Dealer', '', '', 'Solapur, Maharashtra, India', '', 'authorized_dealer', 40, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Solapur');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Kolhapur', 'Authorized POS Billingwala Dealer — Kolhapur', '', 'Authorized Dealer', '', '', 'Kolhapur, Maharashtra, India', '', 'authorized_dealer', 50, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Kolhapur');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Sangli', 'Authorized POS Billingwala Dealer — Sangli', '', 'Authorized Dealer', '', '', 'Sangli, Maharashtra, India', '', 'authorized_dealer', 60, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Sangli');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Ahmednagar', 'Authorized POS Billingwala Dealer — Ahmednagar', '', 'Authorized Dealer', '', '', 'Ahmednagar, Maharashtra, India', '', 'authorized_dealer', 70, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Ahmednagar');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Nashik', 'Authorized POS Billingwala Dealer — Nashik', '', 'Authorized Dealer', '', '', 'Nashik, Maharashtra, India', '', 'authorized_dealer', 80, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Nashik');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Mumbai', 'Authorized POS Billingwala Dealer — Mumbai', '', 'Authorized Dealer', '', '', 'Mumbai, Maharashtra, India', '', 'authorized_dealer', 90, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Mumbai');

INSERT INTO `website_dealers` (`area`, `dealer_name`, `contact_person`, `role_title`, `mobile`, `whatsapp`, `address`, `map_url`, `dealer_type`, `sort_order`, `is_published`)
SELECT 'Karnataka', 'Authorized POS Billingwala Dealer — Karnataka', '', 'Regional Dealer', '', '', 'Karnataka, India', '', 'authorized_dealer', 100, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_dealers` WHERE `area` = 'Karnataka');

-- CMS pages: terms, refund, support, company (requires website_pages from p14)
INSERT INTO `website_pages` (`slug`, `title`, `body_html`)
SELECT 'terms', 'Terms & Conditions', '<p>These Terms &amp; Conditions govern your use of POS Billingwala software, services, and related products sold by CANA Tech Solutions Private Limited.</p><h2>License use</h2><p>Each subscription or renewal grants use of POS Billingwala on licensed device(s) for the purchased validity period. Licence keys are non-transferable unless approved in writing.</p><h2>Software updates</h2><p>We may release updates, fixes, and feature improvements during your active licence period.</p><h2>Support</h2><p>Installation, training, and technical support are provided as described on our Support page and by your authorized dealer.</p><h2>Limitation of liability</h2><p>POS Billingwala is provided on an &ldquo;as available&rdquo; basis. We are not liable for indirect business losses beyond applicable law.</p><h2>Contact</h2><p>Questions about these terms: <a href=\"mailto:support@posbillingwala.com\">support@posbillingwala.com</a>.</p>'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_pages` WHERE `slug` = 'terms');

INSERT INTO `website_pages` (`slug`, `title`, `body_html`)
SELECT 'refund-renewal', 'Refund & Renewal Policy', '<h2>Renewal policy</h2><ul><li>Licences are valid for the purchased period (6 months or 1 year).</li><li>Renew before expiry to continue uninterrupted access.</li><li>Contact your local dealer or our support team for renewal.</li><li>Same licence key may be renewed for eligible accounts.</li></ul><h2>Refund policy</h2><ul><li><strong>Software:</strong> Subscription fees are generally non-refundable after licence activation, except where required by law or explicitly agreed in writing.</li><li><strong>Hardware:</strong> Defective hardware may be replaced within the warranty period stated on your invoice.</li><li><strong>Disputes:</strong> Email <a href=\"mailto:support@posbillingwala.com\">support@posbillingwala.com</a> with invoice and licence details.</li></ul>'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_pages` WHERE `slug` = 'refund-renewal');

INSERT INTO `website_pages` (`slug`, `title`, `body_html`)
SELECT 'support', 'Customer Support', '<p><strong>We Don&rsquo;t Just Sell Software &mdash; We Support Your Business.</strong></p><p>Our team and authorized dealers help you from installation through daily operations and renewal.</p><h2>What we help with</h2><ul><li>Installation &amp; setup</li><li>Product / menu creation support</li><li>Data sync &amp; software updates</li><li>Remote support</li><li>Printer setup (Bluetooth / thermal)</li><li>Billing software training</li><li>Renewal support</li><li>Dealer-backed local support</li><li>WhatsApp / phone support</li></ul>'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_pages` WHERE `slug` = 'support');

INSERT INTO `website_pages` (`slug`, `title`, `body_html`)
SELECT 'company', 'Company Model', '<p>POS Billingwala operates as a <strong>dealer-network based SaaS + hardware + support business</strong> — not only a software download.</p><h2>Organizational structure</h2><pre style=\"white-space:pre-wrap;font-family:inherit;line-height:1.6;background:#f8fafc;padding:1rem;border-radius:12px;\">POS Billingwala → Company / Head Office\n  → Regional Dealers\n    → Area Dealers\n      → Sales &amp; Marketing Team\n      → Customer Support Team\n      → Installation / Technical Team\n        → Customers</pre><h2>Head Office</h2><p>Product development, brand, pricing policy, licence system, legal compliance, and central support escalation.</p><h2>Regional &amp; Area Dealers</h2><p>Local sales, installation, first-line support, and renewal collection in your city.</p><h2>Support &amp; Technical Teams</h2><p>Printer setup, menu creation, remote help, training, and renewal reminders.</p><h2>Revenue model</h2><ul><li><strong>Software</strong> — subscription &amp; renewal (6 months / 1 year)</li><li><strong>Hardware</strong> — POS machine, Bluetooth / thermal printer</li><li><strong>Consumables</strong> — 57mm &amp; 80mm billing rolls, labels</li><li><strong>Services</strong> — installation, training, remote support</li></ul>'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `website_pages` WHERE `slug` = 'company');

-- Sample trusted customers (only if none exist — replace with real logos in admin)
INSERT INTO `website_clients` (`business_name`, `subtitle`, `city`, `business_category`, `description`, `logo_path`, `photo_path`, `cta_url`, `sort_order`, `is_published`)
SELECT `business_name`, `subtitle`, `city`, `business_category`, `description`, '', '', '', `sort_order`, 1 FROM (
  SELECT 'Hotel Shree' AS `business_name`, 'Owner · Pune' AS `subtitle`, 'Pune' AS `city`, 'Restaurant' AS `business_category`, 'Daily billing, KOT printing, and table-wise sales with POS Billingwala.' AS `description`, 10 AS `sort_order`
  UNION ALL SELECT 'Balaji Kirana Store', 'Retail · Pandharpur', 'Pandharpur', 'Retail', 'Fast barcode billing and thermal receipt printing for daily customers.', 20
  UNION ALL SELECT 'Mess Prasad', 'Mess · Solapur', 'Solapur', 'Mess', 'Mess token billing and member payment tracking.', 30
) AS seed
WHERE (SELECT COUNT(*) FROM `website_clients`) = 0;

-- Verify schema + sample row counts
SELECT
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'website_dealers') AS website_dealers_ok,
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'website_pricing_plans') AS website_pricing_ok,
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'website_products') AS website_products_ok,
  (SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'website_settings') AS website_settings_ok,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'website_clients' AND COLUMN_NAME = 'city') AS website_clients_city_ok,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'website_clients' AND COLUMN_NAME = 'business_category') AS website_clients_category_ok;

SELECT
  (SELECT COUNT(*) FROM `website_settings`) AS settings_rows,
  (SELECT COUNT(*) FROM `website_products`) AS products_rows,
  (SELECT COUNT(*) FROM `website_pricing_plans`) AS pricing_rows,
  (SELECT COUNT(*) FROM `website_dealers`) AS dealers_rows,
  (SELECT COUNT(*) FROM `website_clients`) AS clients_rows,
  (SELECT COUNT(*) FROM `website_pages` WHERE `slug` IN ('terms','refund-renewal','support','company')) AS cms_pages_rows;
