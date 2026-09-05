<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization');
header('Access-Control-Allow-Methods: POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

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

$existing = db_stmt_fetch_one(
    $con,
    "SELECT * FROM mess_qr WHERE userId = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1",
    'i',
    (int) $userId
);

if ($existing) {
    if ($printDeviceId !== '') {
        db_stmt_execute(
            $con,
            "UPDATE mess_qr SET print_device_id = ?, mess_label = IF(? = '', mess_label, ?), branch_label = IF(? = '', branch_label, ?) WHERE id = ?",
            'sssssi',
            $printDeviceId,
            $messLabel,
            $messLabel,
            $branchLabel,
            $branchLabel,
            (int) $existing['id']
        );
        $existing['print_device_id'] = $printDeviceId;
    }
    mess_audit($con, $userId, 'qr_get_existing', $existing['public_token'], null, null, 'already active');
    $response['status'] = '1';
    $response['message'] = 'already active';
    $response['qr'] = array(
        'publicToken' => $existing['public_token'],
        'status' => 'ACTIVE',
        'qrUrl' => mess_public_qr_url($existing['public_token']),
        'messLabel' => $messLabel !== '' ? $messLabel : $existing['mess_label'],
        'branchLabel' => $branchLabel !== '' ? $branchLabel : $existing['branch_label'],
        'printDeviceId' => $existing['print_device_id'],
        'createdAt' => $existing['created_at'],
    );
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

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
    $response['message'] = 'Unable to generate QR right now.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

mess_ensure_default_sessions($con, (int) $userId);
mess_audit($con, $userId, 'qr_generated', $publicToken, null, null, null);

$response['status'] = '1';
$response['message'] = 'generated';
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
