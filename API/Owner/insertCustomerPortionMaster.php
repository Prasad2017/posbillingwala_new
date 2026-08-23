<?php
/**
 * Owner: Portion Master — name only.
 */
include_once('config.php');
require_once __DIR__ . '/../auth_tokens.php';
require_once dirname(__DIR__) . '/db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null) {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    exit;
}

$portionName = isset($_POST['portionName']) ? trim((string) $_POST['portionName']) : '';
$portionMasterNetworkStatus = isset($_POST['portionMasterNetworkStatus'])
    ? trim((string) $_POST['portionMasterNetworkStatus']) : '';
$deletedRaw = isset($_POST['portionMasterDeletedStatus'])
    ? trim((string) $_POST['portionMasterDeletedStatus']) : '0';
$status = ($deletedRaw === '1' || strcasecmp($deletedRaw, 'deactive') === 0) ? 'deactive' : 'active';

if ($portionMasterNetworkStatus === '') {
    $response['message'] = 'portionMasterNetworkStatus is required';
    echo json_encode($response);
    exit;
}

if ($status === 'active' && $portionName === '') {
    $response['message'] = 'Portion name is required';
    echo json_encode($response);
    exit;
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `portion_master` WHERE `portionMasterNetworkStatus`=? LIMIT 1',
    's',
    $portionMasterNetworkStatus
);

if ($existing !== null) {
    $masterId = (string) $existing['portionMasterId'];
    if ($status === 'deactive') {
        $inUse = db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `product_portions`
             WHERE `portionMasterId`=? AND (`portionStatus`='active' OR `portionStatus` IS NULL OR `portionStatus`='')",
            's',
            $masterId
        );
        if ($inUse > 0) {
            $response['message'] = 'Cannot delete Portion Master — used by products.';
            echo json_encode($response);
            exit;
        }
    }
    $ok = db_stmt_execute(
        $con,
        'UPDATE `portion_master` SET `portionName`=?, `portionMasterStatus`=?, `updated_at`=NOW() WHERE `portionMasterId`=?',
        'sss',
        $portionName !== '' ? $portionName : (string) $existing['portionName'],
        $status,
        $masterId
    );
    if ($ok && $portionName !== '') {
        db_stmt_execute($con, 'UPDATE `product_portions` SET `portionName`=? WHERE `portionMasterId`=?', 'ss', $portionName, $masterId);
    }
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
    $response['portionMasterId'] = $masterId;
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `portion_master` (`userId`, `portionName`, `portionMasterNetworkStatus`, `portionMasterStatus`, `created_at`, `updated_at`)
         VALUES (?, ?, ?, ?, NOW(), NOW())',
        'ssss',
        $userId,
        $portionName,
        $portionMasterNetworkStatus,
        $status
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
    if ($ok) {
        $response['portionMasterId'] = (string) mysqli_insert_id($con);
    }
}

echo json_encode($response);
