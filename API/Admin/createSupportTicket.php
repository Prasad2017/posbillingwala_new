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

$app = isset($_POST['appName']) ? trim($_POST['appName']) : 'POS App';
$category = isset($_POST['category']) ? trim($_POST['category']) : 'General';
$subject = isset($_POST['subject']) ? trim($_POST['subject']) : '';
$description = isset($_POST['description']) ? trim($_POST['description']) : '';
if ($subject === '') {
    $response['message'] = 'Subject required';
    echo json_encode($response);
    exit;
}

$ticketNo = 'TKT-' . date('Ymd') . '-' . str_pad((string) random_int(1, 9999), 4, '0', STR_PAD_LEFT);
$ok = db_stmt_execute(
    $con,
    "INSERT INTO admin_support_tickets (ticket_no, app_name, category, subject, description, status)
     VALUES (?,?,?,?,?,'Open')",
    'sssss',
    $ticketNo,
    $app,
    $category,
    $subject,
    $description
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
    $response = array('status' => '1', 'message' => 'Ticket created', 'ticketId' => (string) $id, 'ticketNo' => $ticketNo);
} else {
    $response['message'] = 'Unable to create ticket';
}
mysqli_close($con);
echo json_encode($response);
