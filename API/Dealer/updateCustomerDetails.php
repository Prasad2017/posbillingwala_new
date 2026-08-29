<?php
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

dealer_require_auth($con, $response);
mysqli_query($con, 'set names utf8');

$customerId = isset($_POST['customerId']) ? (int) $_POST['customerId'] : 0;
$customerName = isset($_POST['customerName']) ? trim($_POST['customerName']) : '';
$customerMobileNumber = isset($_POST['customerMobileNumber']) ? trim($_POST['customerMobileNumber']) : '';
$customerAddress = isset($_POST['customerAddress']) ? trim($_POST['customerAddress']) : '';
$customerShopName = isset($_POST['customerShopName']) ? trim($_POST['customerShopName']) : '';
$dealerId = isset($_POST['userId']) ? (int) $_POST['userId'] : 0;

if ($customerId <= 0 || $customerName === '' || $customerMobileNumber === '') {
    $response['message'] = 'customerId, name and mobile are required.';
    echo json_encode($response);
    exit;
}

if ($dealerId > 0) {
    $ok = db_stmt_execute(
        $con,
        "UPDATE `users` SET `name`=?, `contact_number`=?, `address`=?, `shopName`=?
         WHERE `id`=? AND `role_id`='3' AND `dealerId`=?",
        'ssssii',
        $customerName,
        $customerMobileNumber,
        $customerAddress,
        $customerShopName,
        $customerId,
        $dealerId
    );
} else {
    $ok = db_stmt_execute(
        $con,
        "UPDATE `users` SET `name`=?, `contact_number`=?, `address`=?, `shopName`=?
         WHERE `id`=? AND `role_id`='3'",
        'ssssi',
        $customerName,
        $customerMobileNumber,
        $customerAddress,
        $customerShopName,
        $customerId
    );
}

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
        $customerId
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
