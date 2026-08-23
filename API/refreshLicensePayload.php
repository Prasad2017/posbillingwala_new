<?php
include_once('config.php');
include_once('licence_expiry.php');
require_once __DIR__ . '/licence_payload.php';
require_once __DIR__ . '/auth_tokens.php';

mysqli_query($con, 'set names utf8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Content-Type: application/json');

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $licenceId = isset($_POST['userId']) ? trim($_POST['userId']) : '';
    $android_device_id = isset($_POST['android_device_id']) ? trim($_POST['android_device_id']) : '';

    $licenceId = auth_pos_licence_id_from_request($con, $licenceId);
    if ($licenceId === null) {
        $response['message'] = 'Invalid or expired auth token';
        echo json_encode($response);
        exit;
    }

    if ($licenceId !== '' && $android_device_id !== '') {
        $check = db_stmt_fetch_one(
            $con,
            "SELECT `licenses`.*, `users`.`shopName`, `users`.`shopImage`, `users`.`reportPin`, `users`.`is_active` AS userActive
             FROM `licenses`
             LEFT JOIN `users` ON `users`.`id` = `licenses`.`userId`
             WHERE `licenses`.`id`=? AND `licenses`.`android_device_id`=? AND `licenses`.`licenseStatus`='active'",
            'ss',
            $licenceId,
            $android_device_id
        );

        if ($check !== null && !empty($check['userActive']) && $check['userActive'] === '1') {
            $check = licence_sync_trial_consumed_state($con, $check);

            if (!licence_trial_allows_login($con, $check)) {
                $response['message'] = 'Trial already used. Please upgrade your licence.';
            } elseif (!licence_enforce_expiry($con, $check)) {
                $response['message'] = 'Licence expired';
            } else {
                $response['status'] = '1';
                $response['message'] = 'License payload refreshed.';
                $response['licenceId'] = $check['id'];
                $response['ownerId'] = $check['userId'];
                $response['licence_key_expire_date'] = $check['expiryDate'];
                $response = licence_append_trial_response($con, $response, $check);
                $response = licence_append_signed_payload($con, $response, $check, $android_device_id);
                auth_token_append_response($con, $response, 'pos_licence', $check['id'], $android_device_id);
            }
        } else {
            $response['message'] = 'Licence not found or device mismatch';
        }
    } else {
        $response['message'] = 'Missing userId or android_device_id';
    }
} else {
    $response['message'] = 'Use Post Method';
}

mysqli_close($con);
echo json_encode($response);
?>
