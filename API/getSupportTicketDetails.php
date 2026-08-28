<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/Admin/admin_tables.php';
require_once __DIR__ . '/support_helpers.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false', 'messages' => array());

$licenceId = pos_require_auth($con, isset($_GET['userId']) ? $_GET['userId'] : '', array('status' => 'false', 'message' => 'Unauthorized'));
admin_ensure_support_crash_tables($con);

$ticketId = isset($_GET['ticketId']) ? (int) $_GET['ticketId'] : 0;
if ($ticketId <= 0) {
    $response['message'] = 'ticketId required';
    echo json_encode($response);
    exit;
}
if (!support_ticket_owned_by_licence($con, $ticketId, $licenceId)) {
    $response['message'] = 'Not found';
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
