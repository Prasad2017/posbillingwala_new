<?php
/**
 * Dealer: export mess members/payments for customer (licenceId optional = all customer licences).
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess/mess_member_remote.php';

dealer_require_auth($con);

$dealerId = isset($_GET['userId']) ? $_GET['userId'] : '';
$dealerId = auth_user_id_from_request($con, $dealerId, 'dealer');
if ($dealerId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status' => '0', 'message' => 'Invalid or expired auth token'));
    mysqli_close($con);
    exit;
}

$customerId = isset($_GET['customerId']) ? (int) $_GET['customerId'] : 0;
$licenceId = isset($_GET['licenceId']) ? (int) $_GET['licenceId'] : 0;
$allowed = mess_remote_resolve_licence_ids_for_dealer_customer($con, $dealerId, $customerId);
if ($allowed === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status' => '0', 'message' => 'Customer not found for this dealer'));
    mysqli_close($con);
    exit;
}

if ($licenceId > 0) {
    if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('status' => '0', 'message' => 'Licence not found for this customer'));
        mysqli_close($con);
        exit;
    }
    $licenceIds = array($licenceId);
} else {
    $licenceIds = $allowed;
}

$name = $licenceId > 0
    ? 'mess_members_licence_' . $licenceId . '.xlsx'
    : 'mess_members_customer_' . $customerId . '.xlsx';
mess_remote_export_xlsx($con, $licenceIds, true, $name);
mysqli_close($con);
