<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request');
mess_common_ensure_schema($con);
date_default_timezone_set('Asia/Kolkata');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
pos_require_auth($con, $userId, $response);

$publicId = isset($_POST['tokenId']) ? trim((string) $_POST['tokenId']) : '';
$result = isset($_POST['result']) ? strtoupper(trim((string) $_POST['result'])) : '';
$deviceId = isset($_POST['android_device_id']) ? trim((string) $_POST['android_device_id']) : '';

if ($publicId === '' || ($result !== 'SUCCESS' && $result !== 'FAILED')) {
    $response['message'] = 'Missing fields';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$token = db_stmt_fetch_one(
    $con,
    'SELECT * FROM mess_meal_token WHERE public_id = ? AND userId = ? LIMIT 1',
    'si',
    $publicId,
    (int) $userId
);

if ($token === null) {
    $response['message'] = 'Token not found';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($result === 'SUCCESS') {
    if ($token['print_status'] === 'PRINTED') {
        $response['status'] = '1';
        $response['message'] = 'already printed';
        $response['printStatus'] = 'PRINTED';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    db_stmt_execute(
        $con,
        "UPDATE mess_meal_token SET print_status = 'PRINTED', printed_at = NOW(), print_device_id = IF(? = '', print_device_id, ?) WHERE public_id = ? AND userId = ?",
        'sssi',
        $deviceId,
        $deviceId,
        $publicId,
        (int) $userId
    );
    mess_audit($con, $userId, 'print_success', null, $token['registration_no'], $publicId, null);
    $response['status'] = '1';
    $response['message'] = 'printed';
    $response['printStatus'] = 'PRINTED';
} else {
    if ($token['print_status'] === 'PRINTED') {
        $response['status'] = '1';
        $response['message'] = 'already printed';
        $response['printStatus'] = 'PRINTED';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    db_stmt_execute(
        $con,
        "UPDATE mess_meal_token SET print_status = 'PRINT_FAILED' WHERE public_id = ? AND userId = ? AND print_status != 'PRINTED'",
        'si',
        $publicId,
        (int) $userId
    );
    mess_audit($con, $userId, 'print_failed', null, $token['registration_no'], $publicId, null);
    $response['status'] = '1';
    $response['message'] = 'print failed recorded';
    $response['printStatus'] = 'PRINT_FAILED';
}

echo json_encode($response);
mysqli_close($con);
