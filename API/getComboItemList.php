<?php
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('comboItemResponse' => array());

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
    "SELECT ci.*, c.comboNetworkStatus, p.productNetworkStatus, p.productName,
            pp.portionNetworkStatus, pp.portionName
     FROM `combo_items` ci
     INNER JOIN `combos` c ON c.comboId = ci.comboId
     LEFT JOIN `products` p ON p.productId = ci.productId
     LEFT JOIN `product_portions` pp ON pp.portionId = ci.portionId
     WHERE c.userId=?
     ORDER BY ci.comboId ASC, ci.comboItemSortOrder ASC, ci.comboItemId ASC",
    's',
    $userId
);

foreach ($rows as $row) {
    $item = array();
    $item['comboItemId'] = (string) $row['comboItemId'];
    $item['comboId'] = (string) $row['comboId'];
    $item['productId'] = isset($row['productId']) ? (string) $row['productId'] : '';
    $item['portionId'] = isset($row['portionId']) ? (string) $row['portionId'] : '';
    $item['comboItemQuantity'] = (string) $row['comboItemQuantity'];
    $item['comboItemSortOrder'] = isset($row['comboItemSortOrder']) ? (string) $row['comboItemSortOrder'] : '0';
    $item['comboItemDeletedStatus'] = (isset($row['comboItemStatus']) && $row['comboItemStatus'] === 'active') ? '0' : '1';
    $item['comboItemNetworkStatus'] = (string) $row['comboItemNetworkStatus'];
    $item['comboNetworkStatus'] = isset($row['comboNetworkStatus']) ? (string) $row['comboNetworkStatus'] : '';
    $item['productNetworkStatus'] = isset($row['productNetworkStatus']) ? (string) $row['productNetworkStatus'] : '';
    $item['portionNetworkStatus'] = isset($row['portionNetworkStatus']) ? (string) $row['portionNetworkStatus'] : '';
    $item['productName'] = isset($row['productName']) ? (string) $row['productName'] : '';
    $item['portionName'] = isset($row['portionName']) ? (string) $row['portionName'] : '';
    $response['comboItemResponse'][] = $item;
}

echo json_encode($response);
