<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false');
admin_require_auth($con, $response);
admin_ensure_website_tables($con);

$contactId = isset($_POST['contactId']) ? (int) $_POST['contactId'] : 0;
$status = isset($_POST['status']) ? trim($_POST['status']) : '';
$allowed = array('New', 'Read', 'Replied', 'Closed');

if ($contactId <= 0 || !in_array($status, $allowed, true)) {
    $response['message'] = 'contactId and valid status required';
    echo json_encode($response);
    exit;
}

$ok = db_stmt_execute(
    $con,
    "UPDATE website_contact_messages SET status=?, updated_at=NOW() WHERE id=?",
    'si',
    $status,
    $contactId
);

if ($ok) {
    $response['status'] = 'true';
    $response['message'] = 'Status updated';
} else {
    $response['message'] = 'Update failed';
}

mysqli_close($con);
echo json_encode($response);
