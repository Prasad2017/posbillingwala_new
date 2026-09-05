<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request');
mess_common_ensure_schema($con);

$userId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
pos_require_auth($con, $userId, $response);
mess_ensure_default_sessions($con, (int) $userId);

$row = db_stmt_fetch_one(
    $con,
    "SELECT * FROM mess_qr WHERE userId = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1",
    'i',
    (int) $userId
);

$response['status'] = '1';
$response['message'] = $row ? 'ok' : 'no active qr';
$response['hasQr'] = $row ? '1' : '0';

if ($row) {
    $response['qr'] = array(
        'publicToken' => $row['public_token'],
        'status' => $row['status'],
        'qrUrl' => mess_public_qr_url($row['public_token']),
        'messLabel' => $row['mess_label'],
        'branchLabel' => $row['branch_label'],
        'printDeviceId' => $row['print_device_id'],
        'createdAt' => $row['created_at'],
    );
} else {
    $response['qr'] = null;
}

echo json_encode($response);
mysqli_close($con);
