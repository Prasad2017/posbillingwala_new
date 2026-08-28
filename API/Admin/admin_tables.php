<?php
/**
 * Ensure support + crash tables exist (idempotent).
 */
require_once __DIR__ . '/../support_helpers.php';

function admin_ensure_support_crash_tables($con) {
    mysqli_query($con, "CREATE TABLE IF NOT EXISTS `admin_support_tickets` (
        `id` INT AUTO_INCREMENT PRIMARY KEY,
        `ticket_no` VARCHAR(40) NOT NULL,
        `app_name` VARCHAR(40) NOT NULL DEFAULT 'POS App',
        `category` VARCHAR(80) NOT NULL DEFAULT 'General',
        `subject` VARCHAR(255) NOT NULL,
        `description` TEXT,
        `status` VARCHAR(32) NOT NULL DEFAULT 'Open',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    support_ensure_ticket_columns($con);

    mysqli_query($con, "CREATE TABLE IF NOT EXISTS `admin_support_messages` (
        `id` INT AUTO_INCREMENT PRIMARY KEY,
        `ticket_id` INT NOT NULL,
        `sender` VARCHAR(80) NOT NULL DEFAULT 'Admin',
        `message` TEXT NOT NULL,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        INDEX (`ticket_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    mysqli_query($con, "CREATE TABLE IF NOT EXISTS `admin_crash_logs` (
        `id` INT AUTO_INCREMENT PRIMARY KEY,
        `error_title` VARCHAR(255) NOT NULL,
        `error_class` VARCHAR(255) DEFAULT '',
        `app_name` VARCHAR(40) NOT NULL DEFAULT 'POS App',
        `status` VARCHAR(32) NOT NULL DEFAULT 'New',
        `device_name` VARCHAR(120) DEFAULT '',
        `android_version` VARCHAR(40) DEFAULT '',
        `app_version` VARCHAR(40) DEFAULT '',
        `user_name` VARCHAR(120) DEFAULT '',
        `user_id` VARCHAR(40) DEFAULT '',
        `occurrences` INT NOT NULL DEFAULT 1,
        `stack_trace` MEDIUMTEXT,
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
}

function admin_ensure_website_tables($con) {
    mysqli_query($con, "CREATE TABLE IF NOT EXISTS `website_contact_messages` (
        `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
        `name` VARCHAR(255) NOT NULL,
        `email` VARCHAR(255) NOT NULL,
        `subject` VARCHAR(255) NOT NULL DEFAULT '',
        `message` TEXT NOT NULL,
        `status` VARCHAR(32) NOT NULL DEFAULT 'New',
        `source_ip` VARCHAR(64) NOT NULL DEFAULT '',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
}

function website_format_contact($row) {
    return array(
        'id' => (string) $row['id'],
        'name' => (string) $row['name'],
        'email' => (string) $row['email'],
        'subject' => (string) ($row['subject'] ?? ''),
        'message' => (string) $row['message'],
        'contactStatus' => (string) $row['status'],
        'status' => (string) $row['status'],
        'createdAt' => (string) $row['created_at'],
        'sourceIp' => (string) ($row['source_ip'] ?? ''),
    );
}
