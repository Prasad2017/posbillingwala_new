<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/fcm_tables.php';
require_once __DIR__ . '/db_prepared.php';

mysqli_query($con, 'set names utf8');
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

$licenceId = pos_require_auth($con, isset($_POST['userId']) ? $_POST['userId'] : '', $response);
$androidDeviceId = isset($_POST['android_device_id']) ? trim((string) $_POST['android_device_id']) : '';
$fcmToken = isset($_POST['fcm_token']) ? trim((string) $_POST['fcm_token']) : '';

if ($androidDeviceId === '') {
    $response['message'] = 'Device id required';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

fcm_ensure_schema($con);

$row = db_stmt_fetch_one(
    $con,
    "SELECT `id`, `android_device_id` FROM `licenses`
     WHERE `id`=? AND `licenseStatus`='active' LIMIT 1",
    'i',
    (int) $licenceId
);

if ($row === null) {
    $response['message'] = 'Licence not found';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$boundDevice = isset($row['android_device_id']) ? trim((string) $row['android_device_id']) : '';
if ($boundDevice !== '' && $boundDevice !== $androidDeviceId) {
    $response['message'] = 'Device mismatch';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($fcmToken === '') {
    db_stmt_execute(
        $con,
        "UPDATE `licenses` SET `fcm_token`=NULL, `fcm_token_updated_at`=NULL
         WHERE `id`=? AND (`android_device_id` IS NULL OR `android_device_id`='' OR `android_device_id`=?)",
        'is',
        (int) $licenceId,
        $androidDeviceId
    );
    $response['status'] = '1';
    $response['message'] = 'FCM token cleared';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$ok = db_stmt_execute(
    $con,
    "UPDATE `licenses` SET `fcm_token`=?, `fcm_token_updated_at`=NOW()
     WHERE `id`=? AND (`android_device_id` IS NULL OR `android_device_id`='' OR `android_device_id`=?)",
    'sis',
    $fcmToken,
    (int) $licenceId,
    $androidDeviceId
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'FCM token registered';
} else {
    $response['message'] = 'Unable to save FCM token';
}

echo json_encode($response);
mysqli_close($con);
?>
