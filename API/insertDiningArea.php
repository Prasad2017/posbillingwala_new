<?php
/**
 * Upsert a dining area (Hall / AC / Garden / custom).
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

$areaName = trim($post('areaName'));
$areaSortOrder = $post('areaSortOrder', '0');
$areaActive = $post('areaActive', '1');
$networkStatus = $post('areaNetworkStatus');

if ($areaName === '') {
    $response['message'] = 'areaName required';
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
    $q = mysqli_query($con, "SELECT `areaId` FROM `dining_area`
        WHERE `licenseId`='" . $esc($userId) . "' AND `areaNetworkStatus`='" . $esc($networkStatus) . "' LIMIT 1");
    if ($q) {
        $existing = mysqli_fetch_assoc($q);
        mysqli_free_result($q);
    }
}
if ($existing === null) {
    $q = mysqli_query($con, "SELECT `areaId` FROM `dining_area`
        WHERE `licenseId`='" . $esc($userId) . "' AND $branchClause
          AND LOWER(TRIM(`areaName`))=LOWER(TRIM('" . $esc($areaName) . "')) LIMIT 1");
    if ($q) {
        $existing = mysqli_fetch_assoc($q);
        mysqli_free_result($q);
    }
}

$set = "`organization_id`=" . ($orgId === null || $orgId === '' ? 'NULL' : "'" . $esc($orgId) . "'") . ",
        `branch_id`=" . ($branchId === null || $branchId === '' ? 'NULL' : "'" . $esc($branchId) . "'") . ",
        `device_id`='" . $esc($deviceId) . "',
        `areaName`='" . $esc($areaName) . "',
        `areaSortOrder`='" . $esc($areaSortOrder) . "',
        `areaActive`='" . $esc($areaActive) . "',
        `areaNetworkStatus`='" . $esc($networkStatus) . "'";

if ($existing !== null && isset($existing['areaId'])) {
    $ok = mysqli_query($con, "UPDATE `dining_area` SET $set WHERE `areaId`='" . $esc($existing['areaId']) . "'");
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
} else {
    $ok = mysqli_query($con, "INSERT INTO `dining_area`(
        `licenseId`, `organization_id`, `branch_id`, `device_id`,
        `areaName`, `areaSortOrder`, `areaActive`, `areaNetworkStatus`
     ) VALUES (
        '" . $esc($userId) . "',
        " . ($orgId === null || $orgId === '' ? 'NULL' : "'" . $esc($orgId) . "'") . ",
        " . ($branchId === null || $branchId === '' ? 'NULL' : "'" . $esc($branchId) . "'") . ",
        '" . $esc($deviceId) . "',
        '" . $esc($areaName) . "',
        '" . $esc($areaSortOrder) . "',
        '" . $esc($areaActive) . "',
        '" . $esc($networkStatus) . "'
     )");
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
