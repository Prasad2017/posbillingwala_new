<?php
/**
 * POS: report a crash / ANR / API / printer / DB / network / device (memory, storage,
 * thermal, battery) error for Admin inbox.
 * Always inserts a new row so Admin can inspect every occurrence.
 * Auth is best-effort — crash logs must still save if the session expired.
 */
include_once('config.php');
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/log_sanitizer.php';
require_once __DIR__ . '/error_logs_ensure.php';
require_once __DIR__ . '/Admin/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

mysqli_query($con, 'set names utf8');
error_logs_ensure($con);
$postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';

date_default_timezone_set('Asia/Kolkata');
$now = date('Y-m-d H:i:s');

$fingerprint = isset($_POST['fingerprint']) ? trim($_POST['fingerprint']) : '';
if ($fingerprint === '' || strlen($fingerprint) > 64) {
    $fingerprint = hash('sha256', microtime(true) . mt_rand() . $postedUserId);
}
// One DB row per event so Admin can inspect every log, not only the last grouped one.
$fingerprint = hash('sha256', $fingerprint . '|' . $now . '|' . uniqid('', true));

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
$licenceId = null;
try {
    $licenceId = auth_pos_licence_id_from_request($con, (string) $postedUserId);
} catch (Throwable $e) {
    $licenceId = null;
}
if ($licenceId !== null && $licenceId !== '' && $fields['customer_id'] === '') {
    $fields['customer_id'] = (string) $licenceId;
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

try {
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
        'ssssssssssssssssssssssssissiiisssssssss',
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
} catch (Throwable $e) {
    $response['status'] = '0';
    $response['message'] = 'insert failed';
}

if ($response['status'] === '1') {
    error_log_mirror_to_admin_crash($con, $fingerprint, $fields, $now);
}

echo json_encode($response);

function error_log_mirror_to_admin_crash($con, $fingerprint, $fields, $now)
{
    try {
        admin_ensure_support_crash_tables($con);
        $title = $fields['summary'] !== '' ? $fields['summary'] : $fields['original_exception_class'];
        if ($title === '') {
            $title = $fields['error_type'];
        }
        if (strlen($title) > 255) {
            $title = substr($title, 0, 252) . '...';
        }
        $appName = 'POS App';
        $appType = strtoupper($fields['app_type']);
        if ($appType === 'DEALER') {
            $appName = 'Dealer App';
        } elseif ($appType === 'ADMIN') {
            $appName = 'Admin App';
        } elseif ($appType === 'OWNER' || $appType === 'USER') {
            $appName = 'User App';
        }
        $existing = db_stmt_fetch_one(
            $con,
            'SELECT id, occurrences FROM `admin_crash_logs` WHERE `source_fingerprint`=? LIMIT 1',
            's',
            $fingerprint
        );
        $stack = $fields['original_stack_trace'];
        if ($stack === '' && $fields['original_error_message'] !== '') {
            $stack = $fields['original_error_message'];
        }
        if ($existing !== null) {
            db_stmt_execute(
                $con,
                'UPDATE `admin_crash_logs` SET
                    `occurrences` = `occurrences` + 1,
                    `error_title` = ?,
                    `error_class` = ?,
                    `device_name` = ?,
                    `app_version` = ?,
                    `user_name` = ?,
                    `stack_trace` = ?,
                    `updated_at` = ?
                 WHERE `source_fingerprint` = ?',
                'ssssssss',
                $title,
                $fields['original_exception_class'],
                $fields['device_name'],
                $fields['app_version'],
                $fields['user_label'] !== '' ? $fields['user_label'] : $fields['shop_name'],
                $stack,
                $now,
                $fingerprint
            );
        } else {
            db_stmt_insert_id(
                $con,
                'INSERT INTO `admin_crash_logs` (
                    `error_title`, `error_class`, `app_name`, `status`,
                    `device_name`, `android_version`, `app_version`,
                    `user_name`, `user_id`, `occurrences`, `stack_trace`, `source_fingerprint`,
                    `created_at`, `updated_at`
                 ) VALUES (?, ?, ?, \'New\', ?, \'\', ?, ?, ?, 1, ?, ?, ?, ?)',
                'sssssssssss',
                $title,
                $fields['original_exception_class'],
                $appName,
                $fields['device_name'],
                $fields['app_version'],
                $fields['user_label'] !== '' ? $fields['user_label'] : $fields['shop_name'],
                $fields['customer_id'],
                $stack,
                $fingerprint,
                $now,
                $now
            );
        }
    } catch (Throwable $e) {
        // Never fail POS ingest because the crash-list mirror failed.
    }
}
?>
