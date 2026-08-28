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

$category = isset($_POST['category']) ? trim($_POST['category']) : 'General';
$subject = isset($_POST['subject']) ? trim($_POST['subject']) : '';
$description = isset($_POST['description']) ? trim($_POST['description']) : '';
if ($subject === '') {
    $response['message'] = 'Subject required';
    echo json_encode($response);
    exit;
}

$ctx = support_fetch_licence_context($con, $licenceId);
$ticketNo = support_generate_ticket_no();
$ok = db_stmt_execute(
    $con,
    "INSERT INTO admin_support_tickets
     (ticket_no, app_name, category, subject, description, status, licence_id, user_id, shop_name, device_name, device_id)
     VALUES (?,?,?,?,?,'Open',?,?,?,?,?)",
    'sssssiisss',
    $ticketNo,
    'POS App',
    $category,
    $subject,
    $description,
    (int) $ctx['licence_id'],
    (int) $ctx['user_id'],
    $ctx['shop_name'],
    $ctx['device_name'],
    $ctx['device_id']
);
if ($ok) {
    $id = (int) mysqli_insert_id($con);
    if ($description !== '') {
        db_stmt_execute(
            $con,
            "INSERT INTO admin_support_messages (ticket_id, sender, message) VALUES (?,'You',?)",
            'is',
            $id,
            $description
        );
    }
    $response = array(
        'status' => '1',
        'message' => 'Ticket created',
        'ticketId' => (string) $id,
        'ticketNo' => $ticketNo,
    );
} else {
    $response['message'] = 'Unable to create ticket';
}
mysqli_close($con);
echo json_encode($response);
