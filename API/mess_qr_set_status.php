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

$status = isset($_POST['status']) ? strtoupper(trim((string) $_POST['status'])) : '';
if ($status !== 'ACTIVE' && $status !== 'INACTIVE') {
    $response['message'] = 'Invalid status';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($status === 'INACTIVE') {
    db_stmt_execute(
        $con,
        "UPDATE mess_qr SET status = 'INACTIVE', deactivated_at = NOW() WHERE userId = ? AND status = 'ACTIVE'",
        'i',
        (int) $userId
    );
    mess_audit($con, $userId, 'qr_deactivated', null, null, null, null);
    $response['status'] = '1';
    $response['message'] = 'deactivated';
    $response['qrStatus'] = 'INACTIVE';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

// Reactivate latest QR for this shop
$latest = db_stmt_fetch_one(
    $con,
    'SELECT * FROM mess_qr WHERE userId = ? ORDER BY id DESC LIMIT 1',
    'i',
    (int) $userId
);

if ($latest === null) {
    $response['message'] = 'No QR found. Generate first.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

db_stmt_execute(
    $con,
    "UPDATE mess_qr SET status = 'INACTIVE', deactivated_at = NOW() WHERE userId = ? AND status = 'ACTIVE' AND id != ?",
    'ii',
    (int) $userId,
    (int) $latest['id']
);
db_stmt_execute(
    $con,
    "UPDATE mess_qr SET status = 'ACTIVE', deactivated_at = NULL WHERE id = ?",
    'i',
    (int) $latest['id']
);

$printDeviceId = isset($_POST['android_device_id']) ? trim((string) $_POST['android_device_id']) : '';
if ($printDeviceId !== '') {
    db_stmt_execute(
        $con,
        'UPDATE mess_qr SET print_device_id = ? WHERE id = ?',
        'si',
        $printDeviceId,
        (int) $latest['id']
    );
}

mess_audit($con, $userId, 'qr_activated', $latest['public_token'], null, null, null);

$response['status'] = '1';
$response['message'] = 'activated';
$response['qrStatus'] = 'ACTIVE';
$response['qr'] = array(
    'publicToken' => $latest['public_token'],
    'status' => 'ACTIVE',
    'qrUrl' => mess_public_qr_url($latest['public_token']),
    'messLabel' => $latest['mess_label'],
    'branchLabel' => $latest['branch_label'],
    'printDeviceId' => $printDeviceId !== '' ? $printDeviceId : $latest['print_device_id'],
);

echo json_encode($response);
mysqli_close($con);
