<?php
include_once('config.php');
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/auth_guard.php';

/**
 * Dealer-wise summary for Admin CRM.
 * GET: dealerId
 */
$response = array(
    'status' => 'false',
    'totalCustomer' => '0',
    'activeCustomer' => '0',
    'trialCustomer' => '0',
    'activeLicenses' => '0',
    'expiredLicenses' => '0',
    'totalBranches' => '0',
    'totalDevices' => '0'
);

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    admin_require_auth($con, $response);
    mysqli_query($con, 'set names utf8');

    $dealerId = isset($_GET['dealerId']) ? trim($_GET['dealerId']) : '';
    if ($dealerId === '') {
        $response['message'] = 'dealerId required';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    date_default_timezone_set('Asia/Kolkata');
    $today = date('Y-m-d');

    $totalCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='3' AND `dealerId`=?",
        'i',
        (int) $dealerId
    );

    $activeCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         INNER JOIN `licenses` l ON l.userId = u.id
         WHERE u.role_id='3' AND u.dealerId=? AND u.is_active='1'
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        'is',
        (int) $dealerId,
        $today
    );

    $trialCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         INNER JOIN `licenses` l ON l.userId = u.id
         WHERE u.role_id='3' AND u.dealerId=?
           AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        'is',
        (int) $dealerId,
        $today
    );

    $activeLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        'is',
        (int) $dealerId,
        $today
    );

    $expiredLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'
           AND (LOWER(IFNULL(l.licenseStatus,'')) IN ('expire','expired')
                OR (l.expiryDate IS NOT NULL AND l.expiryDate <> '' AND l.expiryDate < ?))",
        'is',
        (int) $dealerId,
        $today
    );

    $totalBranches = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'",
        'i',
        (int) $dealerId
    );

    $totalDevices = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'
           AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id) <> ''",
        'i',
        (int) $dealerId
    );

    $response = array(
        'status' => 'true',
        'totalCustomer' => $totalCustomer,
        'activeCustomer' => $activeCustomer,
        'trialCustomer' => $trialCustomer,
        'activeLicenses' => $activeLicenses,
        'expiredLicenses' => $expiredLicenses,
        'totalBranches' => $totalBranches,
        'totalDevices' => $totalDevices
    );
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
