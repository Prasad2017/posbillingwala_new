<?php
/**
 * Update customer profile fields.
 * POST: customerId, customerName, customerMobileNumber, customerAddress, customerShopName
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../licence_expiry.php';
require_once __DIR__ . '/../user_identity.php';

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

$customerMobileDigits = licence_normalize_contact($customerMobileNumber);
if (strlen($customerMobileDigits) < 10) {
    $response['message'] = 'Please enter a valid 10-digit mobile number.';
    echo json_encode($response);
    exit;
}

if (user_customer_mobile_taken($con, $customerMobileDigits, $uid)) {
    $response['message'] = 'This mobile number is already registered for another customer.';
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
    $customerMobileDigits,
    $customerAddress,
    $customerShopName,
    $uid
);

$hasModules = isset($_POST['fastBilling']) || isset($_POST['takeAway'])
    || isset($_POST['dineIn']) || isset($_POST['mess']);
if ($ok && $hasModules) {
    $fastBilling = isset($_POST['fastBilling']) && (string) $_POST['fastBilling'] === '1' ? 1 : 0;
    $takeAway = isset($_POST['takeAway']) && (string) $_POST['takeAway'] === '1' ? 1 : 0;
    $dineIn = isset($_POST['dineIn']) && (string) $_POST['dineIn'] === '1' ? 1 : 0;
    $mess = isset($_POST['mess']) && (string) $_POST['mess'] === '1' ? 1 : 0;
    db_stmt_execute(
        $con,
        "UPDATE `licenses` SET `fastBilling`=?, `takeAway`=?, `dineIn`=?, `mess`=?
         WHERE `userId`=? AND `userType`='owner'",
        'iiiii',
        $fastBilling,
        $takeAway,
        $dineIn,
        $mess,
        $uid
    );
}

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'update successful!';
} else {
    $response['message'] = 'Unable to update customer.';
}

mysqli_close($con);
echo json_encode($response);
