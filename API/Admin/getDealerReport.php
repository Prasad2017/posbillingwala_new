<?php
/**
 * Dealer-wise summary for Admin CRM (KPIs + this-month sales).
 * GET: dealerId
 */
include_once('config.php');
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';
require_once __DIR__ . '/auth_guard.php';

$response = array(
    'status' => 'false',
    'totalCustomer' => '0',
    'activeCustomer' => '0',
    'trialCustomer' => '0',
    'expiredCustomer' => '0',
    'activeLicenses' => '0',
    'trialLicenses' => '0',
    'expiredLicenses' => '0',
    'totalLicenses' => '0',
    'totalBranches' => '0',
    'totalDevices' => '0',
    'netSales' => '0',
    'monthSales' => '0',
    'collection' => '0',
    'topCustomers' => array()
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
    $monthStart = date('Y-m-01');
    $did = (int) $dealerId;

    $totalCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='3' AND `dealerId`=?",
        'i',
        $did
    );

    $activeCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         INNER JOIN `licenses` l ON l.userId = u.id
         WHERE u.role_id='3' AND u.dealerId=? AND u.is_active='1'
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        'is',
        $did,
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
        $did,
        $today
    );

    $expiredCustomer = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
         WHERE u.role_id='3' AND u.dealerId=?
           AND NOT EXISTS (
             SELECT 1 FROM `licenses` l
             WHERE l.userId = u.id
               AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
               AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)
           )",
        'is',
        $did,
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
        $did,
        $today
    );

    $trialLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'
           AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)",
        'is',
        $did,
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
        $did,
        $today
    );

    $totalLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'",
        'i',
        $did
    );

    $totalBranches = $totalLicenses;

    $totalDevices = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'
           AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id) <> ''",
        'i',
        $did
    );

    $allSalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'" . invoice_and_not_refunded(),
        'i',
        $did
    );
    $netSales = '0';
    if ($allSalesRow !== null && isset($allSalesRow['total'])) {
        $netSales = (string) round((float) $allSalesRow['total'], 2);
    }

    $monthSalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3' AND DATE(i.invoiceDate) >= ?" . invoice_and_not_refunded(),
        'is',
        $did,
        $monthStart
    );
    $monthSales = '0';
    if ($monthSalesRow !== null && isset($monthSalesRow['total'])) {
        $monthSales = (string) round((float) $monthSalesRow['total'], 2);
    }

    // Collection approximated from paid license amounts this month
    $collectionRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(l.amount), 0) AS total
         FROM `licenses` l
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.dealerId=? AND u.role_id='3'
           AND LOWER(IFNULL(l.paymentStatus,'')) IN ('paid','success','completed')
           AND l.created_at >= ?",
        'is',
        $did,
        $monthStart
    );
    $collection = '0';
    if ($collectionRow !== null && isset($collectionRow['total'])) {
        $collection = (string) round((float) $collectionRow['total'], 2);
    }

    $topCustomers = array();
    $topRows = db_stmt_fetch_all(
        $con,
        "SELECT u.id AS customerId, u.name AS customerName, u.shopName,
                COALESCE(SUM(CASE WHEN DATE(i.invoiceDate) >= ? THEN i.totalAmount ELSE 0 END), 0) AS totalSales
         FROM `users` u
         LEFT JOIN `licenses` l ON l.userId = u.id
         LEFT JOIN `invoice` i ON i.licenseId = l.id" . invoice_and_not_refunded() . "
         WHERE u.role_id='3' AND u.dealerId=?
         GROUP BY u.id, u.name, u.shopName
         ORDER BY totalSales DESC
         LIMIT 5",
        'si',
        $monthStart,
        $did
    );
    foreach ($topRows as $tr) {
        $topCustomers[] = array(
            'customerId' => (string) $tr['customerId'],
            'customerName' => isset($tr['customerName']) ? (string) $tr['customerName'] : '',
            'shopName' => isset($tr['shopName']) ? (string) $tr['shopName'] : '',
            'totalSales' => (string) round((float) $tr['totalSales'], 2)
        );
    }

    $response = array(
        'status' => 'true',
        'totalCustomer' => $totalCustomer,
        'activeCustomer' => $activeCustomer,
        'trialCustomer' => $trialCustomer,
        'expiredCustomer' => $expiredCustomer,
        'activeLicenses' => $activeLicenses,
        'trialLicenses' => $trialLicenses,
        'expiredLicenses' => $expiredLicenses,
        'totalLicenses' => $totalLicenses,
        'totalBranches' => $totalBranches,
        'totalDevices' => $totalDevices,
        'netSales' => $netSales,
        'monthSales' => $monthSales,
        'collection' => $collection,
        'topCustomers' => $topCustomers
    );
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
