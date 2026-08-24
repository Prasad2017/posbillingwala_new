<?php
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('comboResponse' => array());

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
    'SELECT * FROM `combos` WHERE `userId`=? ORDER BY `comboSortOrder` ASC, `comboId` ASC',
    's',
    $userId
);

foreach ($rows as $row) {
    $item = array();
    $item['comboId'] = (string) $row['comboId'];
    $item['comboName'] = (string) $row['comboName'];
    $item['comboCode'] = isset($row['comboCode']) ? (string) $row['comboCode'] : '';
    $item['comboPrice'] = (string) $row['comboPrice'];
    $item['comboCGST'] = isset($row['comboCGST']) ? (string) $row['comboCGST'] : '';
    $item['comboSGST'] = isset($row['comboSGST']) ? (string) $row['comboSGST'] : '';
    $item['comboWithGSTPrice'] = isset($row['comboWithGSTPrice']) ? (string) $row['comboWithGSTPrice'] : '';
    $item['comboActiveStatus'] = isset($row['comboActiveStatus']) ? (string) $row['comboActiveStatus'] : '1';
    $item['comboDeletedStatus'] = (isset($row['comboStatus']) && $row['comboStatus'] === 'active') ? '0' : '1';
    $item['comboNetworkStatus'] = (string) $row['comboNetworkStatus'];
    $item['comboSortOrder'] = isset($row['comboSortOrder']) ? (string) $row['comboSortOrder'] : '0';
    $response['comboResponse'][] = $item;
}

echo json_encode($response);
