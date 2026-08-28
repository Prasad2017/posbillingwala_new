<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

header('Content-Type: application/json; charset=utf-8');
$response = array(
    'status' => 'true',
    'periodLabel' => 'Today, ' . date('d M Y'),
    'totalSales' => '0',
    'netSales' => '0',
    'totalInvoices' => '0',
    'avgBill' => '0',
    'totalSalesTrend' => '0%',
    'netSalesTrend' => '0%',
    'invoicesTrend' => '0%',
    'avgBillTrend' => '0%',
    'salesTrend' => array(),
    'recentInvoices' => array()
);
admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');
$today = date('Y-m-d');
$yesterday = date('Y-m-d', strtotime('-1 day'));
$weekStart = date('Y-m-d', strtotime('-6 days'));

$cur = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales, COUNT(*) AS bills
     FROM invoice i
     INNER JOIN licenses l ON l.id=i.licenseId
     INNER JOIN users u ON u.id=l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate)=?" . invoice_and_not_refunded(),
    's',
    $today
);
$prev = db_stmt_fetch_one(
    $con,
    "SELECT COALESCE(SUM(i.totalAmount),0) AS totalSales, COUNT(*) AS bills
     FROM invoice i
     INNER JOIN licenses l ON l.id=i.licenseId
     INNER JOIN users u ON u.id=l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate)=?" . invoice_and_not_refunded(),
    's',
    $yesterday
);
$total = (float) ($cur['totalSales'] ?? 0);
$bills = (int) ($cur['bills'] ?? 0);
$avg = $bills > 0 ? round($total / $bills, 2) : 0;
$pTotal = (float) ($prev['totalSales'] ?? 0);
$pBills = (int) ($prev['bills'] ?? 0);
$pAvg = $pBills > 0 ? $pTotal / $pBills : 0;
$pct = function ($c, $p) {
    if ($p == 0) return $c > 0 ? '+100%' : '0%';
    $v = round((($c - $p) / $p) * 100, 1);
    return ($v >= 0 ? '+' : '') . $v . '%';
};

$trend = array();
$rows = db_stmt_fetch_all(
    $con,
    "SELECT DATE(i.invoiceDate) AS d, COALESCE(SUM(i.totalAmount),0) AS total
     FROM invoice i
     INNER JOIN licenses l ON l.id=i.licenseId
     INNER JOIN users u ON u.id=l.userId AND u.role_id='3'
     WHERE DATE(i.invoiceDate)>=? AND DATE(i.invoiceDate)<=?" . invoice_and_not_refunded() . "
     GROUP BY DATE(i.invoiceDate) ORDER BY d",
    'ss',
    $weekStart,
    $today
);
foreach ($rows as $r) {
    $trend[] = array('date' => (string) $r['d'], 'total' => (string) round((float) $r['total'], 2));
}

$recent = array();
$inv = db_stmt_fetch_all(
    $con,
    "SELECT i.invoiceId, i.invoiceNumber, i.invoiceDate, i.totalAmount, u.name AS customerName, u.shopName
     FROM invoice i
     INNER JOIN licenses l ON l.id=i.licenseId
     INNER JOIN users u ON u.id=l.userId AND u.role_id='3'
     ORDER BY i.invoiceId DESC LIMIT 8",
    ''
);
foreach ($inv as $r) {
    $recent[] = array(
        'invoiceId' => (string) $r['invoiceId'],
        'invoiceNumber' => (string) ($r['invoiceNumber'] ?? ''),
        'invoiceDate' => (string) ($r['invoiceDate'] ?? ''),
        'totalAmount' => (string) round((float) ($r['totalAmount'] ?? 0), 2),
        'customerName' => (string) ($r['customerName'] ?? ''),
        'shopName' => (string) ($r['shopName'] ?? ''),
        'paymentStatus' => 'Paid'
    );
}

$response = array(
    'status' => 'true',
    'periodLabel' => 'Today, ' . date('d M Y'),
    'totalSales' => (string) round($total, 2),
    'netSales' => (string) round($total, 2),
    'totalInvoices' => (string) $bills,
    'avgBill' => (string) $avg,
    'totalSalesTrend' => $pct($total, $pTotal),
    'netSalesTrend' => $pct($total, $pTotal),
    'invoicesTrend' => $pct($bills, $pBills),
    'avgBillTrend' => $pct($avg, $pAvg),
    'salesTrend' => $trend,
    'recentInvoices' => $recent
);
mysqli_close($con);
echo json_encode($response);
