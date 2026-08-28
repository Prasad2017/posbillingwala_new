<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/Admin/admin_tables.php';
require_once __DIR__ . '/support_helpers.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'tickets' => array());

$licenceId = pos_require_auth($con, isset($_GET['userId']) ? $_GET['userId'] : '', array('status' => 'false', 'message' => 'Unauthorized'));
admin_ensure_support_crash_tables($con);

$status = isset($_GET['status']) ? strtolower(trim($_GET['status'])) : 'all';
$sql = "SELECT * FROM admin_support_tickets WHERE licence_id=?";
$types = 'i';
$params = array((int) $licenceId);
if ($status !== 'all' && $status !== '') {
    $sql .= " AND LOWER(status)=?";
    $types .= 's';
    $params[] = $status;
}
$sql .= " ORDER BY id DESC LIMIT 100";

if ($types === 'i') {
    $rows = db_stmt_fetch_all($con, $sql, $types, $params[0]);
} else {
    $rows = db_stmt_fetch_all($con, $sql, $types, $params[0], $params[1]);
}

foreach ($rows as $r) {
    $response['tickets'][] = support_format_ticket($r);
}
mysqli_close($con);
echo json_encode($response);
