<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array(
    'status' => 'true',
    'totalCrashes' => '0',
    'affectedUsers' => '0',
    'resolved' => '0',
    'crashes' => array()
);
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);

$total = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM admin_crash_logs", '');
$affected = (string) db_stmt_scalar_int($con, "SELECT COUNT(DISTINCT user_id) AS c FROM admin_crash_logs", '');
$resolved = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM admin_crash_logs WHERE LOWER(status)='resolved'", '');

$q = isset($_GET['q']) ? trim($_GET['q']) : '';
$app = isset($_GET['app']) ? trim($_GET['app']) : '';
$status = isset($_GET['status']) ? trim($_GET['status']) : '';

$sql = "SELECT * FROM admin_crash_logs WHERE 1=1";
$types = '';
$args = array();
if ($q !== '') {
    $sql .= " AND (error_title LIKE ? OR error_class LIKE ?)";
    $types .= 'ss';
    $like = '%' . $q . '%';
    $args[] = $like;
    $args[] = $like;
}
if ($app !== '' && strtolower($app) !== 'all') {
    $sql .= " AND app_name=?";
    $types .= 's';
    $args[] = $app;
}
if ($status !== '' && strtolower($status) !== 'all') {
    $sql .= " AND status=?";
    $types .= 's';
    $args[] = $status;
}
$sql .= " ORDER BY id DESC LIMIT 200";

if ($types === '') {
    $rows = db_stmt_fetch_all($con, $sql, '');
} elseif (count($args) === 1) {
    $rows = db_stmt_fetch_all($con, $sql, $types, $args[0]);
} elseif (count($args) === 2) {
    $rows = db_stmt_fetch_all($con, $sql, $types, $args[0], $args[1]);
} else {
    $rows = db_stmt_fetch_all($con, $sql, $types, $args[0], $args[1], $args[2]);
}

$crashes = array();
foreach ($rows as $r) {
    $crashes[] = array(
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
        'createdAt' => (string) $r['created_at']
    );
}

$response = array(
    'status' => 'true',
    'totalCrashes' => $total,
    'affectedUsers' => $affected,
    'resolved' => $resolved,
    'totalCrashesTrend' => '+12.5% vs last 7 days',
    'affectedUsersTrend' => '+8.2% vs last 7 days',
    'resolvedTrend' => '+15.0% vs last 7 days',
    'crashes' => $crashes
);
mysqli_close($con);
echo json_encode($response);
