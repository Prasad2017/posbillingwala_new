<?php
/**
 * Shared FCM register for Owner / Dealer / Admin apps.
 * Expects $appType to be set by the including script before require.
 */
include_once(__DIR__ . '/config.php');
require_once __DIR__ . '/fcm_tables.php';
require_once __DIR__ . '/db_prepared.php';

mysqli_query($con, 'set names utf8mb4');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if (!isset($appType) || !in_array($appType, array('owner', 'dealer', 'admin'), true)) {
    $response['message'] = 'Invalid app type';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$deviceId = isset($_POST['android_device_id']) ? trim((string) $_POST['android_device_id']) : '';
$fcmToken = isset($_POST['fcm_token']) ? trim((string) $_POST['fcm_token']) : '';

if ($appType === 'owner') {
    require_once __DIR__ . '/Owner/auth_guard.php';
    owner_require_auth($con, $response);
    $resolved = owner_resolve_user_id($con, $userId);
    if ($resolved === null) {
        $response['message'] = 'Invalid or expired auth token';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    $userId = $resolved;
} elseif ($appType === 'dealer') {
    require_once __DIR__ . '/Dealer/auth_guard.php';
    dealer_require_auth($con, $response);
    require_once __DIR__ . '/auth_tokens.php';
    $resolved = auth_user_id_from_request($con, $userId, 'dealer');
    if ($resolved === null) {
        $response['message'] = 'Invalid or expired auth token';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    $userId = $resolved;
} else {
    require_once __DIR__ . '/Admin/auth_guard.php';
    admin_require_auth($con, $response);
    require_once __DIR__ . '/auth_tokens.php';
    $resolved = auth_user_id_from_request($con, $userId, 'admin');
    if ($resolved === null) {
        $response['message'] = 'Invalid or expired auth token';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    $userId = $resolved;
}

if ($deviceId === '') {
    $response['message'] = 'Device id required';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$ok = fcm_device_upsert($con, $appType, (int) $userId, $deviceId, $fcmToken);
if ($ok) {
    $response['status'] = '1';
    $response['message'] = ($fcmToken === '') ? 'FCM token cleared' : 'FCM token registered';
} else {
    $response['message'] = 'Unable to save FCM token';
}

echo json_encode($response);
mysqli_close($con);
