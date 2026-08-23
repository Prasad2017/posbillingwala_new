<?php
/**
 * Owner: Product portions list.
 */
include_once('config.php');
require_once __DIR__ . '/../auth_tokens.php';
require_once dirname(__DIR__) . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('portionResponse' => array());

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

$userId = isset($_GET['userId']) ? trim((string) $_GET['userId']) : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null) {
    echo json_encode($response);
    exit;
}

$productId = isset($_GET['productId']) ? trim((string) $_GET['productId']) : '';

$sql = "SELECT pp.*, p.productNetworkStatus,
               COALESCE(pm.portionName, pp.portionName) AS resolvedPortionName,
               pm.portionMasterId AS masterId
        FROM `product_portions` pp
        INNER JOIN `products` p ON p.productId = pp.productId
        LEFT JOIN `portion_master` pm ON pm.portionMasterId = pp.portionMasterId
        WHERE p.userId=?";
$types = 's';
$params = array($userId);
if ($productId !== '') {
    $sql .= " AND pp.productId=?";
    $types .= 's';
    $params[] = $productId;
}
$sql .= " ORDER BY pp.portionSortOrder ASC, pp.portionId ASC";

$rows = db_stmt_fetch_all($con, $sql, $types, ...$params);

foreach ($rows as $row) {
    $item = array();
    $item['portionId'] = (string) $row['portionId'];
    $item['productId'] = (string) $row['productId'];
    $item['portionMasterId'] = isset($row['portionMasterId']) && $row['portionMasterId'] !== null
        ? (string) $row['portionMasterId'] : '';
    $item['portionName'] = $row['resolvedPortionName'];
    $item['portionPrice'] = (string) $row['portionPrice'];
    $item['portionSortOrder'] = (string) $row['portionSortOrder'];
    $item['portionNetworkStatus'] = $row['portionNetworkStatus'];
    $item['portionDeletedStatus'] = ($row['portionStatus'] === 'active') ? '0' : '1';
    $response['portionResponse'][] = $item;
}

echo json_encode($response);
