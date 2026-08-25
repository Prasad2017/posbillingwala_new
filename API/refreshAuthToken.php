<?php
/**
 * Silent token refresh for already-bound POS devices (offline-friendly).
 * Does NOT require MPIN — only licence key + matching android_device_id.
 * Use when a stored token expired but the device is still the licensed device.
 */
include_once __DIR__ . '/config.php';
include_once __DIR__ . '/licence_expiry.php';
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/db_prepared.php';

mysqli_query($con, 'set names utf8');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Refresh failed',
);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use Post Method';
    echo json_encode($response);
    exit;
}

$app_licence_key = isset($_POST['app_licence_key']) ? trim((string) $_POST['app_licence_key']) : '';
$android_device_id = isset($_POST['android_device_id']) ? trim((string) $_POST['android_device_id']) : '';
if ($android_device_id === '' && isset($_POST['androidId'])) {
    $android_device_id = trim((string) $_POST['androidId']);
}

if ($app_licence_key === '' || $android_device_id === '') {
    $response['message'] = 'licence key and device id required';
    echo json_encode($response);
    exit;
}

$check = db_stmt_fetch_one(
    $con,
    "SELECT `licenses`.* FROM `licenses`
     WHERE `licenseKey`=? AND `licenseStatus`='active' LIMIT 1",
    's',
    $app_licence_key
);

if ($check === null) {
    $response['message'] = 'Invalid or inactive licence';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$check = licence_sync_trial_consumed_state($con, $check);

if (!licence_trial_allows_login($con, $check) || !licence_enforce_expiry($con, $check)) {
    $response['message'] = 'licence key expired or user disable';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$boundDevice = isset($check['android_device_id']) ? trim((string) $check['android_device_id']) : '';
if ($boundDevice === '' || $boundDevice !== $android_device_id) {
    $response['message'] = 'Device not authorized for this licence';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

// Revoke previous token from this request if present (rotation)
$oldToken = auth_token_from_request();
if ($oldToken !== null && $oldToken !== '') {
    auth_token_revoke($con, $oldToken);
}

$response['status'] = '1';
$response['message'] = 'Token refreshed';
$response['licenceId'] = (string) $check['id'];
$response['ownerId'] = (string) $check['userId'];
auth_token_append_response($con, $response, 'pos_licence', $check['id'], $android_device_id, isset($check['expiryDate']) ? $check['expiryDate'] : null);

mysqli_close($con);
echo json_encode($response);
?>
