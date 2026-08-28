<?php
/**
 * Admin customer list with primary licence + branch count + dealer name.
 * GET optional: limit (int), status=all|active|trial|expired
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'customerResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['status'] = 'false';
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => 'false', 'customerResponse' => array()));
mysqli_query($con, 'set names utf8');

date_default_timezone_set('Asia/Kolkata');
$today = date('Y-m-d');

$limit = isset($_GET['limit']) ? (int) $_GET['limit'] : 0;
if ($limit < 0) {
    $limit = 0;
}
if ($limit > 500) {
    $limit = 500;
}

$sql = "SELECT u.id, u.name, u.email, u.contact_number, u.aadhar_number, u.address, u.shopName,
               u.dealerId, d.name AS dealerName,
               l.id AS licenses_id, l.licenseKey, l.licenseValidity, l.licenseType,
               l.licenseStatus, l.expiryDate, l.created_at AS registrationDate,
               l.paymentStatus, l.amount, l.fastBilling, l.takeAway, l.dineIn, l.mess,
               l.android_device_id, l.android_device_name, l.userType, l.userName,
               (SELECT COUNT(*) FROM `licenses` lx WHERE lx.userId = u.id) AS branchCount
        FROM `users` u
        LEFT JOIN `users` d ON d.id = u.dealerId AND d.role_id = '2'
        LEFT JOIN (
            SELECT l1.*
            FROM `licenses` l1
            INNER JOIN (
                SELECT userId, MAX(id) AS max_id
                FROM `licenses`
                GROUP BY userId
            ) lm ON lm.max_id = l1.id
        ) l ON l.userId = u.id
        WHERE u.role_id = '3'
        ORDER BY u.id DESC";

if ($limit > 0) {
    $sql .= ' LIMIT ' . $limit;
}

$rows = db_stmt_fetch_all($con, $sql, '');
$statusFilter = isset($_GET['status']) ? strtolower(trim($_GET['status'])) : 'all';

foreach ($rows as $row) {
    $licenses = array();
    $expiry = isset($row['expiryDate']) ? (string) $row['expiryDate'] : '';
    $statusRaw = strtolower(isset($row['licenseStatus']) ? (string) $row['licenseStatus'] : '');
    $type = isset($row['licenseType']) ? (string) $row['licenseType'] : '';
    $validity = isset($row['licenseValidity']) ? (string) $row['licenseValidity'] : '';
    $isTrial = in_array($type, array('Demo', 'Trial'), true) || $validity === '7';
    $isExpired = in_array($statusRaw, array('expire', 'expired'), true)
        || ($expiry !== '' && $expiry < $today);
    $isSuspended = in_array($statusRaw, array('suspended', 'revoked'), true);

    $displayBucket = 'active';
    if ($isExpired || $isSuspended) {
        $displayBucket = 'expired';
    } elseif ($isTrial) {
        $displayBucket = 'trial';
    }

    if ($statusFilter !== 'all' && $statusFilter !== $displayBucket) {
        continue;
    }

    if (!empty($row['licenses_id'])) {
        $licenses[] = array(
            'licenses_id' => (string) $row['licenses_id'],
            'licenseKey' => isset($row['licenseKey']) ? (string) $row['licenseKey'] : '',
            'licenseValidity' => $validity,
            'licenseType' => $type,
            'licenseStatus' => isset($row['licenseStatus']) ? (string) $row['licenseStatus'] : '',
            'registrationDate' => isset($row['registrationDate']) ? (string) $row['registrationDate'] : '',
            'expiryDate' => $expiry,
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
            'branchLabel' => isset($row['userName']) ? (string) $row['userName'] : ''
        );
    }

    $response['customerResponse'][] = array(
        'id' => (string) $row['id'],
        'name' => isset($row['name']) ? (string) $row['name'] : '',
        'email' => isset($row['email']) ? (string) $row['email'] : '',
        'contact_number' => isset($row['contact_number']) ? (string) $row['contact_number'] : '',
        'aadhar_number' => isset($row['aadhar_number']) ? (string) $row['aadhar_number'] : '',
        'address' => isset($row['address']) ? (string) $row['address'] : '',
        'shopName' => isset($row['shopName']) ? (string) $row['shopName'] : '',
        'dealerId' => isset($row['dealerId']) ? (string) $row['dealerId'] : '',
        'dealerName' => isset($row['dealerName']) ? (string) $row['dealerName'] : '',
        'branchCount' => isset($row['branchCount']) ? (string) $row['branchCount'] : '0',
        'licensesResponse' => $licenses
    );
}

mysqli_close($con);
echo json_encode($response);
