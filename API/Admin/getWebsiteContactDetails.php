<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false');
admin_require_auth($con, $response);
admin_ensure_website_tables($con);

$contactId = isset($_GET['contactId']) ? (int) $_GET['contactId'] : 0;
if ($contactId <= 0) {
    $response['message'] = 'contactId required';
    echo json_encode($response);
    exit;
}

$row = db_stmt_fetch_one($con, "SELECT * FROM website_contact_messages WHERE id=? LIMIT 1", 'i', $contactId);
if ($row === null) {
    $response['message'] = 'Not found';
    echo json_encode($response);
    exit;
}

if (strcasecmp((string) $row['status'], 'New') === 0) {
    db_stmt_execute($con, "UPDATE website_contact_messages SET status='Read', updated_at=NOW() WHERE id=?", 'i', $contactId);
    $row['status'] = 'Read';
}

$response = array_merge(array('status' => 'true'), website_format_contact($row));
mysqli_close($con);
echo json_encode($response);
