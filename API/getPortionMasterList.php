<?php
/**
 * Portion Master list — name only (no price).
 */
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('portionMasterResponse' => array());

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
    'SELECT * FROM `portion_master` WHERE `userId`=? ORDER BY `portionName` ASC, `portionMasterId` ASC',
    's',
    $userId
);

foreach ($rows as $row) {
    $item = array();
    $item['portionMasterId'] = (string) $row['portionMasterId'];
    $item['portionName'] = $row['portionName'];
    $item['portionMasterNetworkStatus'] = $row['portionMasterNetworkStatus'];
    $item['portionMasterDeletedStatus'] = ($row['portionMasterStatus'] === 'active') ? '0' : '1';
    $response['portionMasterResponse'][] = $item;
}

echo json_encode($response);
