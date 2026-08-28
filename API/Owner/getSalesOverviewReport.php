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
    'topCustomers' => array(),
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

$month = isset($_GET['month']) ? trim($_GET['month']) : date('Y-m');
if (!preg_match('/^\d{4}-\d{2}$/', $month)) {
    $month = date('Y-m');
}
$monthStart = $month . '-01';
$monthEnd = date('Y-m-t', strtotime($monthStart));
$prevStart = date('Y-m-01', strtotime($monthStart . ' -1 month'));
$prevEnd = date('Y-m-t', strtotime($prevStart));
$join = owner_sales_invoice_join();

$curSql = "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales, COUNT(*) AS totalInvoices
           FROM `invoice` i $join
           WHERE {$scope['where']} AND DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded();
$periodTypes = $scope['types'] . 'ss';
$curParams = array_merge($scope['params'], array($monthStart, $monthEnd));
$cur = db_stmt_fetch_one($con, $curSql, $periodTypes, ...$curParams);

$prevParams = array_merge($scope['params'], array($prevStart, $prevEnd));
$prev = db_stmt_fetch_one($con, $curSql, $periodTypes, ...$prevParams);

$totalSales = (float) ($cur['totalSales'] ?? 0);
$invoices = (int) ($cur['totalInvoices'] ?? 0);
$avgBill = $invoices > 0 ? round($totalSales / $invoices, 2) : 0;
$pSales = (float) ($prev['totalSales'] ?? 0);
$pInv = (int) ($prev['totalInvoices'] ?? 0);
$pAvg = $pInv > 0 ? $pSales / $pInv : 0;

$trendSql = "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount),0) AS total
             FROM `invoice` i $join
             WHERE {$scope['where']} AND DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded() . "
             GROUP BY DATE(i.invoiceDate) ORDER BY d ASC";
$trendRows = db_stmt_fetch_all($con, $trendSql, $periodTypes, ...$curParams);
$salesTrend = array();
foreach ($trendRows as $tr) {
    $salesTrend[] = array(
        'date' => (string) $tr['d'],
        'total' => (string) round((float) $tr['total'], 2),
    );
}

$topSql = "SELECT l.id AS branchId, l.userName AS customerName,
                  COALESCE(c.shopName1, c.companyName, l.userName) AS shopName,
                  COALESCE(SUM(i.totalAmount),0) AS totalSales, COUNT(*) AS billCount
           FROM `invoice` i $join
           LEFT JOIN `companys` c ON c.licenseId = l.id
           WHERE {$scope['where']} AND DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded() . "
           GROUP BY l.id, l.userName, c.shopName1, c.companyName
           ORDER BY totalSales DESC LIMIT 5";
$topRows = db_stmt_fetch_all($con, $topSql, $periodTypes, ...$curParams);
$topCustomers = array();
foreach ($topRows as $tr) {
    $topCustomers[] = array(
        'branchId' => (string) $tr['branchId'],
        'customerName' => (string) ($tr['customerName'] ?? ''),
        'shopName' => (string) ($tr['shopName'] ?? ''),
        'totalSales' => (string) round((float) $tr['totalSales'], 2),
        'count' => (string) (int) ($tr['billCount'] ?? 0),
    );
}

$branchCount = db_stmt_scalar_string(
    $con,
    'SELECT COUNT(*) FROM `licenses` WHERE `userId`=?',
    's',
    (string) $userId
);

$response = array(
    'status' => 'true',
    'periodLabel' => owner_sales_period_label(
        $branchId,
        $branchLabel,
        'This Month (' . date('d', strtotime($monthStart)) . ' - ' . date('d M Y', strtotime($monthEnd)) . ')'
    ),
    'totalSales' => (string) round($totalSales, 2),
    'netSales' => (string) round($totalSales, 2),
    'totalInvoices' => (string) $invoices,
    'avgBill' => (string) $avgBill,
    'totalSalesTrend' => owner_sales_trend_pct($totalSales, $pSales) . ' vs last month',
    'netSalesTrend' => owner_sales_trend_pct($totalSales, $pSales) . ' vs last month',
    'invoicesTrend' => owner_sales_trend_pct($invoices, $pInv) . ' vs last month',
    'avgBillTrend' => owner_sales_trend_pct($avgBill, $pAvg) . ' vs last month',
    'salesTrend' => $salesTrend,
    'topCustomers' => $topCustomers,
    'branchCount' => (string) (int) $branchCount,
);

mysqli_close($con);
echo json_encode($response);
?>
