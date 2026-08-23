<?php
/**
 * Portion Master CRUD — name only (no price).
 * Price lives on product_portions (Product + Portion).
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
$portionName = isset($_POST['portionName']) ? trim((string) $_POST['portionName']) : '';
$portionMasterNetworkStatus = isset($_POST['portionMasterNetworkStatus'])
    ? trim((string) $_POST['portionMasterNetworkStatus']) : '';
$deletedRaw = isset($_POST['portionMasterDeletedStatus'])
    ? trim((string) $_POST['portionMasterDeletedStatus']) : '0';
$status = ($deletedRaw === '1' || strcasecmp($deletedRaw, 'deactive') === 0) ? 'deactive' : 'active';

if ($userId === '' || $portionMasterNetworkStatus === '') {
    $response['message'] = 'userId and portionMasterNetworkStatus are required';
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

    // Block delete when still linked to active product portions
    if ($status === 'deactive') {
        $inUse = db_stmt_scalar_int(
            $con,
            "SELECT COUNT(*) AS c FROM `product_portions`
             WHERE `portionMasterId`=? AND (`portionStatus`='active' OR `portionStatus` IS NULL OR `portionStatus`='')",
            's',
            $masterId
        );
        if ($inUse > 0) {
            $response['message'] = 'Cannot delete Portion Master — it is used by one or more products. Remove product portions first.';
            echo json_encode($response);
            exit;
        }
    }

    // Prevent duplicate name for same shop (exclude self)
    if ($status === 'active') {
        $dup = db_stmt_scalar_int(
            $con,
            'SELECT COUNT(*) AS c FROM `portion_master`
             WHERE `userId`=? AND LOWER(TRIM(`portionName`))=LOWER(?) AND `portionMasterId`<>?
               AND `portionMasterStatus`=\'active\'',
            'sss',
            $userId,
            $portionName,
            $masterId
        );
        if ($dup > 0) {
            $response['message'] = 'Portion name already exists';
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
    // Keep denormalized names on product_portions in sync with master rename
    if ($ok && $portionName !== '') {
        db_stmt_execute(
            $con,
            'UPDATE `product_portions` SET `portionName`=? WHERE `portionMasterId`=?',
            'ss',
            $portionName,
            $masterId
        );
    }
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
    $response['portionMasterId'] = $masterId;
} else {
    if ($portionName === '') {
        $response['message'] = 'Portion name is required';
        echo json_encode($response);
        exit;
    }
    $dup = db_stmt_scalar_int(
        $con,
        'SELECT COUNT(*) AS c FROM `portion_master`
         WHERE `userId`=? AND LOWER(TRIM(`portionName`))=LOWER(?) AND `portionMasterStatus`=\'active\'',
        'ss',
        $userId,
        $portionName
    );
    if ($dup > 0) {
        $response['message'] = 'Portion name already exists';
        echo json_encode($response);
        exit;
    }

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
