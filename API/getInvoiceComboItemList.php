<?php
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('invoiceComboItemResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? trim((string) $_GET['userId']) : '';
if ($userId === '') {
    echo json_encode($response);
    exit;
}

$rows = db_stmt_fetch_all(
    $con,
    "SELECT ici.*, p.productNetworkStatus, pp.portionNetworkStatus
     FROM `invoice_combo_items` ici
     INNER JOIN `invoice` inv ON inv.`invoiceNumber` = ici.`invoiceNumber`
     LEFT JOIN `products` p ON p.productId = ici.productId
     LEFT JOIN `product_portions` pp ON pp.portionId = ici.portionId
     WHERE inv.`licenseId`=?
     ORDER BY ici.`invoiceComboItemId` ASC",
    's',
    $userId
);

foreach ($rows as $row) {
    $item = array();
    $item['invoiceComboItemId'] = (string) $row['invoiceComboItemId'];
    $item['invoiceNumber'] = (string) $row['invoiceNumber'];
    $item['invoiceProductNetworkStatus'] = isset($row['invoiceProductNetworkStatus']) ? (string) $row['invoiceProductNetworkStatus'] : '';
    $item['comboNetworkStatus'] = isset($row['comboNetworkStatus']) ? (string) $row['comboNetworkStatus'] : '';
    $item['productId'] = isset($row['productId']) ? (string) $row['productId'] : '';
    $item['productName'] = isset($row['productNameSnapshot']) ? (string) $row['productNameSnapshot'] : '';
    $item['portionId'] = isset($row['portionId']) ? (string) $row['portionId'] : '';
    $item['portionName'] = isset($row['portionNameSnapshot']) ? (string) $row['portionNameSnapshot'] : '';
    $item['comboItemQuantity'] = isset($row['quantity']) ? (string) $row['quantity'] : '1';
    $item['comboItemSortOrder'] = isset($row['sortOrder']) ? (string) $row['sortOrder'] : '0';
    $item['invoiceComboItemNetworkStatus'] = (string) $row['invoiceComboItemNetworkStatus'];
    $item['productNetworkStatus'] = isset($row['productNetworkStatus']) ? (string) $row['productNetworkStatus'] : '';
    $item['portionNetworkStatus'] = isset($row['portionNetworkStatus']) ? (string) $row['portionNetworkStatus'] : '';
    $response['invoiceComboItemResponse'][] = $item;
}

echo json_encode($response);
