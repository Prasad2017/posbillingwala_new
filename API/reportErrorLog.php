<?php
/**
 * POS: report a crash / API / printer / DB / network error for Admin inbox.
 * Never blocks POS — always returns quickly. Upserts by fingerprint.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/log_sanitizer.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

mysqli_query($con, 'set names utf8');
$postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';
pos_require_auth($con, $postedUserId, array('status' => '0', 'message' => 'Unauthorized'));

date_default_timezone_set('Asia/Kolkata');
$now = date('Y-m-d H:i:s');

$fingerprint = isset($_POST['fingerprint']) ? trim($_POST['fingerprint']) : '';
if ($fingerprint === '' || strlen($fingerprint) > 64) {
    $fingerprint = hash('sha256', microtime(true) . mt_rand() . $postedUserId);
}

$maxBody = 20480;

$originalErrorMessage = log_post_str('original_error_message', $maxBody);
if (log_is_generic_message($originalErrorMessage)) {
    // Prefer a more specific field if the client accidentally sent a generic.
    $fallback = log_post_str('error_message', $maxBody);
    if ($fallback !== '' && !log_is_generic_message($fallback)) {
        $originalErrorMessage = $fallback;
    }
}

$fields = array(
    'error_type' => log_post_str('error_type', 32),
    'severity' => log_post_str('severity', 16),
    'error_category' => log_post_str('error_category', 64),
    'summary' => log_post_str('summary', 512),
    'app_type' => log_post_str('app_type', 32),
    'app_version' => log_post_str('app_version', 32),
    'customer_id' => log_post_str('customer_id', 64),
    'shop_name' => log_post_str('shop_name', 255),
    'branch_label' => log_post_str('branch_label', 255),
    'device_name' => log_post_str('device_name', 255),
    'device_id' => log_post_str('device_id', 255),
    'user_label' => log_post_str('user_label', 255),
    'screen_name' => log_post_str('screen_name', 255),
    'activity_name' => log_post_str('activity_name', 255),
    'fragment_name' => log_post_str('fragment_name', 255),
    'user_action' => log_post_str('user_action', 512),
    'what_happened' => log_post_str('what_happened', 4096),
    'user_flow' => log_post_str('user_flow', 8192),
    'breadcrumbs' => log_post_str('breadcrumbs', 8192),
    'api_method' => log_post_str('api_method', 16),
    'api_url' => log_post_str('api_url', 1024),
    'http_status' => isset($_POST['http_status']) ? (int) $_POST['http_status'] : null,
    'request_body' => log_post_str('request_body', $maxBody),
    'response_body' => log_post_str('response_body', $maxBody),
    'request_size' => isset($_POST['request_size']) ? (int) $_POST['request_size'] : null,
    'response_size' => isset($_POST['response_size']) ? (int) $_POST['response_size'] : null,
    'request_duration_ms' => isset($_POST['request_duration_ms']) ? (int) $_POST['request_duration_ms'] : null,
    'printer_type' => log_post_str('printer_type', 64),
    'printer_model' => log_post_str('printer_model', 128),
    'printer_connection' => log_post_str('printer_connection', 64),
    'print_operation' => log_post_str('print_operation', 128),
    'original_error_message' => $originalErrorMessage,
    'original_exception_class' => log_post_str('original_exception_class', 512),
    'original_stack_trace' => log_post_str('original_stack_trace', $maxBody),
    'original_error_code' => log_post_str('original_error_code', 128),
    'original_api_response' => log_post_str('original_api_response', $maxBody),
);

if ($fields['error_type'] === '') {
    $fields['error_type'] = 'APPLICATION';
}
if ($fields['severity'] === '') {
    $fields['severity'] = 'ERROR';
}
if ($fields['app_type'] === '') {
    $fields['app_type'] = 'POS';
}
if ($fields['customer_id'] === '' && $postedUserId !== '') {
    $fields['customer_id'] = (string) $postedUserId;
}
if ($fields['summary'] === '') {
    $parts = array();
    if ($fields['error_type'] !== '') {
        $parts[] = $fields['error_type'];
    }
    if ($fields['screen_name'] !== '') {
        $parts[] = $fields['screen_name'];
    }
    if ($fields['user_action'] !== '') {
        $parts[] = $fields['user_action'];
    }
    $fields['summary'] = implode(' — ', $parts);
    if ($fields['summary'] === '') {
        $fields['summary'] = 'Error reported';
    }
}
if ($fields['original_api_response'] === '' && $fields['response_body'] !== '') {
    $fields['original_api_response'] = $fields['response_body'];
}
if ($fields['response_body'] === '' && $fields['original_api_response'] !== '') {
    $fields['response_body'] = $fields['original_api_response'];
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT id, occurrence_count FROM `error_logs` WHERE `fingerprint`=? LIMIT 1',
    's',
    $fingerprint
);

if ($existing !== null) {
    $ok = db_stmt_execute(
        $con,
        'UPDATE `error_logs` SET
            `occurrence_count` = `occurrence_count` + 1,
            `last_seen_at` = ?,
            `severity` = ?,
            `summary` = ?,
            `user_action` = ?,
            `what_happened` = ?,
            `user_flow` = ?,
            `breadcrumbs` = ?,
            `request_body` = ?,
            `response_body` = ?,
            `original_error_message` = ?,
            `original_exception_class` = ?,
            `original_stack_trace` = ?,
            `original_error_code` = ?,
            `original_api_response` = ?,
            `http_status` = ?,
            `request_duration_ms` = ?,
            `request_size` = ?,
            `response_size` = ?
         WHERE `fingerprint` = ?',
        'ssssssssssssssiiiis',
        $now,
        $fields['severity'],
        $fields['summary'],
        $fields['user_action'],
        $fields['what_happened'],
        $fields['user_flow'],
        $fields['breadcrumbs'],
        $fields['request_body'],
        $fields['response_body'],
        $fields['original_error_message'],
        $fields['original_exception_class'],
        $fields['original_stack_trace'],
        $fields['original_error_code'],
        $fields['original_api_response'],
        $fields['http_status'] !== null ? (int) $fields['http_status'] : 0,
        $fields['request_duration_ms'] !== null ? (int) $fields['request_duration_ms'] : 0,
        $fields['request_size'] !== null ? (int) $fields['request_size'] : 0,
        $fields['response_size'] !== null ? (int) $fields['response_size'] : 0,
        $fingerprint
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'updated' : 'update failed';
    $response['id'] = (string) $existing['id'];
    $response['occurrence_count'] = (string) ((int) $existing['occurrence_count'] + 1);
} else {
    $insertId = db_stmt_insert_id(
        $con,
        'INSERT INTO `error_logs` (
            `fingerprint`, `occurrence_count`, `first_seen_at`, `last_seen_at`,
            `error_type`, `severity`, `error_category`, `summary`,
            `app_type`, `app_version`, `customer_id`, `shop_name`, `branch_label`,
            `device_name`, `device_id`, `user_label`,
            `screen_name`, `activity_name`, `fragment_name`, `user_action`, `what_happened`,
            `user_flow`, `breadcrumbs`,
            `api_method`, `api_url`, `http_status`,
            `request_body`, `response_body`, `request_size`, `response_size`, `request_duration_ms`,
            `printer_type`, `printer_model`, `printer_connection`, `print_operation`,
            `original_error_message`, `original_exception_class`, `original_stack_trace`,
            `original_error_code`, `original_api_response`
         ) VALUES (
            ?, 1, ?, ?,
            ?, ?, ?, ?,
            ?, ?, ?, ?, ?,
            ?, ?, ?,
            ?, ?, ?, ?, ?,
            ?, ?,
            ?, ?, ?,
            ?, ?, ?, ?, ?,
            ?, ?, ?, ?,
            ?, ?, ?,
            ?, ?
         )',
        'ssssssssssssssssssssssssissiiiisssssssss',
        $fingerprint,
        $now,
        $now,
        $fields['error_type'],
        $fields['severity'],
        $fields['error_category'],
        $fields['summary'],
        $fields['app_type'],
        $fields['app_version'],
        $fields['customer_id'],
        $fields['shop_name'],
        $fields['branch_label'],
        $fields['device_name'],
        $fields['device_id'],
        $fields['user_label'],
        $fields['screen_name'],
        $fields['activity_name'],
        $fields['fragment_name'],
        $fields['user_action'],
        $fields['what_happened'],
        $fields['user_flow'],
        $fields['breadcrumbs'],
        $fields['api_method'],
        $fields['api_url'],
        $fields['http_status'] !== null ? (int) $fields['http_status'] : 0,
        $fields['request_body'],
        $fields['response_body'],
        $fields['request_size'] !== null ? (int) $fields['request_size'] : 0,
        $fields['response_size'] !== null ? (int) $fields['response_size'] : 0,
        $fields['request_duration_ms'] !== null ? (int) $fields['request_duration_ms'] : 0,
        $fields['printer_type'],
        $fields['printer_model'],
        $fields['printer_connection'],
        $fields['print_operation'],
        $fields['original_error_message'],
        $fields['original_exception_class'],
        $fields['original_stack_trace'],
        $fields['original_error_code'],
        $fields['original_api_response']
    );
    if ($insertId) {
        $response['status'] = '1';
        $response['message'] = 'created';
        $response['id'] = (string) $insertId;
        $response['occurrence_count'] = '1';
    } else {
        $response['status'] = '0';
        $response['message'] = 'insert failed';
    }
}

echo json_encode($response);
?>
