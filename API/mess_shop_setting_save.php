<?php
/**
 * POST: save mess shop setting (payer mode: user | institute).
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request',
    'payerMode' => 'user',
);

mess_common_ensure_schema($con);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$licenceId = pos_require_auth($con, $userId, $response);
require_once __DIR__ . '/pos_staff.php';
pos_require_permission($con, $licenceId, 'mess.manage');

$mode = isset($_POST['payerMode']) ? $_POST['payerMode'] : 'user';
$mode = mess_normalize_payer_mode($mode);
$uid = (int) $licenceId;

if (!mess_set_payer_mode($con, $uid, $mode)) {
    $err = mysqli_error($con);
    error_log('mess_shop_setting_save FAIL userId=' . $uid . ' mode=' . $mode . ' err=' . $err);
    $response['message'] = 'Unable to save setting' . ($err !== '' ? (': ' . $err) : '');
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

error_log('mess_shop_setting_save OK userId=' . $uid . ' mode=' . $mode);
$response['status'] = '1';
$response['message'] = 'ok';
$response['payerMode'] = $mode;
echo json_encode($response);
mysqli_close($con);
