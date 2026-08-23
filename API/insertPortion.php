<?php
/**
 * Product Portion — links Product + Portion Master with a selling price.
 * Portion Master has no price. Upserts on (productId, portionMasterId).
 */
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$productId = isset($_POST['productId']) ? trim((string) $_POST['productId']) : '';
$productNetworkStatus = isset($_POST['productNetworkStatus']) ? trim((string) $_POST['productNetworkStatus']) : '';
$portionMasterId = isset($_POST['portionMasterId']) ? trim((string) $_POST['portionMasterId']) : '';
$portionMasterNetworkStatus = isset($_POST['portionMasterNetworkStatus'])
    ? trim((string) $_POST['portionMasterNetworkStatus']) : '';
$portionName = isset($_POST['portionName']) ? trim((string) $_POST['portionName']) : '';
$portionPrice = isset($_POST['portionPrice']) ? trim((string) $_POST['portionPrice']) : '';
$portionSortOrder = isset($_POST['portionSortOrder']) ? trim((string) $_POST['portionSortOrder']) : '0';
$portionNetworkStatus = isset($_POST['portionNetworkStatus']) ? trim((string) $_POST['portionNetworkStatus']) : '';
$deletedRaw = isset($_POST['portionDeletedStatus']) ? trim((string) $_POST['portionDeletedStatus']) : '0';
$portionStatus = ($deletedRaw === '1' || strcasecmp($deletedRaw, 'deactive') === 0) ? 'deactive' : 'active';

if ($userId === '' || $portionNetworkStatus === '') {
    $response['message'] = 'userId and portionNetworkStatus are required';
    echo json_encode($response);
    exit;
}

if ($productNetworkStatus !== '') {
    $prod = db_stmt_fetch_one(
        $con,
        'SELECT `productId` FROM `products` WHERE `userId`=? AND `productNetworkStatus`=? LIMIT 1',
        'ss',
        $userId,
        $productNetworkStatus
    );
    if ($prod !== null) {
        $productId = (string) $prod['productId'];
    }
}

if ($productId === '') {
    $response['message'] = 'product not found!';
    echo json_encode($response);
    exit;
}

// Resolve Portion Master (preferred: id / network key; legacy: create/find by name)
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
if ($master === null && $portionMasterNetworkStatus !== '') {
    $master = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `portion_master` WHERE `portionMasterNetworkStatus`=? LIMIT 1',
        's',
        $portionMasterNetworkStatus
    );
}
if ($master === null && $portionName !== '') {
    $master = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `portion_master`
         WHERE `userId`=? AND LOWER(TRIM(`portionName`))=LOWER(?) AND `portionMasterStatus`=\'active\'
         LIMIT 1',
        'ss',
        $userId,
        $portionName
    );
}
// Legacy clients: auto-create master from free-text name (no price on master)
if ($master === null && $portionName !== '' && $portionStatus === 'active') {
    $net = $portionMasterNetworkStatus !== ''
        ? $portionMasterNetworkStatus
        : ('auto-' . $userId . '-' . md5(strtolower($portionName)));
    db_stmt_execute(
        $con,
        'INSERT INTO `portion_master` (`userId`, `portionName`, `portionMasterNetworkStatus`, `portionMasterStatus`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, \'active\', NOW(), NOW())',
        'sss',
        $userId,
        $portionName,
        $net
    );
    $newId = (string) mysqli_insert_id($con);
    if ($newId !== '' && $newId !== '0') {
        $master = db_stmt_fetch_one($con, 'SELECT * FROM `portion_master` WHERE `portionMasterId`=? LIMIT 1', 's', $newId);
    }
}

if ($master === null && $portionStatus === 'active') {
    $response['message'] = 'Portion Master not found. Create Portion Master first (name only).';
    echo json_encode($response);
    exit;
}

if ($master !== null) {
    $portionMasterId = (string) $master['portionMasterId'];
    $portionName = (string) $master['portionName'];
}

if ($portionStatus === 'active') {
    if ($portionPrice === '' || !is_numeric($portionPrice)) {
        $response['message'] = 'Portion price is required for Product + Portion';
        echo json_encode($response);
        exit;
    }
}

// Prefer upsert by productId + portionMasterId (single source of truth)
$byMaster = null;
if ($portionMasterId !== '') {
    $byMaster = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `product_portions` WHERE `productId`=? AND `portionMasterId`=? LIMIT 1',
        'ss',
        $productId,
        $portionMasterId
    );
}

$byNetwork = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `product_portions` WHERE `portionNetworkStatus`=? LIMIT 1',
    's',
    $portionNetworkStatus
);

$target = $byMaster !== null ? $byMaster : $byNetwork;

if ($target !== null) {
    $portionId = (string) $target['portionId'];
    $ok = db_stmt_execute(
        $con,
        'UPDATE `product_portions` SET
            `productId`=?, `portionMasterId`=?, `portionName`=?, `portionPrice`=?,
            `portionSortOrder`=?, `portionStatus`=?, `portionNetworkStatus`=?, `updated_at`=NOW()
         WHERE `portionId`=?',
        'ssssssss',
        $productId,
        $portionMasterId,
        $portionName,
        $portionPrice !== '' ? $portionPrice : (string) $target['portionPrice'],
        $portionSortOrder,
        $portionStatus,
        $portionNetworkStatus,
        $portionId
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
    $response['portionId'] = $portionId;
    $response['portionMasterId'] = $portionMasterId;
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `product_portions`
            (`userId`, `productId`, `portionMasterId`, `portionName`, `portionPrice`,
             `portionSortOrder`, `portionNetworkStatus`, `portionStatus`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())',
        'ssssssss',
        $userId,
        $productId,
        $portionMasterId,
        $portionName,
        $portionPrice,
        $portionSortOrder,
        $portionNetworkStatus,
        $portionStatus
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
    if ($ok) {
        $response['portionId'] = (string) mysqli_insert_id($con);
        $response['portionMasterId'] = $portionMasterId;
    }
}

echo json_encode($response);
