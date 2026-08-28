<?php
/**
 * Admin dashboard — dealer-wise sales for this month (donut chart / reports).
 * GET optional: limit (default 10, max 50), period=month|all (default month)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

header('Content-Type: application/json; charset=utf-8');
$response = array(
    'status' => 'true',
    'dealerSalesResponse' => array(),
    'totalSales' => '0',
    'period' => 'month'
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['status'] = 'false';
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => 'false', 'dealerSalesResponse' => array(), 'totalSales' => '0'));
mysqli_query($con, 'set names utf8');

date_default_timezone_set('Asia/Kolkata');
$today = date('Y-m-d');
$monthStart = date('Y-m-01');

$limit = isset($_GET['limit']) ? (int) $_GET['limit'] : 10;
if ($limit < 1) {
    $limit = 10;
}
if ($limit > 50) {
    $limit = 50;
}

$period = isset($_GET['period']) ? strtolower(trim($_GET['period'])) : 'month';
if ($period !== 'all' && $period !== 'month') {
    $period = 'month';
}
$response['period'] = $period;

if ($period === 'all') {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT d.id AS dealerId, d.name AS dealerName,
                COUNT(DISTINCT c.id) AS totalCustomer,
                COUNT(DISTINCT CASE
                    WHEN LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
                     AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)
                    THEN l.id END) AS activeLicenses,
                COALESCE(SUM(i.totalAmount), 0) AS totalSales
         FROM `users` d
         LEFT JOIN `users` c ON c.dealerId = d.id AND c.role_id = '3'
         LEFT JOIN `licenses` l ON l.userId = c.id
         LEFT JOIN `invoice` i ON i.licenseId = l.id" . invoice_and_not_refunded() . "
         WHERE d.role_id = '2'
         GROUP BY d.id, d.name
         ORDER BY totalSales DESC, totalCustomer DESC
         LIMIT " . (int) $limit,
        's',
        $today
    );
} else {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT d.id AS dealerId, d.name AS dealerName,
                COUNT(DISTINCT c.id) AS totalCustomer,
                COUNT(DISTINCT CASE
                    WHEN LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
                     AND (l.expiryDate IS NULL OR l.expiryDate = '' OR l.expiryDate >= ?)
                    THEN l.id END) AS activeLicenses,
                COALESCE(SUM(CASE WHEN i.invoiceDate IS NOT NULL AND DATE(i.invoiceDate) >= ? THEN i.totalAmount ELSE 0 END), 0) AS totalSales
         FROM `users` d
         LEFT JOIN `users` c ON c.dealerId = d.id AND c.role_id = '3'
         LEFT JOIN `licenses` l ON l.userId = c.id
         LEFT JOIN `invoice` i ON i.licenseId = l.id" . invoice_and_not_refunded() . "
         WHERE d.role_id = '2'
         GROUP BY d.id, d.name
         ORDER BY totalSales DESC, totalCustomer DESC
         LIMIT " . (int) $limit,
        'ss',
        $today,
        $monthStart
    );
}

$grandTotal = 0.0;
foreach ($rows as $row) {
    $sales = isset($row['totalSales']) ? (float) $row['totalSales'] : 0.0;
    $grandTotal += $sales;
    $response['dealerSalesResponse'][] = array(
        'dealerId' => (string) $row['dealerId'],
        'dealerName' => isset($row['dealerName']) ? (string) $row['dealerName'] : '',
        'totalCustomer' => isset($row['totalCustomer']) ? (string) $row['totalCustomer'] : '0',
        'activeLicenses' => isset($row['activeLicenses']) ? (string) $row['activeLicenses'] : '0',
        'totalSales' => (string) round($sales, 2)
    );
}

$response['totalSales'] = (string) round($grandTotal, 2);

mysqli_close($con);
echo json_encode($response);
