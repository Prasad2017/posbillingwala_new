<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false');
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);

$id = isset($_GET['crashId']) ? (int) $_GET['crashId'] : 0;
if ($id <= 0) {
    $response['message'] = 'crashId required';
    echo json_encode($response);
    exit;
}
$r = db_stmt_fetch_one($con, "SELECT * FROM admin_crash_logs WHERE id=? LIMIT 1", 'i', $id);
if ($r === null) {
    $response['message'] = 'Not found';
    echo json_encode($response);
    exit;
}
$response = array(
    'status' => 'true',
    'id' => (string) $r['id'],
    'errorTitle' => (string) $r['error_title'],
    'errorClass' => (string) $r['error_class'],
    'appName' => (string) $r['app_name'],
    'status' => (string) $r['status'],
    'deviceName' => (string) $r['device_name'],
    'androidVersion' => (string) $r['android_version'],
    'appVersion' => (string) $r['app_version'],
    'userName' => (string) $r['user_name'],
    'userId' => (string) $r['user_id'],
    'occurrences' => (string) $r['occurrences'],
    'stackTrace' => (string) $r['stack_trace'],
    'createdAt' => (string) $r['created_at']
);
mysqli_close($con);
echo json_encode($response);
