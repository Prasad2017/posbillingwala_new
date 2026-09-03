<?php
/**
 * Shared support ticket helpers (POS + Admin).
 */
require_once __DIR__ . '/db_prepared.php';

function support_ensure_ticket_columns($con)
{
    require_once __DIR__ . '/php_compat.php';
    $columns = array(
        'licence_id' => "ALTER TABLE `admin_support_tickets` ADD COLUMN `licence_id` INT UNSIGNED NULL DEFAULT NULL AFTER `status`",
        'user_id' => "ALTER TABLE `admin_support_tickets` ADD COLUMN `user_id` INT UNSIGNED NULL DEFAULT NULL AFTER `licence_id`",
        'shop_name' => "ALTER TABLE `admin_support_tickets` ADD COLUMN `shop_name` VARCHAR(255) NULL DEFAULT '' AFTER `user_id`",
        'device_name' => "ALTER TABLE `admin_support_tickets` ADD COLUMN `device_name` VARCHAR(120) NULL DEFAULT '' AFTER `shop_name`",
        'device_id' => "ALTER TABLE `admin_support_tickets` ADD COLUMN `device_id` VARCHAR(255) NULL DEFAULT '' AFTER `device_name`",
    );
    try {
        foreach ($columns as $name => $sql) {
            $chk = db_safe_query(
                $con,
                "SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='admin_support_tickets' AND COLUMN_NAME='" . mysqli_real_escape_string($con, $name) . "'"
            );
            if ($chk && ($row = mysqli_fetch_assoc($chk)) && (int) $row['c'] === 0) {
                db_safe_query($con, $sql);
            }
            if ($chk) {
                mysqli_free_result($chk);
            }
        }
    } catch (Throwable $e) {
        // Ignore schema probe failures.
    }
}

function support_fetch_licence_context($con, $licenceId)
{
    $licenceId = (int) $licenceId;
    if ($licenceId <= 0) {
        return array('licence_id' => 0, 'user_id' => 0, 'shop_name' => '', 'device_name' => '', 'device_id' => '');
    }
    $row = db_stmt_fetch_one(
        $con,
        "SELECT l.id AS licence_id, l.userId AS user_id,
                IFNULL(u.shopName,'') AS shop_name,
                IFNULL(l.android_device_name,'') AS device_name,
                IFNULL(l.android_device_id,'') AS device_id
         FROM licenses l
         LEFT JOIN users u ON u.id = l.userId
         WHERE l.id=? LIMIT 1",
        'i',
        $licenceId
    );
    if ($row === null) {
        return array('licence_id' => $licenceId, 'user_id' => 0, 'shop_name' => '', 'device_name' => '', 'device_id' => '');
    }
    return array(
        'licence_id' => (int) $row['licence_id'],
        'user_id' => (int) $row['user_id'],
        'shop_name' => (string) $row['shop_name'],
        'device_name' => (string) $row['device_name'],
        'device_id' => (string) $row['device_id'],
    );
}

function support_format_ticket($row)
{
    return array(
        'id' => (string) $row['id'],
        'ticketNo' => (string) $row['ticket_no'],
        'appName' => (string) $row['app_name'],
        'category' => (string) $row['category'],
        'subject' => (string) $row['subject'],
        'description' => (string) ($row['description'] ?? ''),
        'status' => (string) $row['status'],
        'ticketStatus' => (string) $row['status'],
        'createdAt' => (string) $row['created_at'],
        'licenceId' => isset($row['licence_id']) ? (string) $row['licence_id'] : '',
        'shopName' => isset($row['shop_name']) ? (string) $row['shop_name'] : '',
        'deviceName' => isset($row['device_name']) ? (string) $row['device_name'] : '',
    );
}

function support_format_messages($con, $ticketId)
{
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT * FROM admin_support_messages WHERE ticket_id=? ORDER BY id ASC",
        'i',
        (int) $ticketId
    );
    $messages = array();
    foreach ($rows as $m) {
        $messages[] = array(
            'id' => (string) $m['id'],
            'sender' => (string) $m['sender'],
            'message' => (string) $m['message'],
            'createdAt' => (string) $m['created_at'],
        );
    }
    return $messages;
}

function support_generate_ticket_no()
{
    return 'TKT-' . date('Ymd') . '-' . str_pad((string) random_int(1, 9999), 4, '0', STR_PAD_LEFT);
}

function support_ticket_owned_by_licence($con, $ticketId, $licenceId)
{
    $t = db_stmt_fetch_one($con, "SELECT licence_id FROM admin_support_tickets WHERE id=? LIMIT 1", 'i', (int) $ticketId);
    if ($t === null) {
        return false;
    }
    if (!isset($t['licence_id']) || $t['licence_id'] === null || (int) $t['licence_id'] <= 0) {
        return false;
    }
    return (int) $t['licence_id'] === (int) $licenceId;
}

?>
