<?php
/**
 * Upsert a dining session (floor state) for cloud / multi-device backup.
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

$localSessionId = $post('localSessionId');
$primaryTableNumber = trim($post('primaryTableNumber'));
$joinedTableNumbers = $post('joinedTableNumbers');
$sessionStatus = $post('sessionStatus', 'RUNNING');
$guestCount = $post('guestCount', '0');
$startedAt = $post('startedAt');
$closedAt = $post('closedAt');
$customerName = $post('customerName');
$customerMobile = $post('customerMobile');
$waiterName = $post('waiterName');
$unpaidInvoiceNumber = $post('unpaidInvoiceNumber');
$paidAmount = $post('paidAmount', '0');
$sessionVersion = $post('sessionVersion', '1');
$networkStatus = $post('sessionNetworkStatus');

if ($primaryTableNumber === '') {
    $response['message'] = 'primaryTableNumber required';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$esc = function ($v) use ($con) {
    return mysqli_real_escape_string($con, (string) $v);
};

$existing = null;
if ($networkStatus !== '') {
    $q = mysqli_query($con, "SELECT `sessionId` FROM `dining_session`
        WHERE `licenseId`='" . $esc($userId) . "' AND `sessionNetworkStatus`='" . $esc($networkStatus) . "' LIMIT 1");
    if ($q) {
        $existing = mysqli_fetch_assoc($q);
        mysqli_free_result($q);
    }
}
if ($existing === null && $localSessionId !== '') {
    $q = mysqli_query($con, "SELECT `sessionId` FROM `dining_session`
        WHERE `licenseId`='" . $esc($userId) . "' AND `localSessionId`='" . $esc($localSessionId) . "' LIMIT 1");
    if ($q) {
        $existing = mysqli_fetch_assoc($q);
        mysqli_free_result($q);
    }
}

$set = "`organization_id`=" . ($orgId === null || $orgId === '' ? 'NULL' : "'" . $esc($orgId) . "'") . ",
        `branch_id`=" . ($branchId === null || $branchId === '' ? 'NULL' : "'" . $esc($branchId) . "'") . ",
        `device_id`='" . $esc($deviceId) . "',
        `localSessionId`='" . $esc($localSessionId) . "',
        `primaryTableNumber`='" . $esc($primaryTableNumber) . "',
        `joinedTableNumbers`='" . $esc($joinedTableNumbers) . "',
        `sessionStatus`='" . $esc($sessionStatus) . "',
        `guestCount`='" . $esc($guestCount) . "',
        `startedAt`='" . $esc($startedAt) . "',
        `closedAt`='" . $esc($closedAt) . "',
        `customerName`='" . $esc($customerName) . "',
        `customerMobile`='" . $esc($customerMobile) . "',
        `waiterName`='" . $esc($waiterName) . "',
        `unpaidInvoiceNumber`='" . $esc($unpaidInvoiceNumber) . "',
        `paidAmount`='" . $esc($paidAmount) . "',
        `sessionVersion`='" . $esc($sessionVersion) . "',
        `sessionNetworkStatus`='" . $esc($networkStatus) . "'";

if ($existing !== null && isset($existing['sessionId'])) {
    $ok = mysqli_query($con, "UPDATE `dining_session` SET $set WHERE `sessionId`='" . $esc($existing['sessionId']) . "'");
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed!';
} else {
    $ok = mysqli_query($con, "INSERT INTO `dining_session`(
        `licenseId`, `organization_id`, `branch_id`, `device_id`, `localSessionId`,
        `primaryTableNumber`, `joinedTableNumbers`, `sessionStatus`, `guestCount`,
        `startedAt`, `closedAt`, `customerName`, `customerMobile`, `waiterName`,
        `unpaidInvoiceNumber`, `paidAmount`, `sessionVersion`, `sessionNetworkStatus`
     ) VALUES (
        '" . $esc($userId) . "',
        " . ($orgId === null || $orgId === '' ? 'NULL' : "'" . $esc($orgId) . "'") . ",
        " . ($branchId === null || $branchId === '' ? 'NULL' : "'" . $esc($branchId) . "'") . ",
        '" . $esc($deviceId) . "',
        '" . $esc($localSessionId) . "',
        '" . $esc($primaryTableNumber) . "',
        '" . $esc($joinedTableNumbers) . "',
        '" . $esc($sessionStatus) . "',
        '" . $esc($guestCount) . "',
        '" . $esc($startedAt) . "',
        '" . $esc($closedAt) . "',
        '" . $esc($customerName) . "',
        '" . $esc($customerMobile) . "',
        '" . $esc($waiterName) . "',
        '" . $esc($unpaidInvoiceNumber) . "',
        '" . $esc($paidAmount) . "',
        '" . $esc($sessionVersion) . "',
        '" . $esc($networkStatus) . "'
     )");
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed!';
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
