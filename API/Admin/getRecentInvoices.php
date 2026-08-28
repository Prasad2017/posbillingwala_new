<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../invoice_sales_filter.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'invoiceResponse' => array(), 'billCount' => '0', 'netSales' => '0', 'totalSales' => '0');
admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');

$limit = isset($_GET['limit']) ? (int) $_GET['limit'] : 50;
if ($limit < 1) $limit = 50;
if ($limit > 200) $limit = 200;
$q = isset($_GET['q']) ? trim($_GET['q']) : '';

$sql = "SELECT i.invoiceId, i.invoiceNumber, i.invoiceDate, i.totalAmount, i.invoiceOrderStatus, u.name AS customerName, u.shopName
        FROM invoice i
        INNER JOIN licenses l ON l.id=i.licenseId
        INNER JOIN users u ON u.id=l.userId AND u.role_id='3'";
if ($q !== '') {
    $sql .= " WHERE i.invoiceNumber LIKE ? OR u.name LIKE ? OR u.shopName LIKE ?";
    $like = '%' . $q . '%';
    $rows = db_stmt_fetch_all($con, $sql . " ORDER BY i.invoiceId DESC LIMIT " . $limit, 'sss', $like, $like, $like);
} else {
    $rows = db_stmt_fetch_all($con, $sql . " ORDER BY i.invoiceId DESC LIMIT " . $limit, '');
}

$total = 0.0;
$list = array();
foreach ($rows as $r) {
    $amt = (float) ($r['totalAmount'] ?? 0);
    $orderStatus = isset($r['invoiceOrderStatus']) ? (string) $r['invoiceOrderStatus'] : 'completed';
    if (strcasecmp($orderStatus, 'refunded') !== 0) {
        $total += $amt;
    }
    $list[] = array(
        'invoiceId' => (string) $r['invoiceId'],
        'invoiceNumber' => (string) ($r['invoiceNumber'] ?? ''),
        'invoiceDate' => (string) ($r['invoiceDate'] ?? ''),
        'totalAmount' => (string) round($amt, 2),
        'customerName' => (string) ($r['customerName'] ?? ''),
        'shopName' => (string) ($r['shopName'] ?? ''),
        'paymentStatus' => strcasecmp($orderStatus, 'refunded') === 0 ? 'Refunded' : 'Paid',
        'invoiceOrderStatus' => $orderStatus
    );
}
$response['invoiceResponse'] = $list;
$response['billCount'] = (string) count($list);
$response['totalSales'] = (string) round($total, 2);
$response['netSales'] = (string) round($total, 2);
mysqli_close($con);
echo json_encode($response);
