<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request');
mess_common_ensure_schema($con);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
pos_require_auth($con, $userId, $response);

$printDeviceId = isset($_POST['android_device_id']) ? trim((string) $_POST['android_device_id']) : '';
$messLabel = isset($_POST['messLabel']) ? trim((string) $_POST['messLabel']) : '';
$branchLabel = isset($_POST['branchLabel']) ? trim((string) $_POST['branchLabel']) : '';

db_stmt_execute(
    $con,
    "UPDATE mess_qr SET status = 'INACTIVE', deactivated_at = NOW() WHERE userId = ? AND status = 'ACTIVE'",
    'i',
    (int) $userId
);

$publicToken = mess_random_token(24);
$ok = db_stmt_execute(
    $con,
    'INSERT INTO mess_qr (userId, public_token, status, print_device_id, mess_label, branch_label)
     VALUES (?, ?, \'ACTIVE\', ?, ?, ?)',
    'issss',
    (int) $userId,
    $publicToken,
    $printDeviceId,
    $messLabel,
    $branchLabel
);

if (!$ok) {
    $response['message'] = 'Unable to regenerate QR right now.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

mess_audit($con, $userId, 'qr_regenerated', $publicToken, null, null, 'old invalidated');

$response['status'] = '1';
$response['message'] = 'regenerated';
$response['qr'] = array(
    'publicToken' => $publicToken,
    'status' => 'ACTIVE',
    'qrUrl' => mess_public_qr_url($publicToken),
    'messLabel' => $messLabel,
    'branchLabel' => $branchLabel,
    'printDeviceId' => $printDeviceId,
    'createdAt' => date('Y-m-d H:i:s'),
);

echo json_encode($response);
mysqli_close($con);
