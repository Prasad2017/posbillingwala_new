<?php
/**
 * Licenses for all customers under a dealer.
 * GET: dealerId, optional limit
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'licensesResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['status'] = 'false';
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => 'false', 'licensesResponse' => array()));
mysqli_query($con, 'set names utf8');

$dealerId = isset($_GET['dealerId']) ? trim($_GET['dealerId']) : '';
if ($dealerId === '') {
    $response['status'] = 'false';
    $response['message'] = 'dealerId required';
    echo json_encode($response);
    exit;
}

$limit = isset($_GET['limit']) ? (int) $_GET['limit'] : 0;
if ($limit < 0) {
    $limit = 0;
}
if ($limit > 500) {
    $limit = 500;
}

$sql = "SELECT l.id AS licenses_id, l.licenseKey, l.licenseValidity, l.licenseType,
               l.licenseStatus, l.expiryDate, l.created_at AS registrationDate,
               l.paymentStatus, l.amount, l.fastBilling, l.takeAway, l.dineIn, l.mess,
               l.android_device_id, l.android_device_name, l.userType, l.userName,
               u.id AS customerId, u.name AS customerName, u.shopName
        FROM `licenses` l
        INNER JOIN `users` u ON u.id = l.userId
        WHERE u.role_id = '3' AND u.dealerId = ?
        ORDER BY l.id DESC";

if ($limit > 0) {
    $sql .= ' LIMIT ' . $limit;
}

$rows = db_stmt_fetch_all($con, $sql, 'i', (int) $dealerId);

foreach ($rows as $row) {
    $response['licensesResponse'][] = array(
        'licenses_id' => (string) $row['licenses_id'],
        'licenseKey' => isset($row['licenseKey']) ? (string) $row['licenseKey'] : '',
        'licenseValidity' => isset($row['licenseValidity']) ? (string) $row['licenseValidity'] : '',
        'licenseType' => isset($row['licenseType']) ? (string) $row['licenseType'] : '',
        'licenseStatus' => isset($row['licenseStatus']) ? (string) $row['licenseStatus'] : '',
        'registrationDate' => isset($row['registrationDate']) ? (string) $row['registrationDate'] : '',
        'expiryDate' => isset($row['expiryDate']) ? (string) $row['expiryDate'] : '',
        'paymentStatus' => isset($row['paymentStatus']) ? (string) $row['paymentStatus'] : '',
        'amount' => isset($row['amount']) ? (string) $row['amount'] : '0',
        'fastBilling' => isset($row['fastBilling']) ? (string) $row['fastBilling'] : '0',
        'takeAway' => isset($row['takeAway']) ? (string) $row['takeAway'] : '0',
        'dineIn' => isset($row['dineIn']) ? (string) $row['dineIn'] : '0',
        'mess' => isset($row['mess']) ? (string) $row['mess'] : '0',
        'android_device_id' => isset($row['android_device_id']) ? (string) $row['android_device_id'] : '',
        'android_device_name' => isset($row['android_device_name']) ? (string) $row['android_device_name'] : '',
        'userType' => isset($row['userType']) ? (string) $row['userType'] : '',
        'userName' => isset($row['userName']) ? (string) $row['userName'] : '',
        'branchLabel' => isset($row['userName']) ? (string) $row['userName'] : '',
        'customerId' => isset($row['customerId']) ? (string) $row['customerId'] : '',
        'customerName' => isset($row['customerName']) ? (string) $row['customerName'] : '',
        'shopName' => isset($row['shopName']) ? (string) $row['shopName'] : ''
    );
}

mysqli_close($con);
echo json_encode($response);
