<?php
/**
 * Dealer: Product Portion — Product + Portion Master + price (upsert).
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once dirname(__DIR__) . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

dealer_require_auth($con);
mysqli_query($con, 'set names utf8');

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$productId = isset($_POST['productId']) ? trim((string) $_POST['productId']) : '';
$portionMasterId = isset($_POST['portionMasterId']) ? trim((string) $_POST['portionMasterId']) : '';
$portionName = isset($_POST['portionName']) ? trim((string) $_POST['portionName']) : '';
$portionPrice = isset($_POST['portionPrice']) ? trim((string) $_POST['portionPrice']) : '';
$portionSortOrder = isset($_POST['portionSortOrder']) ? trim((string) $_POST['portionSortOrder']) : '0';
$portionNetworkStatus = isset($_POST['portionNetworkStatus']) ? trim((string) $_POST['portionNetworkStatus']) : '';

if ($userId === '' || $productId === '' || $portionNetworkStatus === '') {
    $response['message'] = 'userId, productId and portionNetworkStatus are required';
    echo json_encode($response);
    exit;
}

if ($portionPrice === '' || !is_numeric($portionPrice)) {
    $response['message'] = 'Portion price is required';
    echo json_encode($response);
    exit;
}

$master = null;
if ($portionMasterId !== '') {
    $master = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `portion_master` WHERE `portionMasterId`=? AND `userId`=? LIMIT 1',
        'ss',
        $portionMasterId,
        $userId
    );
}
if ($master === null && $portionName !== '') {
    $master = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `portion_master` WHERE `userId`=? AND LOWER(TRIM(`portionName`))=LOWER(?) AND `portionMasterStatus`=\'active\' LIMIT 1',
        'ss',
        $userId,
        $portionName
    );
}
if ($master === null && $portionName !== '') {
    db_stmt_execute(
        $con,
        'INSERT INTO `portion_master` (`userId`, `portionName`, `portionMasterNetworkStatus`, `portionMasterStatus`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, \'active\', NOW(), NOW())',
        'sss',
        $userId,
        $portionName,
        'dlr-' . $userId . '-' . md5(strtolower($portionName) . uniqid('', true))
    );
    $newId = (string) mysqli_insert_id($con);
    $master = db_stmt_fetch_one($con, 'SELECT * FROM `portion_master` WHERE `portionMasterId`=? LIMIT 1', 's', $newId);
}

if ($master === null) {
    $response['message'] = 'Portion Master not found. Create portion name first.';
    echo json_encode($response);
    exit;
}

$portionMasterId = (string) $master['portionMasterId'];
$portionName = (string) $master['portionName'];

$byMaster = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `product_portions` WHERE `productId`=? AND `portionMasterId`=? LIMIT 1',
    'ss',
    $productId,
    $portionMasterId
);
$byNetwork = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `product_portions` WHERE `portionNetworkStatus`=? LIMIT 1',
    's',
    $portionNetworkStatus
);
$target = $byMaster !== null ? $byMaster : $byNetwork;

if ($target !== null) {
    $ok = db_stmt_execute(
        $con,
        'UPDATE `product_portions` SET `productId`=?, `portionMasterId`=?, `portionName`=?, `portionPrice`=?,
         `portionSortOrder`=?, `portionStatus`=\'active\', `updated_at`=NOW() WHERE `portionId`=?',
        'ssssss',
        $productId,
        $portionMasterId,
        $portionName,
        $portionPrice,
        $portionSortOrder,
        (string) $target['portionId']
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `product_portions`
         (`userId`, `productId`, `portionMasterId`, `portionName`, `portionPrice`, `portionSortOrder`, `portionNetworkStatus`, `portionStatus`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, ?, ?, ?, ?, \'active\', NOW(), NOW())',
        'sssssss',
        $userId,
        $productId,
        $portionMasterId,
        $portionName,
        $portionPrice,
        $portionSortOrder,
        $portionNetworkStatus
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
}

echo json_encode($response);
mysqli_close($con);
