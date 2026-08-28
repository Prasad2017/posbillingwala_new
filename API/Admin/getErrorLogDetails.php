<?php
/**
 * Admin: Crash & Error Log detail (full debug context).
 * GET required: id
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../log_sanitizer.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'errorLogDetail' => null);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => '0', 'errorLogDetail' => null));
mysqli_query($con, 'set names utf8');

$id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
if ($id <= 0) {
    $response['message'] = 'Missing id';
    echo json_encode($response);
    exit;
}

$row = db_stmt_fetch_one($con, 'SELECT * FROM `error_logs` WHERE `id`=? LIMIT 1', 'i', $id);
if ($row === null) {
    $response['message'] = 'Not found';
    echo json_encode($response);
    exit;
}

$maxBody = 20480;
$response['status'] = '1';
$response['errorLogDetail'] = array(
    'id' => (string) $row['id'],
    'fingerprint' => (string) $row['fingerprint'],
    'occurrenceCount' => (string) $row['occurrence_count'],
    'firstSeenAt' => (string) $row['first_seen_at'],
    'lastSeenAt' => (string) $row['last_seen_at'],
    'errorType' => (string) $row['error_type'],
    'severity' => (string) $row['severity'],
    'errorCategory' => (string) $row['error_category'],
    'summary' => log_sanitize_text($row['summary'], 512),
    'appType' => (string) $row['app_type'],
    'appVersion' => (string) $row['app_version'],
    'customerId' => (string) $row['customer_id'],
    'shopName' => (string) $row['shop_name'],
    'branchLabel' => (string) $row['branch_label'],
    'deviceName' => (string) $row['device_name'],
    'deviceId' => (string) $row['device_id'],
    'userLabel' => (string) $row['user_label'],
    'screenName' => (string) $row['screen_name'],
    'activityName' => (string) $row['activity_name'],
    'fragmentName' => (string) $row['fragment_name'],
    'userAction' => log_sanitize_text($row['user_action'], 512),
    'whatHappened' => log_sanitize_text($row['what_happened'], 4096),
    'userFlow' => log_sanitize_text($row['user_flow'], 8192),
    'breadcrumbs' => log_sanitize_text($row['breadcrumbs'], 8192),
    'apiMethod' => (string) $row['api_method'],
    'apiUrl' => log_sanitize_text($row['api_url'], 1024),
    'httpStatus' => $row['http_status'] !== null ? (string) $row['http_status'] : '',
    'requestBody' => log_sanitize_text($row['request_body'], $maxBody),
    'responseBody' => log_sanitize_text($row['response_body'], $maxBody),
    'requestSize' => $row['request_size'] !== null ? (string) $row['request_size'] : '',
    'responseSize' => $row['response_size'] !== null ? (string) $row['response_size'] : '',
    'requestDurationMs' => $row['request_duration_ms'] !== null ? (string) $row['request_duration_ms'] : '',
    'printerType' => (string) $row['printer_type'],
    'printerModel' => (string) $row['printer_model'],
    'printerConnection' => (string) $row['printer_connection'],
    'printOperation' => (string) $row['print_operation'],
    'originalErrorMessage' => log_sanitize_text($row['original_error_message'], $maxBody),
    'originalExceptionClass' => (string) $row['original_exception_class'],
    'originalStackTrace' => log_sanitize_text($row['original_stack_trace'], $maxBody),
    'originalErrorCode' => (string) $row['original_error_code'],
    'originalApiResponse' => log_sanitize_text($row['original_api_response'], $maxBody),
    'resolutionNotes' => (string) $row['resolution_notes'],
    'resolvedAt' => (string) $row['resolved_at'],
    'resolvedBy' => (string) $row['resolved_by'],
);

echo json_encode($response);
?>
