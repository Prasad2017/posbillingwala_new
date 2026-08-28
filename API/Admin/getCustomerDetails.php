<?php
/**
 * Admin customer details with licenses + summary counts + dealer name + catalog counts.
 * GET: customerId
 */
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../company_store_fields.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false', 'customerResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => 'false', 'customerResponse' => array()));
mysqli_query($con, 'set names utf8');

$customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
if ($customerId === '') {
    $response['message'] = 'customerId required';
    echo json_encode($response);
    exit;
}

date_default_timezone_set('Asia/Kolkata');
$today = date('Y-m-d');
$in30 = date('Y-m-d', strtotime('+30 days'));
$uid = (int) $customerId;

$user = db_stmt_fetch_one(
    $con,
    "SELECT u.id, u.name, u.email, u.contact_number, u.aadhar_number, u.address, u.shopName,
            u.dealerId, u.is_active, u.created_at, d.name AS dealerName
     FROM `users` u
     LEFT JOIN `users` d ON d.id = u.dealerId AND d.role_id = '2'
     WHERE u.id = ? AND u.role_id = '3'
     LIMIT 1",
    'i',
    $uid
);

if ($user === null) {
    $response['message'] = 'Customer not found';
    echo json_encode($response);
    exit;
}

$licenseRows = db_stmt_fetch_all(
    $con,
    "SELECT l.*, c.shopName1, c.shopName2, c.addressLine1, c.addressLine2, c.addressLine3, c.phoneNo1, c.phoneNo2
     FROM `licenses` l
     LEFT JOIN `companys` c ON c.licenseId = l.id
     WHERE l.userId = ?
     ORDER BY l.id DESC",
    'i',
    $uid
);

$licenses = array();
$activeLicenses = 0;
$expiringLicenses = 0;
$expiredLicenses = 0;
$trialLicenses = 0;
$deviceCount = 0;
$nextExpiry = '';

foreach ($licenseRows as $row) {
    $store = company_structured_fields($row);
    $companyAddress = company_display_address_oneline($row);
    $branch = licence_branch_fields($row);

    $expiry = isset($row['expiryDate']) ? (string) $row['expiryDate'] : '';
    $statusRaw = strtolower(isset($row['licenseStatus']) ? (string) $row['licenseStatus'] : '');
    $type = isset($row['licenseType']) ? (string) $row['licenseType'] : '';
    $validity = isset($row['licenseValidity']) ? (string) $row['licenseValidity'] : '';
    $isTrial = in_array($type, array('Demo', 'Trial'), true) || $validity === '7';
    $isExpired = in_array($statusRaw, array('expire', 'expired'), true)
        || ($expiry !== '' && $expiry < $today);
    $isSuspended = in_array($statusRaw, array('suspended', 'revoked'), true);
    $isActive = !$isExpired && !$isSuspended;
    $isExpiring = $isActive && $expiry !== '' && $expiry >= $today && $expiry <= $in30;

    if ($isExpired) {
        $expiredLicenses++;
    } elseif ($isTrial && $isActive) {
        $trialLicenses++;
        $activeLicenses++;
        if ($isExpiring) {
            $expiringLicenses++;
        }
    } elseif ($isActive) {
        $activeLicenses++;
        if ($isExpiring) {
            $expiringLicenses++;
        }
    }

    $deviceId = isset($row['android_device_id']) ? trim((string) $row['android_device_id']) : '';
    if ($deviceId !== '') {
        $deviceCount++;
    }

    if ($isActive && $expiry !== '') {
        if ($nextExpiry === '' || $expiry < $nextExpiry) {
            $nextExpiry = $expiry;
        }
    }

    $licenses[] = array(
        'licenses_id' => (string) $row['id'],
        'companyAddress' => $companyAddress,
        'shopName1' => $store['shopName1'],
        'shopName2' => $store['shopName2'],
        'addressLine1' => $store['addressLine1'],
        'addressLine2' => $store['addressLine2'],
        'addressLine3' => $store['addressLine3'],
        'phoneNo1' => $store['phoneNo1'],
        'phoneNo2' => $store['phoneNo2'],
        'licenseKey' => isset($row['licenseKey']) ? (string) $row['licenseKey'] : '',
        'licenseValidity' => $validity,
        'licenseType' => $type,
        'licenseStatus' => isset($row['licenseStatus']) ? (string) $row['licenseStatus'] : '',
        'registrationDate' => isset($row['created_at']) ? (string) $row['created_at'] : '',
        'expiryDate' => $expiry,
        'paymentStatus' => isset($row['paymentStatus']) ? (string) $row['paymentStatus'] : '',
        'amount' => isset($row['amount']) ? (string) $row['amount'] : '0',
        'fastBilling' => isset($row['fastBilling']) ? (string) $row['fastBilling'] : '0',
        'takeAway' => isset($row['takeAway']) ? (string) $row['takeAway'] : '0',
        'dineIn' => isset($row['dineIn']) ? (string) $row['dineIn'] : '0',
        'mess' => isset($row['mess']) ? (string) $row['mess'] : '0',
        'android_device_id' => $deviceId,
        'android_device_name' => isset($row['android_device_name']) ? (string) $row['android_device_name'] : '',
        'userType' => $branch['userType'],
        'userName' => $branch['userName'],
        'branchLabel' => $branch['branchLabel']
    );
}

