<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'tickets' => array());
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);

$status = isset($_GET['status']) ? strtolower(trim($_GET['status'])) : 'all';
$sql = "SELECT * FROM admin_support_tickets";
$types = '';
$params = array();
if ($status !== 'all' && $status !== '') {
    $sql .= " WHERE LOWER(status)=?";
    $types = 's';
    $params[] = $status;
}
$sql .= " ORDER BY id DESC LIMIT 200";
$rows = $types === '' ? db_stmt_fetch_all($con, $sql, '') : db_stmt_fetch_all($con, $sql, $types, $params[0]);
foreach ($rows as $r) {
    $response['tickets'][] = support_format_ticket($r);
}
mysqli_close($con);
echo json_encode($response);
