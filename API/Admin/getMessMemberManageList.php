<?php
/**
 * Admin: list mess members for a customer licence.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

admin_require_auth($con);
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request',
    'memberResponse' => array(),
);

$adminId = isset($_GET['userId']) ? $_GET['userId'] : '';
$adminId = auth_user_id_from_request($con, $adminId, 'admin');
if ($adminId === null) {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$customerId = isset($_GET['customerId']) ? (int) $_GET['customerId'] : 0;
$licenceId = isset($_GET['licenceId']) ? (int) $_GET['licenceId'] : 0;
$allowed = mess_remote_resolve_licence_ids_for_admin_customer($con, $customerId);
if ($allowed === null) {
    $response['message'] = 'Customer not found';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($licenceId > 0) {
    if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
        $response['message'] = 'Licence not found for this customer';
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
$response['memberResponse'] = mess_remote_list_members($con, $licenceIds);
echo json_encode($response);
mysqli_close($con);
