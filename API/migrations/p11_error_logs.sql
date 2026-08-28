-- P11: Crash & Error Logs (POS → Admin inbox)
-- Additive only — does not alter existing shop tables.

CREATE TABLE IF NOT EXISTS `error_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `fingerprint` CHAR(64) NOT NULL,
  `occurrence_count` INT UNSIGNED NOT NULL DEFAULT 1,
  `first_seen_at` DATETIME NOT NULL,
  `last_seen_at` DATETIME NOT NULL,

  `error_type` VARCHAR(32) NOT NULL DEFAULT 'APPLICATION',
  `severity` VARCHAR(16) NOT NULL DEFAULT 'ERROR',
  `error_category` VARCHAR(64) NULL DEFAULT NULL,
  `summary` VARCHAR(512) NOT NULL DEFAULT '',

  `app_type` VARCHAR(32) NOT NULL DEFAULT 'POS',
  `app_version` VARCHAR(32) NULL DEFAULT NULL,
  `customer_id` VARCHAR(64) NULL DEFAULT NULL,
  `shop_name` VARCHAR(255) NULL DEFAULT NULL,
  `branch_label` VARCHAR(255) NULL DEFAULT NULL,
  `device_name` VARCHAR(255) NULL DEFAULT NULL,
  `device_id` VARCHAR(255) NULL DEFAULT NULL,
  `user_label` VARCHAR(255) NULL DEFAULT NULL,

  `screen_name` VARCHAR(255) NULL DEFAULT NULL,
  `activity_name` VARCHAR(255) NULL DEFAULT NULL,
  `fragment_name` VARCHAR(255) NULL DEFAULT NULL,
  `user_action` VARCHAR(512) NULL DEFAULT NULL,
  `what_happened` TEXT NULL,

  `user_flow` TEXT NULL,
  `breadcrumbs` MEDIUMTEXT NULL,

  `api_method` VARCHAR(16) NULL DEFAULT NULL,
  `api_url` VARCHAR(1024) NULL DEFAULT NULL,
  `http_status` INT NULL DEFAULT NULL,
  `request_body` MEDIUMTEXT NULL,
  `response_body` MEDIUMTEXT NULL,
  `request_size` INT UNSIGNED NULL DEFAULT NULL,
  `response_size` INT UNSIGNED NULL DEFAULT NULL,
  `request_duration_ms` INT UNSIGNED NULL DEFAULT NULL,

  `printer_type` VARCHAR(64) NULL DEFAULT NULL,
  `printer_model` VARCHAR(128) NULL DEFAULT NULL,
  `printer_connection` VARCHAR(64) NULL DEFAULT NULL,
  `print_operation` VARCHAR(128) NULL DEFAULT NULL,

  `original_error_message` MEDIUMTEXT NULL,
  `original_exception_class` VARCHAR(512) NULL DEFAULT NULL,
  `original_stack_trace` MEDIUMTEXT NULL,
  `original_error_code` VARCHAR(128) NULL DEFAULT NULL,
  `original_api_response` MEDIUMTEXT NULL,

  `resolution_notes` TEXT NULL,
  `resolved_at` DATETIME NULL DEFAULT NULL,
  `resolved_by` VARCHAR(128) NULL DEFAULT NULL,

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_error_logs_fingerprint` (`fingerprint`),
  KEY `idx_error_logs_last_seen` (`last_seen_at`),
  KEY `idx_error_logs_severity` (`severity`),
  KEY `idx_error_logs_type` (`error_type`),
  KEY `idx_error_logs_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
