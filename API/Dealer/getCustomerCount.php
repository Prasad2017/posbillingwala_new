<?php
/**
 * Dealer dashboard KPIs — scoped to this dealer's customers (same shape as Admin).
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

function dealer_pct_change($current, $previous) {
    $c = (float) $current;
    $p = (float) $previous;
    if ($p == 0.0) {
        return $c > 0 ? '100.0' : '0.0';
    }
    return (string) round((($c - $p) / $p) * 100.0, 1);
}

function dealer_trend_label($pct) {
    $v = (float) $pct;
    $arrow = $v >= 0 ? '↑' : '↓';
    return $arrow . ' ' . number_format(abs($v), 1) . '% vs last month';
}

function dealer_trend_label_short($pct) {
    $v = (float) $pct;
    $arrow = $v >= 0 ? '↑' : '↓';
    return $arrow . ' ' . number_format(abs($v), 1) . '%';
}

header('Content-Type: application/json; charset=utf-8');
$empty = array(
    'status' => 'false',
    'message' => '0',
    'totalCustomer' => '0',
    'activeCustomer' => '0',
    'trialCustomer' => '0',
    'expiredCustomer' => '0',
    'activePercent' => '0',
    'trialPercent' => '0',
    'expiredPercent' => '0',
    'activeLicenses' => '0',
    'expiringLicenses' => '0',
    'expiredLicenses' => '0',
    'totalBranches' => '0',
    'trialLicenses' => '0',
    'expiringLicenses7Days' => '0',
    'trialLicensesExpiringTomorrow' => '0',
    'customersAddedThisMonth' => '0',
    'netSales' => '0',
    'todaySales' => '0',
    'billCount' => '0',
    'notificationCount' => '0',
    'salesSparkline' => array(),
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($empty);
    exit;
}

dealer_require_auth($con, $empty);
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');

$dealerId = isset($_GET['userId']) ? (int) $_GET['userId'] : 0;
if ($dealerId <= 0) {
    echo json_encode($empty);
    exit;
}

$today = date('Y-m-d');
$yesterday = date('Y-m-d', strtotime('-1 day'));
$tomorrow = date('Y-m-d', strtotime('+1 day'));
$in30 = date('Y-m-d', strtotime('+30 days'));
$in7 = date('Y-m-d', strtotime('+7 days'));
$monthStart = date('Y-m-01');
$lastMonthStart = date('Y-m-01', strtotime('first day of last month'));
$lastMonthEnd = date('Y-m-t', strtotime('last day of last month'));
$sparkStart = date('Y-m-d', strtotime('-6 days'));

$custSql = "u.role_id='3' AND u.dealerId=?";
$licJoin = "FROM `licenses` l INNER JOIN `users` u ON u.id=l.userId WHERE {$custSql}";

$totalCustomer = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `users` u WHERE {$custSql}", 'i', $dealerId);

$activeCustomer = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE {$custSql} AND IFNULL(u.is_active,'1')='1'
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    'is',
    $dealerId,
    $today
);

$trialCustomer = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE {$custSql}
       AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    'is',
    $dealerId,
    $today
);

$expiredCustomer = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     WHERE {$custSql}
       AND NOT EXISTS (
         SELECT 1 FROM `licenses` l
         WHERE l.userId=u.id
           AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
           AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)
       )",
    'is',
    $dealerId,
    $today
);

$activeLicenses = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin}
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    'is',
    $dealerId,
    $today
);

$expiringLicenses = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin}
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND l.expiryDate IS NOT NULL AND l.expiryDate<>''
       AND l.expiryDate>=? AND l.expiryDate<=?",
    'iss',
    $dealerId,
    $today,
    $in30
);

$expiredLicenses = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin}
       AND (LOWER(IFNULL(l.licenseStatus,'')) IN ('expire','expired')
            OR (l.expiryDate IS NOT NULL AND l.expiryDate<>'' AND l.expiryDate<?))",
    'is',
    $dealerId,
    $today
);

$totalBranches = (string) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c {$licJoin}", 'i', $dealerId);

$trialLicenses = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin}
       AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    'is',
    $dealerId,
    $today
);

$expiringLicenses7Days = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin}
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND l.expiryDate IS NOT NULL AND l.expiryDate<>''
       AND l.expiryDate>=? AND l.expiryDate<=?",
    'iss',
    $dealerId,
    $today,
    $in7
);

$trialLicensesExpiringTomorrow = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin}
       AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND l.expiryDate IS NOT NULL AND l.expiryDate<>''
       AND DATE(l.expiryDate)=?",
    'is',
    $dealerId,
    $tomorrow
);

$customersAddedThisMonth = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `users` u WHERE {$custSql} AND u.created_at>=?",
    'is',
    $dealerId,
    $monthStart
);
$customersAddedLastMonth = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `users` u WHERE {$custSql} AND u.created_at>=? AND u.created_at<?",
    'iss',
    $dealerId,
    $lastMonthStart,
    $monthStart
);
$customersBeforeThisMonth = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `users` u WHERE {$custSql} AND (u.created_at IS NULL OR u.created_at<?)",
    'is',
    $dealerId,
    $monthStart
);

$branchesThisMonth = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin} AND l.created_at>=?",
    'is',
    $dealerId,
    $monthStart
);
$branchesLastMonth = (string) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c {$licJoin} AND l.created_at>=? AND l.created_at<?",
    'iss',
    $dealerId,
    $lastMonthStart,
    $monthStart
);

$invBase = "FROM `invoice` i
 INNER JOIN `licenses` l ON l.id=i.licenseId
 INNER JOIN `users` u ON u.id=l.userId
 WHERE {$custSql}" . invoice_and_not_refunded();

$monthSalesRow = db_stmt_fetch_one($con, "SELECT COALESCE(SUM(i.totalAmount),0) AS total {$invBase} AND DATE(i.invoiceDate)>=?", 'is', $dealerId, $monthStart);
$monthSales = (string) round((float) ($monthSalesRow['total'] ?? 0), 2);

$lastMonthSalesRow = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS total {$invBase} AND DATE(i.invoiceDate)>=? AND DATE(i.invoiceDate)<=?",
    'iss',
    $dealerId,
    $lastMonthStart,
    $lastMonthEnd
);
$lastMonthSales = (string) round((float) ($lastMonthSalesRow['total'] ?? 0), 2);

$todaySalesRow = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS total, COUNT(*) AS bills {$invBase} AND DATE(i.invoiceDate)=?",
    'is',
    $dealerId,
    $today
);
$todaySales = (string) round((float) ($todaySalesRow['total'] ?? 0), 2);
$billCount = (string) (int) ($todaySalesRow['bills'] ?? 0);

$yesterdaySalesRow = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS total {$invBase} AND DATE(i.invoiceDate)=?",
    'is',
    $dealerId,
    $yesterday
);
$yesterdaySales = (string) round((float) ($yesterdaySalesRow['total'] ?? 0), 2);

$sparkRows = db_stmt_fetch_all(
    $con,
    "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount),0) AS total
     {$invBase} AND DATE(i.invoiceDate)>=? AND DATE(i.invoiceDate)<=?
     GROUP BY DATE(i.invoiceDate)",
    'iss',
    $dealerId,
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

$total = (int) $totalCustomer;
$pct = function ($n) use ($total) {
    return $total > 0 ? (string) round(($n / $total) * 100.0, 1) : '0';
};

$netSalesTrend = dealer_pct_change($monthSales, $lastMonthSales);
$todaySalesTrend = dealer_pct_change($todaySales, $yesterdaySales);
$customersAddedTrend = dealer_pct_change($customersAddedThisMonth, $customersAddedLastMonth);
$totalCustomerTrend = dealer_pct_change($customersAddedThisMonth, $customersBeforeThisMonth);
$activeBranchesTrend = dealer_pct_change($branchesThisMonth, $branchesLastMonth);

$notificationCount = (string) (
    (int) $expiringLicenses7Days
    + (int) $expiredLicenses
    + (int) $trialLicensesExpiringTomorrow
);

$response = array(
    'status' => 'true',
    'message' => $totalCustomer,
    'totalCustomer' => $totalCustomer,
    'activeCustomer' => $activeCustomer,
    'trialCustomer' => $trialCustomer,
    'expiredCustomer' => $expiredCustomer,
    'activePercent' => $pct((int) $activeCustomer),
    'trialPercent' => $pct((int) $trialCustomer),
    'expiredPercent' => $pct((int) $expiredCustomer),
    'activeLicenses' => $activeLicenses,
    'expiringLicenses' => $expiringLicenses,
    'expiredLicenses' => $expiredLicenses,
    'totalBranches' => $totalBranches,
    'trialLicenses' => $trialLicenses,
    'expiringLicenses7Days' => $expiringLicenses7Days,
    'trialLicensesExpiringTomorrow' => $trialLicensesExpiringTomorrow,
    'customersAddedThisMonth' => $customersAddedThisMonth,
    'netSales' => $monthSales,
    'monthSales' => $monthSales,
    'todaySales' => $todaySales,
    'billCount' => $billCount,
    'notificationCount' => $notificationCount,
    'salesSparkline' => $salesSparkline,
    'netSalesTrend' => $netSalesTrend,
    'todaySalesTrend' => $todaySalesTrend,
    'customersAddedTrend' => $customersAddedTrend,
    'activeBranchesTrend' => $activeBranchesTrend,
    'totalCustomerTrend' => $totalCustomerTrend,
    'activeCustomerTrend' => $totalCustomerTrend,
    'trialCustomerTrend' => $customersAddedTrend,
    'expiredCustomerTrend' => dealer_pct_change($expiredCustomer, max((int) $expiredCustomer - (int) $customersAddedThisMonth, 0)),
    'activeLicensesTrend' => $activeBranchesTrend,
    'expiringLicensesTrend' => dealer_pct_change($expiringLicenses, max((int) $expiringLicenses - 1, 0)),
    'trialLicensesTrend' => $customersAddedTrend,
    'expiredLicensesTrend' => dealer_pct_change($expiredLicenses, max((int) $expiredLicenses - 1, 0)),
    'netSalesTrendLabel' => dealer_trend_label_short($netSalesTrend),
    'todaySalesTrendLabel' => dealer_trend_label_short($todaySalesTrend),
    'customersAddedTrendLabel' => dealer_trend_label_short($customersAddedTrend),
    'activeBranchesTrendLabel' => dealer_trend_label_short($activeBranchesTrend),
    'totalCustomerTrendLabel' => dealer_trend_label($totalCustomerTrend),
    'activeCustomerTrendLabel' => dealer_trend_label($totalCustomerTrend),
    'trialCustomerTrendLabel' => dealer_trend_label($customersAddedTrend),
    'expiredCustomerTrendLabel' => dealer_trend_label(dealer_pct_change($expiredCustomer, max((int) $expiredCustomer - (int) $customersAddedThisMonth, 0))),
    'activeLicensesTrendLabel' => dealer_trend_label($activeBranchesTrend),
    'expiringLicensesTrendLabel' => dealer_trend_label(dealer_pct_change($expiringLicenses, max((int) $expiringLicenses - 1, 0))),
    'trialLicensesTrendLabel' => dealer_trend_label($customersAddedTrend),
    'expiredLicensesTrendLabel' => dealer_trend_label(dealer_pct_change($expiredLicenses, max((int) $expiredLicenses - 1, 0))),
);

mysqli_close($con);
echo json_encode($response);
