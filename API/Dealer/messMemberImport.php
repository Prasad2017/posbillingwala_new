<?php
/**
 * Dealer: import mess members + payments (customerId + licenceId required).
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

dealer_require_auth($con);
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$dealerId = isset($_POST['userId']) ? $_POST['userId'] : '';
$dealerId = auth_user_id_from_request($con, $dealerId, 'dealer');
if ($dealerId === null) {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$customerId = isset($_POST['customerId']) ? (int) $_POST['customerId'] : 0;
$licenceId = isset($_POST['licenceId']) ? (int) $_POST['licenceId'] : 0;
$allowed = mess_remote_resolve_licence_ids_for_dealer_customer($con, $dealerId, $customerId);
if ($allowed === null || !mess_remote_assert_licence_access($allowed, $licenceId)) {
    $response['message'] = 'customerId and licenceId required (licence must belong to customer)';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if (!isset($_FILES['importFile']) || !is_uploaded_file($_FILES['importFile']['tmp_name'])) {
    $response['message'] = 'importFile is required';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$response = mess_remote_import_xlsx($con, $licenceId, $_FILES['importFile']['tmp_name']);
echo json_encode($response);
mysqli_close($con);
