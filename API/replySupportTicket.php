<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/Admin/admin_tables.php';
require_once __DIR__ . '/support_helpers.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

$licenceId = pos_require_auth($con, isset($_POST['userId']) ? $_POST['userId'] : '', array('status' => '0', 'message' => 'Unauthorized'));
admin_ensure_support_crash_tables($con);

$ticketId = isset($_POST['ticketId']) ? (int) $_POST['ticketId'] : 0;
$message = isset($_POST['message']) ? trim($_POST['message']) : '';
if ($ticketId <= 0 || $message === '') {
    $response['message'] = 'ticketId and message required';
    echo json_encode($response);
    exit;
}
if (!support_ticket_owned_by_licence($con, $ticketId, $licenceId)) {
    $response['message'] = 'Not found';
    echo json_encode($response);
    exit;
}
$statusRow = db_stmt_fetch_one($con, "SELECT status FROM admin_support_tickets WHERE id=? LIMIT 1", 'i', $ticketId);
if ($statusRow !== null && in_array(strtolower((string) $statusRow['status']), array('closed', 'resolved'), true)) {
    $response['message'] = 'Ticket is closed';
    echo json_encode($response);
    exit;
}
$ok = db_stmt_execute(
    $con,
    "INSERT INTO admin_support_messages (ticket_id, sender, message) VALUES (?,'You',?)",
    'is',
    $ticketId,
    $message
);
if ($ok) {
    db_stmt_execute($con, "UPDATE admin_support_tickets SET updated_at=NOW(), status='Open' WHERE id=?", 'i', $ticketId);
    $response = array('status' => '1', 'message' => 'Reply sent');
}
mysqli_close($con);
echo json_encode($response);
