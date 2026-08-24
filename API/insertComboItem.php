<?php
/**
 * Combo component upsert. Resolves combo/product/portion via *NetworkStatus like insertPortion.php.
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
$comboId = isset($_POST['comboId']) ? trim((string) $_POST['comboId']) : '';
$comboNetworkStatus = isset($_POST['comboNetworkStatus']) ? trim((string) $_POST['comboNetworkStatus']) : '';
$productId = isset($_POST['productId']) ? trim((string) $_POST['productId']) : '';
$productNetworkStatus = isset($_POST['productNetworkStatus']) ? trim((string) $_POST['productNetworkStatus']) : '';
$portionId = isset($_POST['portionId']) ? trim((string) $_POST['portionId']) : '';
$portionNetworkStatus = isset($_POST['portionNetworkStatus']) ? trim((string) $_POST['portionNetworkStatus']) : '';
$comboItemQuantity = isset($_POST['comboItemQuantity']) ? trim((string) $_POST['comboItemQuantity']) : '1';
$comboItemSortOrder = isset($_POST['comboItemSortOrder']) ? trim((string) $_POST['comboItemSortOrder']) : '0';
$comboItemNetworkStatus = isset($_POST['comboItemNetworkStatus']) ? trim((string) $_POST['comboItemNetworkStatus']) : '';
$deletedRaw = isset($_POST['comboItemDeletedStatus']) ? trim((string) $_POST['comboItemDeletedStatus']) : '0';
$comboItemStatus = ($deletedRaw === '1' || strcasecmp($deletedRaw, 'deactive') === 0) ? 'deactive' : 'active';

if ($userId === '' || $comboItemNetworkStatus === '') {
    $response['message'] = 'userId and comboItemNetworkStatus are required';
    echo json_encode($response);
    exit;
}

if ($comboNetworkStatus !== '') {
    $combo = db_stmt_fetch_one(
        $con,
        'SELECT `comboId` FROM `combos` WHERE `userId`=? AND `comboNetworkStatus`=? LIMIT 1',
        'ss',
        $userId,
        $comboNetworkStatus
    );
    if ($combo !== null) {
        $comboId = (string) $combo['comboId'];
    }
}

if ($comboId === '') {
    $response['message'] = 'combo not found!';
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

if ($productId === '' && $comboItemStatus === 'active') {
    $response['message'] = 'product not found!';
    echo json_encode($response);
    exit;
}

if ($portionNetworkStatus !== '') {
    $portion = db_stmt_fetch_one(
        $con,
        'SELECT `portionId` FROM `product_portions` WHERE `portionNetworkStatus`=? LIMIT 1',
        's',
        $portionNetworkStatus
    );
    if ($portion !== null) {
        $portionId = (string) $portion['portionId'];
    }
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `combo_items` WHERE `comboItemNetworkStatus`=? LIMIT 1',
    's',
    $comboItemNetworkStatus
);

if ($existing !== null) {
    $comboItemId = (string) $existing['comboItemId'];
    $ok = db_stmt_execute(
        $con,
        'UPDATE `combo_items` SET
            `userId`=?, `comboId`=?, `productId`=?, `portionId`=?, `comboItemQuantity`=?,
            `comboItemSortOrder`=?, `comboItemStatus`=?, `updated_at`=NOW()
         WHERE `comboItemId`=?',
        'ssssssss',
        $userId,
        $comboId,
        $productId,
        $portionId !== '' ? $portionId : null,
        $comboItemQuantity !== '' ? $comboItemQuantity : '1',
        $comboItemSortOrder,
        $comboItemStatus,
        $comboItemId
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
    $response['comboItemId'] = $comboItemId;
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `combo_items`
            (`userId`, `comboId`, `productId`, `portionId`, `comboItemQuantity`,
             `comboItemSortOrder`, `comboItemNetworkStatus`, `comboItemStatus`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())',
        'ssssssss',
        $userId,
        $comboId,
        $productId,
        $portionId !== '' ? $portionId : null,
        $comboItemQuantity !== '' ? $comboItemQuantity : '1',
        $comboItemSortOrder,
        $comboItemNetworkStatus,
        $comboItemStatus
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
    if ($ok) {
        $response['comboItemId'] = (string) mysqli_insert_id($con);
    }
}

echo json_encode($response);
