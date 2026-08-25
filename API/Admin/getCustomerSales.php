<?php
/**
 * Admin: customer sales (invoices) — limited recent list.
 * GET: customerId, optional invoiceDate (Y-m-d)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '1', 'invoiceResponse' => array(), 'billCount' => '0', 'netSales' => '0');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, array('status' => '0', 'invoiceResponse' => array(), 'billCount' => '0', 'netSales' => '0'));
mysqli_query($con, 'set names utf8');

$customerId = isset($_GET['customerId']) ? trim($_GET['customerId']) : '';
$invoiceDate = isset($_GET['invoiceDate']) ? trim($_GET['invoiceDate']) : '';

if ($customerId === '') {
    $response['status'] = '0';
    $response['message'] = 'customerId required';
    echo json_encode($response);
    exit;
}

if ($invoiceDate !== '') {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT i.*, l.licenseKey, l.userName AS branchName
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         WHERE l.userId=? AND i.invoiceDate=?
         ORDER BY i.invoiceId DESC
         LIMIT 200",
        'is',
        (int) $customerId,
        $invoiceDate
    );
} else {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT i.*, l.licenseKey, l.userName AS branchName
         FROM `invoice` i
         INNER JOIN `licenses` l ON l.id = i.licenseId
         WHERE l.userId=?
         ORDER BY i.invoiceId DESC
         LIMIT 100",
        'i',
        (int) $customerId
    );
}

$net = 0.0;
foreach ($rows as $row) {
    $total = isset($row['totalAmount']) ? (float) $row['totalAmount'] : 0;
    $net += $total;
    $response['invoiceResponse'][] = array(
        'invoiceId' => (string) $row['invoiceId'],
        'invoiceNumber' => isset($row['invoiceNumber']) ? (string) $row['invoiceNumber'] : '',
        'invoiceDate' => isset($row['invoiceDate']) ? (string) $row['invoiceDate'] : '',
        'invoiceType' => isset($row['invoiceType']) ? (string) $row['invoiceType'] : '',
        'customerName' => isset($row['customerName']) ? (string) $row['customerName'] : '',
        'totalAmount' => isset($row['totalAmount']) ? (string) $row['totalAmount'] : '0',
        'discount' => isset($row['discount']) ? (string) $row['discount'] : '0',
        'totalGSTAmount' => isset($row['totalGSTAmount']) ? (string) $row['totalGSTAmount'] : '0',
        'paymentMode' => isset($row['paymentMode']) ? (string) $row['paymentMode'] : '',
        'licenseKey' => isset($row['licenseKey']) ? (string) $row['licenseKey'] : '',
        'branchName' => isset($row['branchName']) ? (string) $row['branchName'] : '',
        'invoiceNetworkStatus' => isset($row['invoiceNetworkStatus']) ? (string) $row['invoiceNetworkStatus'] : ''
    );
}

$response['billCount'] = (string) count($rows);
$response['netSales'] = (string) round($net, 2);
echo json_encode($response);
?>
