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

$ticketId = isset($_POST['ticketId']) ? (int) $_POST['ticketId'] : 0;
$message = isset($_POST['message']) ? trim($_POST['message']) : '';
$sender = isset($_POST['sender']) ? trim($_POST['sender']) : 'Admin';
if ($ticketId <= 0 || $message === '') {
    $response['message'] = 'ticketId and message required';
    echo json_encode($response);
    exit;
}
$ok = db_stmt_execute(
    $con,
    "INSERT INTO admin_support_messages (ticket_id, sender, message) VALUES (?,?,?)",
    'iss',
    $ticketId,
    $sender,
    $message
);
if ($ok) {
    db_stmt_execute($con, "UPDATE admin_support_tickets SET updated_at=NOW(), status='Open' WHERE id=?", 'i', $ticketId);
    $response = array('status' => '1', 'message' => 'Reply sent');
}
mysqli_close($con);
echo json_encode($response);
