<?php
/**
 * Owner: list mess member payments.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

owner_require_auth($con);
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request',
    'memberResponse' => array(),
);

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null) {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$allowed = mess_remote_resolve_licence_ids_for_owner($con, $userId);
$licenceId = isset($_GET['licenceId']) ? (int) $_GET['licenceId'] : 0;
$memberId = isset($_GET['memberId']) ? (int) $_GET['memberId'] : 0;

if ($licenceId > 0) {
    if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
        $response['message'] = 'Licence not found for this owner';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    $licenceIds = array($licenceId);
} else {
    $licenceIds = $allowed;
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['memberResponse'] = mess_remote_list_payments(
    $con,
    $licenceIds,
    $memberId > 0 ? $memberId : null
);
echo json_encode($response);
mysqli_close($con);
