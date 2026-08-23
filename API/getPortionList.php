<?php
/**
 * Product portions for a shop — each row is Product + Portion Master + price.
 */
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('portionResponse' => array());

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
    "SELECT pp.*, p.productNetworkStatus,
            COALESCE(pm.portionName, pp.portionName) AS resolvedPortionName,
            pm.portionMasterNetworkStatus
     FROM `product_portions` pp
     INNER JOIN `products` p ON p.productId = pp.productId
     LEFT JOIN `portion_master` pm ON pm.portionMasterId = pp.portionMasterId
     WHERE p.userId=?
     ORDER BY pp.portionId ASC",
    's',
    $userId
);

foreach ($rows as $row) {
    $item = array();
    $item['portionId'] = (string) $row['portionId'];
    $item['productId'] = (string) $row['productId'];
    $item['productNetworkStatus'] = $row['productNetworkStatus'];
    $item['portionMasterId'] = isset($row['portionMasterId']) && $row['portionMasterId'] !== null
        ? (string) $row['portionMasterId'] : '';
    $item['portionMasterNetworkStatus'] = isset($row['portionMasterNetworkStatus'])
        ? (string) $row['portionMasterNetworkStatus'] : '';
    $item['portionName'] = $row['resolvedPortionName'];
    $item['portionPrice'] = (string) $row['portionPrice'];
    $item['portionSortOrder'] = (string) $row['portionSortOrder'];
    $item['portionNetworkStatus'] = $row['portionNetworkStatus'];
    $item['portionDeletedStatus'] = ($row['portionStatus'] === 'active') ? '0' : '1';
    $response['portionResponse'][] = $item;
}

echo json_encode($response);