$categoryCount = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `categories` WHERE `userId`=?", 'i', $uid);
$subcategoryCount = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `product_subcategories` WHERE `userId`=?", 'i', $uid);
$productCount = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `products` WHERE `userId`=?", 'i', $uid);

$response = array(
    'status' => 'true',
    'customerResponse' => array(
        array(
            'id' => (string) $user['id'],
            'name' => isset($user['name']) ? (string) $user['name'] : '',
            'email' => isset($user['email']) ? (string) $user['email'] : '',
            'contact_number' => isset($user['contact_number']) ? (string) $user['contact_number'] : '',
            'aadhar_number' => isset($user['aadhar_number']) ? (string) $user['aadhar_number'] : '',
            'address' => isset($user['address']) ? (string) $user['address'] : '',
            'shopName' => isset($user['shopName']) ? (string) $user['shopName'] : '',
            'dealerId' => isset($user['dealerId']) ? (string) $user['dealerId'] : '',
            'dealerName' => isset($user['dealerName']) ? (string) $user['dealerName'] : '',
            'is_active' => isset($user['is_active']) ? (string) $user['is_active'] : '1',
            'created_at' => isset($user['created_at']) ? (string) $user['created_at'] : '',
            'licensesResponse' => $licenses,
            'branchCount' => (string) count($licenses),
            'licenseCount' => (string) count($licenses),
            'deviceCount' => (string) $deviceCount,
            'activeLicenses' => (string) $activeLicenses,
            'expiringLicenses' => (string) $expiringLicenses,
            'expiredLicenses' => (string) $expiredLicenses,
            'trialLicenses' => (string) $trialLicenses,
            'nextExpiry' => $nextExpiry,
            'categoryCount' => $categoryCount !== '' ? $categoryCount : '0',
            'subcategoryCount' => $subcategoryCount !== '' ? $subcategoryCount : '0',
            'productCount' => $productCount !== '' ? $productCount : '0'
        )
    ),
    'activeLicenses' => (string) $activeLicenses,
    'expiringLicenses' => (string) $expiringLicenses,
    'expiredLicenses' => (string) $expiredLicenses,
    'trialLicenses' => (string) $trialLicenses,
    'categoryCount' => $categoryCount !== '' ? $categoryCount : '0',
    'subcategoryCount' => $subcategoryCount !== '' ? $subcategoryCount : '0',
    'productCount' => $productCount !== '' ? $productCount : '0'
);

mysqli_close($con);
echo json_encode($response);
