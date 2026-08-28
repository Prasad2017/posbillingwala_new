<?php
/**
 * Admin: Crash & Error Log list (summary cards).
 * GET optional: severity, errorType, customerId, limit, offset
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../log_sanitizer.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '1', 'errorLogList' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => '0', 'errorLogList' => array()));
mysqli_query($con, 'set names utf8');

$severity = isset($_GET['severity']) ? trim($_GET['severity']) : '';
$errorType = isset($_GET['errorType']) ? trim($_GET['errorType']) : '';
$customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
$limit = isset($_GET['limit']) ? max(1, min(200, (int) $_GET['limit'])) : 100;
$offset = isset($_GET['offset']) ? max(0, (int) $_GET['offset']) : 0;

$where = array('1=1');
$types = '';
$params = array();

if ($severity !== '') {
    $where[] = '`severity`=?';
    $types .= 's';
    $params[] = $severity;
}
if ($errorType !== '') {
    $where[] = '`error_type`=?';
    $types .= 's';
    $params[] = $errorType;
}
if ($customerId !== '') {
    $where[] = '`customer_id`=?';
    $types .= 's';
    $params[] = $customerId;
}

$sql = 'SELECT `id`, `fingerprint`, `occurrence_count`, `first_seen_at`, `last_seen_at`,
               `error_type`, `severity`, `error_category`, `summary`,
               `app_type`, `app_version`, `customer_id`, `shop_name`, `branch_label`,
               `device_name`, `screen_name`, `user_action`,
               `api_method`, `api_url`, `http_status`,
               `original_exception_class`, `original_error_code`
        FROM `error_logs`
        WHERE ' . implode(' AND ', $where) . '
        ORDER BY `last_seen_at` DESC
        LIMIT ' . (int) $limit . ' OFFSET ' . (int) $offset;

if ($types !== '') {
    $rows = call_user_func_array('db_stmt_fetch_all', array_merge(array($con, $sql, $types), $params));
} else {
    $rows = db_stmt_fetch_all($con, $sql, '');
}

foreach ($rows as $row) {
    $apiPath = '';
    if (!empty($row['api_url'])) {
        $path = parse_url($row['api_url'], PHP_URL_PATH);
        $apiPath = $path !== null ? (string) $path : (string) $row['api_url'];
        if (strlen($apiPath) > 80) {
            $apiPath = substr($apiPath, -80);
        }
    }
    $methodPath = '';
    if (!empty($row['api_method']) || $apiPath !== '') {
        $methodPath = trim(($row['api_method'] ? $row['api_method'] : '') . ' ' . $apiPath);
    }

    $response['errorLogList'][] = array(
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
        'screenName' => (string) $row['screen_name'],
        'userAction' => log_sanitize_text($row['user_action'], 512),
        'apiMethodPath' => $methodPath,
        'httpStatus' => $row['http_status'] !== null ? (string) $row['http_status'] : '',
        'originalExceptionClass' => (string) $row['original_exception_class'],
        'originalErrorCode' => (string) $row['original_error_code'],
    );
}

echo json_encode($response);
?>
