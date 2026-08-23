<?php
/**
 * Owner: Portion Master list.
 */
include_once('config.php');
require_once __DIR__ . '/../auth_tokens.php';
require_once dirname(__DIR__) . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('portionMasterResponse' => array());

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

$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM `portion_master` WHERE `userId`=? AND `portionMasterStatus`=\'active\' ORDER BY `portionName` ASC',
    's',
    $userId
);

foreach ($rows as $row) {
    $item = array();
    $item['portionMasterId'] = (string) $row['portionMasterId'];
    $item['portionName'] = $row['portionName'];
    $item['portionMasterNetworkStatus'] = $row['portionMasterNetworkStatus'];
    $item['portionMasterDeletedStatus'] = '0';
    $response['portionMasterResponse'][] = $item;
}

echo json_encode($response);
