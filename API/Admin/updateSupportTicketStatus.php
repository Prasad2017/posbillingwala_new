<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';
require_once __DIR__ . '/../support_helpers.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);

$ticketId = isset($_POST['ticketId']) ? (int) $_POST['ticketId'] : 0;
$status = isset($_POST['status']) ? trim($_POST['status']) : '';
$allowed = array('Open', 'Closed', 'Resolved');
if ($ticketId <= 0 || !in_array($status, $allowed, true)) {
    $response['message'] = 'ticketId and valid status required';
    echo json_encode($response);
    exit;
}
$ok = db_stmt_execute(
    $con,
    "UPDATE admin_support_tickets SET status=?, updated_at=NOW() WHERE id=?",
    'si',
    $status,
    $ticketId
);
if ($ok) {
    $response = array('status' => '1', 'message' => 'Status updated');
}
mysqli_close($con);
echo json_encode($response);
