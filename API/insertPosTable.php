<?php
/**
 * Upsert a physical POS table (dine-in master).
 */
include_once('config.php');
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/dine_in_helpers.php';

$response = array('status' => '0', 'message' => 'Failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

mysqli_query($con, 'set names utf8');
dine_in_ensure_schema($con);

$postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';
$ctx = branch_pos_prepare_write($con, $postedUserId, $response);
if ($ctx === null) {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$userId = $ctx['licenseId'];
$orgId = $ctx['triplet']['organization_id'];
$branchId = $ctx['triplet']['branch_id'];
$deviceId = $ctx['triplet']['device_id'];

$post = function ($key, $default = '') {
    return isset($_POST[$key]) ? (string) $_POST[$key] : $default;
};

$tableNumber = trim($post('tableNumber'));
$tableName = $post('tableName');
$tableTypeId = $post('tableTypeId');
$capacity = $post('capacity', '4');
$areaId = $post('areaId');
$tableActive = $post('tableActive', '1');
$positionX = $post('positionX');
$positionY = $post('positionY');
$sortOrder = $post('sortOrder', '0');
$statusOverride = $post('statusOverride');
$networkStatus = $post('posTableNetworkStatus');

if ($tableNumber === '') {
    $response['message'] = 'tableNumber required';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$esc = function ($v) use ($con) {
    return mysqli_real_escape_string($con, (string) $v);
};

$branchClause = ($branchId === null || $branchId === '')
    ? '(`branch_id` IS NULL OR `branch_id`=\'\')'
    : "`branch_id`='" . $esc($branchId) . "'";

$existing = null;
if ($networkStatus !== '') {
    $q = mysqli_query($con, "SELECT `tableId` FROM `pos_table`
        WHERE `licenseId`='" . $esc($userId) . "' AND `posTableNetworkStatus`='" . $esc($networkStatus) . "' LIMIT 1");
    if ($q) {
        $existing = mysqli_fetch_assoc($q);
        mysqli_free_result($q);
    }
}
if ($existing === null) {
    $q = mysqli_query($con, "SELECT `tableId` FROM `pos_table`
        WHERE `licenseId`='" . $esc($userId) . "' AND $branchClause
          AND `tableNumber`='" . $esc($tableNumber) . "' LIMIT 1");
    if ($q) {
        $existing = mysqli_fetch_assoc($q);
        mysqli_free_result($q);
    }
}

$set = "`organization_id`='" . $esc($orgId) . "',
        `branch_id`=" . ($branchId === null || $branchId === '' ? 'NULL' : "'" . $esc($branchId) . "'") . ",
        `device_id`='" . $esc($deviceId) . "',
        `tableNumber`='" . $esc($tableNumber) . "',
        `tableName`='" . $esc($tableName) . "',
        `tableTypeId`='" . $esc($tableTypeId) . "',
        `capacity`='" . $esc($capacity) . "',
        `areaId`='" . $esc($areaId) . "',
        `tableActive`='" . $esc($tableActive) . "',
        `positionX`='" . $esc($positionX) . "',
        `positionY`='" . $esc($positionY) . "',
        `sortOrder`='" . $esc($sortOrder) . "',
        `statusOverride`='" . $esc($statusOverride) . "',
        `posTableNetworkStatus`='" . $esc($networkStatus) . "'";

if ($existing !== null && isset($existing['tableId'])) {
    $ok = mysqli_query($con, "UPDATE `pos_table` SET $set WHERE `tableId`='" . $esc($existing['tableId']) . "'");
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
} else {
    $ok = mysqli_query($con, "INSERT INTO `pos_table`(
        `licenseId`, `organization_id`, `branch_id`, `device_id`, `tableNumber`, `tableName`,
        `tableTypeId`, `capacity`, `areaId`, `tableActive`, `positionX`, `positionY`,
        `sortOrder`, `statusOverride`, `posTableNetworkStatus`
     ) VALUES (
        '" . $esc($userId) . "',
        " . ($orgId === null || $orgId === '' ? 'NULL' : "'" . $esc($orgId) . "'") . ",
        " . ($branchId === null || $branchId === '' ? 'NULL' : "'" . $esc($branchId) . "'") . ",
        '" . $esc($deviceId) . "',
        '" . $esc($tableNumber) . "',
        '" . $esc($tableName) . "',
        '" . $esc($tableTypeId) . "',
        '" . $esc($capacity) . "',
        '" . $esc($areaId) . "',
        '" . $esc($tableActive) . "',
        '" . $esc($positionX) . "',
        '" . $esc($positionY) . "',
        '" . $esc($sortOrder) . "',
        '" . $esc($statusOverride) . "',
        '" . $esc($networkStatus) . "'
     )");
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
