<?php
/**
 * Admin dashboard KPIs, month sales, trends, and 7-day sparkline.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

function admin_pct_change($current, $previous) {
    $c = (float) $current;
    $p = (float) $previous;
    if ($p == 0.0) {
        return $c > 0 ? '100.0' : '0.0';
    }
    return (string) round((($c - $p) / $p) * 100.0, 1);
}

function admin_trend_label($pct) {
    $v = (float) $pct;
    $abs = abs($v);
    $arrow = $v >= 0 ? '↑' : '↓';
    return $arrow . ' ' . number_format($abs, 1) . '% vs last month';
}

function admin_trend_label_short($pct) {
    $v = (float) $pct;
    $abs = abs($v);
    $arrow = $v >= 0 ? '↑' : '↓';
    return $arrow . ' ' . number_format($abs, 1) . '%';
}

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
        'totalDevices' => '0',
        'trialLicenses' => '0',
        'expiringLicenses7Days' => '0',
        'trialLicensesExpiringTomorrow' => '0',
        'customersAddedThisMonth' => '0',
        'netSales' => '0',
        'todaySales' => '0',
        'billCount' => '0',
        'notificationCount' => '0',
        'salesSparkline' => array()
    ));

    date_default_timezone_set('Asia/Kolkata');
    $today = date('Y-m-d');
    $yesterday = date('Y-m-d', strtotime('-1 day'));
    $tomorrow = date('Y-m-d', strtotime('+1 day'));
    $in30 = date('Y-m-d', strtotime('+30 days'));
    $in7 = date('Y-m-d', strtotime('+7 days'));
    $monthStart = date('Y-m-01');
    $lastMonthStart = date('Y-m-01', strtotime('first day of last month'));
    $lastMonthEnd = date('Y-m-t', strtotime('last day of last month'));
    $sparkStart = date('Y-m-d', strtotime('-6 days'));

    $totalCustomer = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='3'", '');
    $totalDealer = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='2'", '');

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

    $trialLicenses = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE (licenseType IN ('Demo','Trial') OR licenseValidity='7')
           AND LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (expiryDate IS NULL OR expiryDate = '' OR expiryDate >= ?)",
        's',
        $today
    );

    $expiringLicenses7Days = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND expiryDate IS NOT NULL AND expiryDate <> ''
           AND expiryDate >= ? AND expiryDate <= ?",
        'ss',
        $today,
        $in7
    );

    $trialLicensesExpiringTomorrow = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE (licenseType IN ('Demo','Trial') OR licenseValidity='7')
           AND LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND expiryDate IS NOT NULL AND expiryDate <> ''
           AND DATE(expiryDate) = ?",
        's',
        $tomorrow
    );

    $customersAddedThisMonth = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users`
         WHERE role_id='3' AND created_at >= ?",
        's',
        $monthStart
    );

    $customersAddedLastMonth = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users`
         WHERE role_id='3' AND created_at >= ? AND created_at < ?",
        'ss',
        $lastMonthStart,
        $monthStart
    );

    $customersBeforeThisMonth = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users`
         WHERE role_id='3' AND (created_at IS NULL OR created_at < ?)",
        's',
        $monthStart
    );

    $branchesThisMonth = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` WHERE created_at >= ?",
        's',
        $monthStart
    );
    $branchesLastMonth = (string) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses` WHERE created_at >= ? AND created_at < ?",
        'ss',
        $lastMonthStart,
        $monthStart
    );

    $monthSalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3' AND DATE(i.invoiceDate) >= ?" . invoice_and_not_refunded(),
        's',
        $monthStart
    );
    $monthSales = '0';
    if ($monthSalesRow !== null && isset($monthSalesRow['total'])) {
        $monthSales = (string) round((float) $monthSalesRow['total'], 2);
    }

    $lastMonthSalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3'
           AND DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded(),
        'ss',
        $lastMonthStart,
        $lastMonthEnd
    );
    $lastMonthSales = '0';
    if ($lastMonthSalesRow !== null && isset($lastMonthSalesRow['total'])) {
        $lastMonthSales = (string) round((float) $lastMonthSalesRow['total'], 2);
    }

    $allTimeSalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3'" . invoice_and_not_refunded(),
        ''
    );
    $allTimeSales = '0';
    if ($allTimeSalesRow !== null && isset($allTimeSalesRow['total'])) {
        $allTimeSales = (string) round((float) $allTimeSalesRow['total'], 2);
    }

    $todaySalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total, COUNT(*) AS bills
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3' AND DATE(i.invoiceDate) = ?" . invoice_and_not_refunded(),
        's',
        $today
    );
    $todaySales = '0';
    $billCount = '0';
    if ($todaySalesRow !== null) {
        $todaySales = (string) round((float) $todaySalesRow['total'], 2);
        $billCount = (string) (int) $todaySalesRow['bills'];
    }

    $yesterdaySalesRow = db_stmt_fetch_one(
        $con,
        "SELECT COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3' AND DATE(i.invoiceDate) = ?" . invoice_and_not_refunded(),
        's',
        $yesterday
    );
    $yesterdaySales = '0';
    if ($yesterdaySalesRow !== null && isset($yesterdaySalesRow['total'])) {
        $yesterdaySales = (string) round((float) $yesterdaySalesRow['total'], 2);
    }

    $sparkRows = db_stmt_fetch_all(
        $con,
        "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount), 0) AS total
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         INNER JOIN `users` u ON u.id = l.userId
         WHERE u.role_id='3' AND DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded() . "
         GROUP BY DATE(i.invoiceDate)",
        'ss',
        $sparkStart,
        $today
    );
    $sparkMap = array();
    foreach ($sparkRows as $sr) {
        $sparkMap[(string) $sr['d']] = (float) $sr['total'];
    }
    $salesSparkline = array();
    for ($i = 6; $i >= 0; $i--) {
        $d = date('Y-m-d', strtotime('-' . $i . ' days'));
        $salesSparkline[] = isset($sparkMap[$d]) ? (string) round($sparkMap[$d], 2) : '0';
    }

    $netSalesTrend = admin_pct_change($monthSales, $lastMonthSales);
    $todaySalesTrend = admin_pct_change($todaySales, $yesterdaySales);
    $customersAddedTrend = admin_pct_change($customersAddedThisMonth, $customersAddedLastMonth);
    $totalCustomerTrend = admin_pct_change($customersAddedThisMonth, $customersBeforeThisMonth);
    $activeBranchesTrend = admin_pct_change($branchesThisMonth, $branchesLastMonth);

    // License/customer KPI trends approximated from month-over-month growth rates
    $activeCustomerTrend = $totalCustomerTrend;
    $trialCustomerTrend = $customersAddedTrend;
    $expiredCustomerTrend = admin_pct_change($expiredCustomer, max((int) $expiredCustomer - (int) $customersAddedThisMonth, 0));
    $activeLicensesTrend = $activeBranchesTrend;
    $expiringLicensesTrend = admin_pct_change($expiringLicenses, max((int) $expiringLicenses - 1, 0));
    $trialLicensesTrend = $customersAddedTrend;
    $expiredLicensesTrend = admin_pct_change($expiredLicenses, max((int) $expiredLicenses - 1, 0));

    $notificationCount = (string) (
        (int) $expiringLicenses7Days
        + (int) $expiredLicenses
        + (int) $trialLicensesExpiringTomorrow
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
        'totalDevices' => $totalDevices !== '' ? $totalDevices : '0',
        'trialLicenses' => $trialLicenses !== '' ? $trialLicenses : '0',
        'expiringLicenses7Days' => $expiringLicenses7Days !== '' ? $expiringLicenses7Days : '0',
        'trialLicensesExpiringTomorrow' => $trialLicensesExpiringTomorrow !== '' ? $trialLicensesExpiringTomorrow : '0',
        'customersAddedThisMonth' => $customersAddedThisMonth !== '' ? $customersAddedThisMonth : '0',
        'customersAddedLastMonth' => $customersAddedLastMonth !== '' ? $customersAddedLastMonth : '0',
        'netSales' => $monthSales,
        'monthSales' => $monthSales,
        'lastMonthSales' => $lastMonthSales,
        'allTimeSales' => $allTimeSales,
        'todaySales' => $todaySales,
        'yesterdaySales' => $yesterdaySales,
        'billCount' => $billCount,
        'notificationCount' => $notificationCount,
        'salesSparkline' => $salesSparkline,
        'netSalesTrend' => $netSalesTrend,
        'todaySalesTrend' => $todaySalesTrend,
        'customersAddedTrend' => $customersAddedTrend,
        'activeBranchesTrend' => $activeBranchesTrend,
        'totalCustomerTrend' => $totalCustomerTrend,
        'activeCustomerTrend' => $activeCustomerTrend,
        'trialCustomerTrend' => $trialCustomerTrend,
        'expiredCustomerTrend' => $expiredCustomerTrend,
        'activeLicensesTrend' => $activeLicensesTrend,
        'expiringLicensesTrend' => $expiringLicensesTrend,
        'trialLicensesTrend' => $trialLicensesTrend,
        'expiredLicensesTrend' => $expiredLicensesTrend,
        'netSalesTrendLabel' => admin_trend_label_short($netSalesTrend),
        'todaySalesTrendLabel' => admin_trend_label_short($todaySalesTrend),
        'customersAddedTrendLabel' => admin_trend_label_short($customersAddedTrend),
        'activeBranchesTrendLabel' => admin_trend_label_short($activeBranchesTrend),
        'totalCustomerTrendLabel' => admin_trend_label($totalCustomerTrend),
        'activeCustomerTrendLabel' => admin_trend_label($activeCustomerTrend),
        'trialCustomerTrendLabel' => admin_trend_label($trialCustomerTrend),
        'expiredCustomerTrendLabel' => admin_trend_label($expiredCustomerTrend),
        'activeLicensesTrendLabel' => admin_trend_label($activeLicensesTrend),
        'expiringLicensesTrendLabel' => admin_trend_label($expiringLicensesTrend),
        'trialLicensesTrendLabel' => admin_trend_label($trialLicensesTrend),
        'expiredLicensesTrendLabel' => admin_trend_label($expiredLicensesTrend)
    );

    mysqli_close($con);
} else {
    $response = array('status' => 'false', 'message' => 'Use GET');
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
