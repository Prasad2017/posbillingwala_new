<?php
/**
 * Admin Sales Overview report (month KPIs, trend, top customers).
 * GET optional: month=YYYY-MM
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

header('Content-Type: application/json; charset=utf-8');

function so_pct($c, $p) {
    $c = (float) $c;
    $p = (float) $p;
    if ($p == 0.0) {
        return $c > 0 ? '+100.0%' : '0%';
    }
    $v = round((($c - $p) / $p) * 100.0, 1);
    return ($v >= 0 ? '+' : '') . $v . '%';
}

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
    'topCustomers' => array()
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');

$month = isset($_GET['month']) ? trim($_GET['month']) : date('Y-m');
if (!preg_match('/^\d{4}-\d{2}$/', $month)) {
    $month = date('Y-m');
}
$monthStart = $month . '-01';
$monthEnd = date('Y-m-t', strtotime($monthStart));
$prevStart = date('Y-m-01', strtotime($monthStart . ' -1 month'));
$prevEnd = date('Y-m-t', strtotime($prevStart));

$cur = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales,
            COALESCE(SUM(i.totalAmount),0) AS netSales,
            COUNT(*) AS totalInvoices
     FROM `invoice` i
     INNER JOIN `licenses` l ON l.id = i.licenseId
     INNER JOIN `users` u ON u.id = l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded(),
    'ss',
    $monthStart,
    $monthEnd
);
$prev = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales,
            COALESCE(SUM(i.totalAmount),0) AS netSales,
            COUNT(*) AS totalInvoices
     FROM `invoice` i
     INNER JOIN `licenses` l ON l.id = i.licenseId
     INNER JOIN `users` u ON u.id = l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded(),
    'ss',
    $prevStart,
    $prevEnd
);

$totalSales = (float) ($cur['totalSales'] ?? 0);
$netSales = (float) ($cur['netSales'] ?? 0);
$invoices = (int) ($cur['totalInvoices'] ?? 0);
$avgBill = $invoices > 0 ? round($totalSales / $invoices, 2) : 0;
$pSales = (float) ($prev['totalSales'] ?? 0);
$pNet = (float) ($prev['netSales'] ?? 0);
$pInv = (int) ($prev['totalInvoices'] ?? 0);
$pAvg = $pInv > 0 ? $pSales / $pInv : 0;

$trendRows = db_stmt_fetch_all(
    $con,
    "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount),0) AS total
     FROM `invoice` i
     INNER JOIN `licenses` l ON l.id = i.licenseId
     INNER JOIN `users` u ON u.id = l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded() . "
     GROUP BY DATE(i.invoiceDate)
     ORDER BY d ASC",
    'ss',
    $monthStart,
    $monthEnd
);
$salesTrend = array();
foreach ($trendRows as $tr) {
    $salesTrend[] = array(
        'date' => (string) $tr['d'],
        'total' => (string) round((float) $tr['total'], 2)
    );
}

$topRows = db_stmt_fetch_all(
    $con,
    "SELECT u.id AS customerId, u.name AS customerName, u.shopName,
            COALESCE(SUM(i.totalAmount),0) AS totalSales
     FROM `invoice` i
     INNER JOIN `licenses` l ON l.id = i.licenseId
     INNER JOIN `users` u ON u.id = l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate) >= ? AND DATE(i.invoiceDate) <= ?" . invoice_and_not_refunded() . "
     GROUP BY u.id, u.name, u.shopName
     ORDER BY totalSales DESC
     LIMIT 5",
    'ss',
    $monthStart,
    $monthEnd
);
$topCustomers = array();
foreach ($topRows as $tr) {
    $topCustomers[] = array(
        'customerId' => (string) $tr['customerId'],
        'customerName' => (string) ($tr['customerName'] ?? ''),
        'shopName' => (string) ($tr['shopName'] ?? ''),
        'totalSales' => (string) round((float) $tr['totalSales'], 2)
    );
}

$response = array(
    'status' => 'true',
    'periodLabel' => 'This Month (' . date('d', strtotime($monthStart)) . ' - ' . date('d M Y', strtotime($monthEnd)) . ')',
    'totalSales' => (string) round($totalSales, 2),
    'netSales' => (string) round($netSales, 2),
    'totalInvoices' => (string) $invoices,
    'avgBill' => (string) $avgBill,
    'totalSalesTrend' => so_pct($totalSales, $pSales) . ' vs last month',
    'netSalesTrend' => so_pct($netSales, $pNet) . ' vs last month',
    'invoicesTrend' => so_pct($invoices, $pInv) . ' vs last month',
    'avgBillTrend' => so_pct($avgBill, $pAvg) . ' vs last month',
    'salesTrend' => $salesTrend,
    'topCustomers' => $topCustomers
);

mysqli_close($con);
echo json_encode($response);
