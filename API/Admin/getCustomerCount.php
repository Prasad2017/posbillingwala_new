<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    admin_require_auth($con, array(
        'status' => 'false',
        'totalCustomer' => '0',
        'totalDealer' => '0',
        'activeCustomer' => '0',
        'trialCustomer' => '0',
        'expiredCustomer' => '0',
        'activeLicenses' => '0',
        'expiringLicenses' => '0',
        'expiredLicenses' => '0',
        'totalBranches' => '0',
        'totalDevices' => '0'
    ));

    date_default_timezone_set('Asia/Kolkata');
    $today = date('Y-m-d');
    $in30 = date('Y-m-d', strtotime('+30 days'));

    $totalCustomer = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='3'", '');
    $totalDealer = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='2'", '');

    // Customers with at least one non-expired, non-suspended licence
    $activeCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         INNER JOIN `licenses` l ON l.userId = u.id
         WHERE u.role_id='3' AND u.is_active='1'
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        's',
        $today
    );

    $trialCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         INNER JOIN `licenses` l ON l.userId = u.id
         WHERE u.role_id='3'
           AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        's',
        $today
    );

    $expiredCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         WHERE u.role_id='3'
           AND NOT EXISTS (
             SELECT 1 FROM `licenses` l
             WHERE l.userId = u.id
               AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
               AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)
           )",
        's',
        $today
    );

    $activeLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (expiryDate IS NULL OR expiryDate = '' OR expiryDate >= ?)",
        's',
        $today
    );

    $expiringLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND expiryDate IS NOT NULL AND expiryDate <> ''
           AND expiryDate >= ? AND expiryDate <= ?",
        'ss',
        $today,
        $in30
    );

    $expiredLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE LOWER(IFNULL(licenseStatus,'')) IN ('expire','expired')
            OR (expiryDate IS NOT NULL AND expiryDate <> '' AND expiryDate < ?)",
        's',
        $today
    );

    $totalBranches = (string) db_stmt_scalar_int($con, 'SELECT COUNT(*) AS c FROM `licenses`', '');

    $totalDevices = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE android_device_id IS NOT NULL AND TRIM(android_device_id) <> ''",
        ''
    );

    $response = array(
        'status' => 'true',
        'totalCustomer' => $totalCustomer !== '' ? $totalCustomer : '0',
        'totalDealer' => $totalDealer !== '' ? $totalDealer : '0',
        'activeCustomer' => $activeCustomer !== '' ? $activeCustomer : '0',
        'trialCustomer' => $trialCustomer !== '' ? $trialCustomer : '0',
        'expiredCustomer' => $expiredCustomer !== '' ? $expiredCustomer : '0',
        'activeLicenses' => $activeLicenses !== '' ? $activeLicenses : '0',
        'expiringLicenses' => $expiringLicenses !== '' ? $expiringLicenses : '0',
        'expiredLicenses' => $expiredLicenses !== '' ? $expiredLicenses : '0',
        'totalBranches' => $totalBranches !== '' ? $totalBranches : '0',
        'totalDevices' => $totalDevices !== '' ? $totalDevices : '0'
    );

    mysqli_close($con);
} else {
    $response = array('status' => 'false', 'message' => 'Use GET');
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
