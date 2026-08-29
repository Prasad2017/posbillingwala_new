<?php
/**
 * Dealer dashboard — customer-wise sales for this month (donut chart).
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
    'period' => 'month',
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['status'] = 'false';
    echo json_encode($response);
    exit;
}

dealer_require_auth($con, array('status' => 'false', 'dealerSalesResponse' => array(), 'totalSales' => '0'));
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');

$dealerId = isset($_GET['userId']) ? (int) $_GET['userId'] : 0;
if ($dealerId <= 0) {
    echo json_encode($response);
    exit;
}

$today = date('Y-m-d');
$monthStart = date('Y-m-01');
$limit = isset($_GET['limit']) ? (int) $_GET['limit'] : 8;
if ($limit < 1) {
    $limit = 8;
}
if ($limit > 50) {
    $limit = 50;
}

$rows = db_stmt_fetch_all(
    $con,
    "SELECT c.id AS customerId,
            COALESCE(NULLIF(c.shopName,''), c.name, 'Customer') AS customerName,
            COUNT(DISTINCT CASE
                WHEN LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
                 AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)
                THEN l.id END) AS activeLicenses,
            COALESCE(SUM(CASE WHEN i.invoiceDate IS NOT NULL AND DATE(i.invoiceDate)>=? THEN i.totalAmount ELSE 0 END), 0) AS totalSales
     FROM `users` c
     LEFT JOIN `licenses` l ON l.userId=c.id
     LEFT JOIN `invoice` i ON i.licenseId=l.id" . invoice_and_not_refunded() . "
     WHERE c.role_id='3' AND c.dealerId=?
     GROUP BY c.id, c.shopName, c.name
     ORDER BY totalSales DESC, customerName ASC
     LIMIT " . (int) $limit,
    'ssi',
    $today,
    $monthStart,
    $dealerId
);

$grandTotal = 0.0;
foreach ($rows as $row) {
    $sales = isset($row['totalSales']) ? (float) $row['totalSales'] : 0.0;
    $grandTotal += $sales;
    $response['dealerSalesResponse'][] = array(
        'dealerId' => (string) $row['customerId'],
        'dealerName' => isset($row['customerName']) ? (string) $row['customerName'] : 'Customer',
        'totalCustomer' => '1',
        'activeLicenses' => isset($row['activeLicenses']) ? (string) $row['activeLicenses'] : '0',
        'totalSales' => (string) round($sales, 2),
    );
}

$response['totalSales'] = (string) round($grandTotal, 2);
mysqli_close($con);
echo json_encode($response);
