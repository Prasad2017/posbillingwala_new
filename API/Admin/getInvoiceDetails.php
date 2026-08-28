<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false', 'items' => array());
admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');

$invoiceId = isset($_GET['invoiceId']) ? (int) $_GET['invoiceId'] : 0;
if ($invoiceId <= 0) {
    $response['message'] = 'invoiceId required';
    echo json_encode($response);
    exit;
}

$inv = db_stmt_fetch_one(
    $con,
    "SELECT i.*, u.name AS customerName, u.shopName
     FROM invoice i
     INNER JOIN licenses l ON l.id=i.licenseId
     INNER JOIN users u ON u.id=l.userId
     WHERE i.invoiceId=? LIMIT 1",
    'i',
    $invoiceId
);
if ($inv === null) {
    $response['message'] = 'Invoice not found';
    echo json_encode($response);
    exit;
}

$items = array();
// Try common item table names gracefully
$try = mysqli_query($con, "SHOW TABLES LIKE 'invoice_items'");
if ($try && mysqli_num_rows($try) > 0) {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT * FROM invoice_items WHERE invoiceId=? OR invoice_id=? LIMIT 200",
        'ii',
        $invoiceId,
        $invoiceId
    );
    foreach ($rows as $r) {
        $items[] = array(
            'label' => (string) ($r['productName'] ?? $r['itemName'] ?? $r['name'] ?? 'Item'),
            'count' => (string) ($r['qty'] ?? $r['quantity'] ?? '1'),
            'amount' => (string) ($r['amount'] ?? $r['total'] ?? '0'),
            'name' => (string) ($r['productName'] ?? $r['itemName'] ?? $r['name'] ?? 'Item')
        );
    }
}

$subtotal = (float) ($inv['totalAmount'] ?? 0);
$response = array(
    'status' => 'true',
    'invoiceId' => (string) $inv['invoiceId'],
    'invoiceNumber' => (string) ($inv['invoiceNumber'] ?? ''),
    'invoiceDate' => (string) ($inv['invoiceDate'] ?? ''),
    'customerName' => (string) ($inv['customerName'] ?? ''),
    'shopName' => (string) ($inv['shopName'] ?? ''),
    'paymentStatus' => 'Paid',
    'paymentMethod' => (string) ($inv['paymentMode'] ?? $inv['paymentMethod'] ?? 'UPI'),
    'cashierName' => (string) ($inv['cashierName'] ?? '—'),
    'subtotal' => (string) round($subtotal, 2),
    'discount' => '0',
    'tax' => '0',
    'totalAmount' => (string) round($subtotal, 2),
    'paidAmount' => (string) round($subtotal, 2),
    'items' => $items
);
mysqli_close($con);
echo json_encode($response);
