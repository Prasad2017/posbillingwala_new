<?php
/**
 * Admin: create/update mess member for customer licence.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

admin_require_auth($con);
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$adminId = isset($_POST['userId']) ? $_POST['userId'] : '';
$adminId = auth_user_id_from_request($con, $adminId, 'admin');
if ($adminId === null) {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$customerId = isset($_POST['customerId']) ? (int) $_POST['customerId'] : 0;
$licenceId = isset($_POST['licenceId']) ? (int) $_POST['licenceId'] : 0;
$allowed = mess_remote_resolve_licence_ids_for_admin_customer($con, $customerId);
if ($allowed === null || !mess_remote_assert_licence_access($allowed, $licenceId)) {
    $response['message'] = 'customerId and licenceId required (licence must belong to customer)';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$response = mess_remote_save_member($con, $licenceId, $_POST);
echo json_encode($response);
mysqli_close($con);
