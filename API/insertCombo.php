<?php
/**
 * Combo master upsert — separate from products. Idempotent on comboNetworkStatus.
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
$comboName = isset($_POST['comboName']) ? trim((string) $_POST['comboName']) : '';
$comboCode = isset($_POST['comboCode']) ? trim((string) $_POST['comboCode']) : '';
$comboPrice = isset($_POST['comboPrice']) ? trim((string) $_POST['comboPrice']) : '';
$comboCGST = isset($_POST['comboCGST']) ? trim((string) $_POST['comboCGST']) : '';
$comboSGST = isset($_POST['comboSGST']) ? trim((string) $_POST['comboSGST']) : '';
$comboWithGSTPrice = isset($_POST['comboWithGSTPrice']) ? trim((string) $_POST['comboWithGSTPrice']) : '';
$comboActiveStatus = isset($_POST['comboActiveStatus']) ? trim((string) $_POST['comboActiveStatus']) : '1';
$comboNetworkStatus = isset($_POST['comboNetworkStatus']) ? trim((string) $_POST['comboNetworkStatus']) : '';
$comboSortOrder = isset($_POST['comboSortOrder']) ? trim((string) $_POST['comboSortOrder']) : '0';
$deletedRaw = isset($_POST['comboDeletedStatus']) ? trim((string) $_POST['comboDeletedStatus']) : '0';
$comboStatus = ($deletedRaw === '1' || strcasecmp($deletedRaw, 'deactive') === 0) ? 'deactive' : 'active';
$comboActiveStatus = ($comboActiveStatus === '0') ? '0' : '1';

if ($userId === '' || $comboNetworkStatus === '') {
    $response['message'] = 'userId and comboNetworkStatus are required';
    echo json_encode($response);
    exit;
}

if ($comboWithGSTPrice === '' && $comboPrice !== '' && is_numeric($comboPrice)) {
    $cgst = is_numeric($comboCGST) ? (float) $comboCGST : 0;
    $sgst = is_numeric($comboSGST) ? (float) $comboSGST : 0;
    $comboWithGSTPrice = (string) ((float) $comboPrice + ((float) $comboPrice * (($cgst + $sgst) / 100)));
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `combos` WHERE `comboNetworkStatus`=? LIMIT 1',
    's',
    $comboNetworkStatus
);

if ($existing !== null) {
    $comboId = (string) $existing['comboId'];
    $ok = db_stmt_execute(
        $con,
        'UPDATE `combos` SET
            `userId`=?, `comboName`=?, `comboCode`=?, `comboPrice`=?, `comboCGST`=?, `comboSGST`=?,
            `comboWithGSTPrice`=?, `comboActiveStatus`=?, `comboStatus`=?, `comboSortOrder`=?, `updated_at`=NOW()
         WHERE `comboId`=?',
        'sssssssssss',
        $userId,
        $comboName !== '' ? $comboName : (string) $existing['comboName'],
        $comboCode,
        $comboPrice !== '' ? $comboPrice : (string) $existing['comboPrice'],
        $comboCGST,
        $comboSGST,
        $comboWithGSTPrice,
        $comboActiveStatus,
        $comboStatus,
        $comboSortOrder,
        $comboId
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
    $response['comboId'] = $comboId;
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `combos`
            (`userId`, `comboName`, `comboCode`, `comboPrice`, `comboCGST`, `comboSGST`, `comboWithGSTPrice`,
             `comboActiveStatus`, `comboNetworkStatus`, `comboStatus`, `comboSortOrder`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())',
        'sssssssssss',
        $userId,
        $comboName,
        $comboCode,
        $comboPrice,
        $comboCGST,
        $comboSGST,
        $comboWithGSTPrice,
        $comboActiveStatus,
        $comboNetworkStatus,
        $comboStatus,
        $comboSortOrder
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
    if ($ok) {
        $response['comboId'] = (string) mysqli_insert_id($con);
    }
}

echo json_encode($response);
