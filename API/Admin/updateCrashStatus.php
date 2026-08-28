<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'failed');
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);
$id = isset($_POST['crashId']) ? (int) $_POST['crashId'] : 0;
$status = isset($_POST['status']) ? trim($_POST['status']) : 'Resolved';
if ($id <= 0) {
    $response['message'] = 'crashId required';
    echo json_encode($response);
    exit;
}
$ok = db_stmt_execute($con, "UPDATE admin_crash_logs SET status=? WHERE id=?", 'si', $status, $id);
$response = $ok
    ? array('status' => '1', 'message' => 'Updated')
    : array('status' => '0', 'message' => 'Update failed');
mysqli_close($con);
echo json_encode($response);
