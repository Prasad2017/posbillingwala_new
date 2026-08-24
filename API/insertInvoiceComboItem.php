<?php
/**
 * Frozen combo component lines on a bill. Idempotent on invoiceComboItemNetworkStatus.
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

$invoiceNumber = isset($_POST['invoiceNumber']) ? trim((string) $_POST['invoiceNumber']) : '';
$invoiceProductNetworkStatus = isset($_POST['invoiceProductNetworkStatus']) ? trim((string) $_POST['invoiceProductNetworkStatus']) : '';
$comboNetworkStatus = isset($_POST['comboNetworkStatus']) ? trim((string) $_POST['comboNetworkStatus']) : '';
$productId = isset($_POST['productId']) ? trim((string) $_POST['productId']) : '';
$productNetworkStatus = isset($_POST['productNetworkStatus']) ? trim((string) $_POST['productNetworkStatus']) : '';
$productName = isset($_POST['productName']) ? trim((string) $_POST['productName']) : '';
$portionId = isset($_POST['portionId']) ? trim((string) $_POST['portionId']) : '';
$portionNetworkStatus = isset($_POST['portionNetworkStatus']) ? trim((string) $_POST['portionNetworkStatus']) : '';
$portionName = isset($_POST['portionName']) ? trim((string) $_POST['portionName']) : '';
$quantity = isset($_POST['quantity']) ? trim((string) $_POST['quantity']) : '1';
$sortOrder = isset($_POST['sortOrder']) ? trim((string) $_POST['sortOrder']) : '0';
$invoiceComboItemNetworkStatus = isset($_POST['invoiceComboItemNetworkStatus']) ? trim((string) $_POST['invoiceComboItemNetworkStatus']) : '';

if ($invoiceNumber === '' || $invoiceComboItemNetworkStatus === '') {
    $response['message'] = 'invoiceNumber and invoiceComboItemNetworkStatus are required';
    echo json_encode($response);
    exit;
}

if ($productNetworkStatus !== '') {
    $prod = db_stmt_fetch_one(
        $con,
        'SELECT `productId`, `productName` FROM `products` WHERE `productNetworkStatus`=? LIMIT 1',
        's',
        $productNetworkStatus
    );
    if ($prod !== null) {
        $productId = (string) $prod['productId'];
        if ($productName === '') {
            $productName = (string) $prod['productName'];
        }
    }
}

if ($portionNetworkStatus !== '') {
    $portion = db_stmt_fetch_one(
        $con,
        'SELECT `portionId`, `portionName` FROM `product_portions` WHERE `portionNetworkStatus`=? LIMIT 1',
        's',
        $portionNetworkStatus
    );
    if ($portion !== null) {
        $portionId = (string) $portion['portionId'];
        if ($portionName === '') {
            $portionName = (string) $portion['portionName'];
        }
    }
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `invoice_combo_items` WHERE `invoiceComboItemNetworkStatus`=? LIMIT 1',
    's',
    $invoiceComboItemNetworkStatus
);

if ($existing !== null) {
    $id = (string) $existing['invoiceComboItemId'];
    $ok = db_stmt_execute(
        $con,
        'UPDATE `invoice_combo_items` SET
            `invoiceNumber`=?, `invoiceProductNetworkStatus`=?, `comboNetworkStatus`=?,
            `productId`=?, `productNameSnapshot`=?, `portionId`=?, `portionNameSnapshot`=?,
            `quantity`=?, `sortOrder`=?
         WHERE `invoiceComboItemId`=?',
        'ssssssssss',
        $invoiceNumber,
        $invoiceProductNetworkStatus,
        $comboNetworkStatus,
        $productId !== '' ? $productId : null,
        $productName,
        $portionId !== '' ? $portionId : null,
        $portionName,
        $quantity !== '' ? $quantity : '1',
        $sortOrder,
        $id
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `invoice_combo_items`
            (`invoiceNumber`, `invoiceProductNetworkStatus`, `comboNetworkStatus`, `productId`,
             `productNameSnapshot`, `portionId`, `portionNameSnapshot`, `quantity`, `sortOrder`,
             `invoiceComboItemNetworkStatus`)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
        'ssssssssss',
        $invoiceNumber,
        $invoiceProductNetworkStatus,
        $comboNetworkStatus,
        $productId !== '' ? $productId : null,
        $productName,
        $portionId !== '' ? $portionId : null,
        $portionName,
        $quantity !== '' ? $quantity : '1',
        $sortOrder,
        $invoiceComboItemNetworkStatus
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
}

echo json_encode($response);
