<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/owner_sales_helpers.php';

header('Content-Type: application/json; charset=utf-8');
date_default_timezone_set('Asia/Kolkata');

$response = array(
    'status' => 'false',
    'periodLabel' => '',
    'totalSales' => '0',
    'netSales' => '0',
    'totalInvoices' => '0',
    'avgBill' => '0',
    'totalSalesTrend' => '0%',
    'netSalesTrend' => '0%',
    'invoicesTrend' => '0%',
    'avgBillTrend' => '0%',
    'salesTrend' => array(),
    'recentInvoices' => array(),
    'branchCount' => '0',
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

owner_require_auth($con);
mysqli_query($con, 'set names utf8');

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null || $userId === '') {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    exit;
}

$branchId = isset($_GET['branchId']) ? trim($_GET['branchId']) : '';
$scope = owner_sales_scope_bind($con, $userId, $branchId);
if ($scope === null) {
    $response['message'] = 'Invalid branch';
    echo json_encode($response);
    exit;
}

$branchLabel = '';
if ($branchId !== '' && strtolower($branchId) !== 'all' && $branchId !== '0') {
    require_once __DIR__ . '/../licence_expiry.php';
    $lic = db_stmt_fetch_one($con, 'SELECT * FROM `licenses` WHERE `id`=? LIMIT 1', 'i', (int) $branchId);
    if ($lic !== null) {
        $bf = licence_branch_fields($lic);
        $branchLabel = $bf['branchLabel'];
    }
}

$branchCount = db_stmt_scalar_string(
    $con,
    'SELECT COUNT(*) FROM `licenses` WHERE `userId`=?',
    's',
    (string) $userId
);

$today = date('Y-m-d');
$yesterday = date('Y-m-d', strtotime('-1 day'));
$weekStart = date('Y-m-d', strtotime('-6 days'));
$join = owner_sales_invoice_join();

$curSql = "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales, COUNT(*) AS bills
           FROM `invoice` i $join WHERE {$scope['where']} AND DATE(i.invoiceDate)=?" . invoice_and_not_refunded();
$curTypes = $scope['types'] . 's';
$curParams = array_merge($scope['params'], array($today));
$cur = db_stmt_fetch_one($con, $curSql, $curTypes, ...$curParams);

$prevSql = "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales, COUNT(*) AS bills
            FROM `invoice` i $join WHERE {$scope['where']} AND DATE(i.invoiceDate)=?" . invoice_and_not_refunded();
$prevParams = array_merge($scope['params'], array($yesterday));
$prev = db_stmt_fetch_one($con, $prevSql, $curTypes, ...$prevParams);

$total = (float) ($cur['totalSales'] ?? 0);
$bills = (int) ($cur['bills'] ?? 0);
$avg = $bills > 0 ? round($total / $bills, 2) : 0;
$pTotal = (float) ($prev['totalSales'] ?? 0);
$pBills = (int) ($prev['bills'] ?? 0);
$pAvg = $pBills > 0 ? $pTotal / $pBills : 0;

$trendSql = "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount),0) AS total
             FROM `invoice` i $join
             WHERE {$scope['where']} AND DATE(i.invoiceDate)>=? AND DATE(i.invoiceDate)<=?" . invoice_and_not_refunded() . "
             GROUP BY DATE(i.invoiceDate) ORDER BY d";
$trendTypes = $scope['types'] . 'ss';
$trendParams = array_merge($scope['params'], array($weekStart, $today));
$trendRows = db_stmt_fetch_all($con, $trendSql, $trendTypes, ...$trendParams);
$salesTrend = array();
foreach ($trendRows as $r) {
    $salesTrend[] = array(
        'date' => (string) $r['d'],
        'total' => (string) round((float) $r['total'], 2),
    );
}

$recentSql = "SELECT i.invoiceId, i.invoiceNumber, i.invoiceDate, i.totalAmount, i.paymentMode,
                     l.userName AS branchName
              FROM `invoice` i $join
              WHERE {$scope['where']}
              ORDER BY i.invoiceId DESC LIMIT 8";
$recentRows = db_stmt_fetch_all($con, $recentSql, $scope['types'], ...$scope['params']);
$recent = array();
foreach ($recentRows as $r) {
    $recent[] = array(
        'invoiceId' => (string) $r['invoiceId'],
        'invoiceNumber' => (string) ($r['invoiceNumber'] ?? ''),
        'invoiceDate' => (string) ($r['invoiceDate'] ?? ''),
        'totalAmount' => (string) round((float) ($r['totalAmount'] ?? 0), 2),
        'customerName' => (string) ($r['branchName'] ?? ''),
        'paymentStatus' => !empty($r['paymentMode']) ? (string) $r['paymentMode'] : 'Paid',
    );
}

$response = array(
    'status' => 'true',
    'periodLabel' => owner_sales_period_label($branchId, $branchLabel, 'Today, ' . date('d M Y')),
    'totalSales' => (string) round($total, 2),
    'netSales' => (string) round($total, 2),
    'totalInvoices' => (string) $bills,
    'avgBill' => (string) $avg,
    'totalSalesTrend' => owner_sales_trend_pct($total, $pTotal),
    'netSalesTrend' => owner_sales_trend_pct($total, $pTotal),
    'invoicesTrend' => owner_sales_trend_pct($bills, $pBills),
    'avgBillTrend' => owner_sales_trend_pct($avg, $pAvg),
    'salesTrend' => $salesTrend,
    'recentInvoices' => $recent,
    'branchCount' => (string) (int) $branchCount,
);

mysqli_close($con);
echo json_encode($response);
?>
