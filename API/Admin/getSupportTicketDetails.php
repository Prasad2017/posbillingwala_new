<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';
require_once __DIR__ . '/../support_helpers.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false', 'messages' => array());
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);

$ticketId = isset($_GET['ticketId']) ? (int) $_GET['ticketId'] : 0;
if ($ticketId <= 0) {
    $response['message'] = 'ticketId required';
    echo json_encode($response);
    exit;
}
$t = db_stmt_fetch_one($con, "SELECT * FROM admin_support_tickets WHERE id=? LIMIT 1", 'i', $ticketId);
if ($t === null) {
    $response['message'] = 'Not found';
    echo json_encode($response);
    exit;
}
$formatted = support_format_ticket($t);
unset($formatted['status']);
$response = array_merge(array('status' => 'true'), $formatted, array(
    'messages' => support_format_messages($con, $ticketId),
));
mysqli_close($con);
echo json_encode($response);
