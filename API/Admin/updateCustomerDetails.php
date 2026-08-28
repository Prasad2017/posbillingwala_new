<?php
/**
 * Update customer profile fields.
 * POST: customerId, customerName, customerMobileNumber, customerAddress, customerShopName
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'update failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use POST';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');

$customerId = isset($_POST['customerId']) ? trim($_POST['customerId']) : '';
$customerName = isset($_POST['customerName']) ? trim($_POST['customerName']) : '';
$customerMobileNumber = isset($_POST['customerMobileNumber']) ? trim($_POST['customerMobileNumber']) : '';
$customerAddress = isset($_POST['customerAddress']) ? trim($_POST['customerAddress']) : '';
$customerShopName = isset($_POST['customerShopName']) ? trim($_POST['customerShopName']) : '';

if ($customerId === '' || $customerName === '' || $customerMobileNumber === '') {
    $response['message'] = 'customerId, name and mobile are required.';
    echo json_encode($response);
    exit;
}

$uid = (int) $customerId;
$existing = db_stmt_fetch_one(
    $con,
    "SELECT id FROM `users` WHERE id=? AND role_id='3' LIMIT 1",
    'i',
    $uid
);
if ($existing === null) {
    $response['message'] = 'Customer not found.';
    echo json_encode($response);
    exit;
}

$ok = db_stmt_execute(
    $con,
    "UPDATE `users`
     SET `name`=?, `contact_number`=?, `address`=?, `shopName`=?
     WHERE `id`=? AND `role_id`='3'",
    'ssssi',
    $customerName,
    $customerMobileNumber,
    $customerAddress,
    $customerShopName,
    $uid
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'update successful!';
} else {
    $response['message'] = 'Unable to update customer.';
}

mysqli_close($con);
echo json_encode($response);
