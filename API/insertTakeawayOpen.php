<?php
/**
 * Upsert open takeaway parcel for web POS.
 */
include_once('config.php');
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/pos_staff.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('status' => '0', 'message' => 'Failed');

mysqli_query($con, "CREATE TABLE IF NOT EXISTS `takeaway_open_parcels` (
    `parcelId` INT NOT NULL AUTO_INCREMENT,
    `licenseId` VARCHAR(64) NOT NULL,
    `organization_id` VARCHAR(64) NULL,
    `branch_id` VARCHAR(64) NULL,
    `device_id` VARCHAR(128) NULL,
    `parcelNetworkStatus` VARCHAR(128) NOT NULL,
    `customerName` VARCHAR(255) NOT NULL DEFAULT '',
    `customerMobile` VARCHAR(64) NOT NULL DEFAULT '',
    `parcelStatus` VARCHAR(32) NOT NULL DEFAULT 'open',
    `createdAt` DATETIME NOT NULL,
    `updatedAt` DATETIME NOT NULL,
    PRIMARY KEY (`parcelId`),
    UNIQUE KEY `uq_parcel_net` (`licenseId`, `parcelNetworkStatus`),
    KEY `idx_parcel_status` (`licenseId`, `parcelStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

$postedUserId = isset($_POST['userId']) ? $_POST['userId'] : '';
$ctx = branch_pos_prepare_write($con, $postedUserId, $response);
if ($ctx === null) {
    echo json_encode($response);
    exit;
}

$userId = $ctx['licenseId'];
pos_require_permission($con, $userId, 'takeaway.create');
$orgId = $ctx['triplet']['organization_id'];
$branchId = $ctx['triplet']['branch_id'];
$deviceId = $ctx['triplet']['device_id'];

$post = function ($k, $d = '') {
    return isset($_POST[$k]) ? trim((string) $_POST[$k]) : $d;
};

$network = $post('parcelNetworkStatus');
if ($network === '') {
    $network = 'tw_' . time() . '_' . substr(md5(uniqid('', true)), 0, 8);
}
$customerName = $post('customerName', 'Walk-in');
$customerMobile = $post('customerMobile');
$status = $post('parcelStatus', 'open');
if (!in_array($status, array('open', 'billed', 'cancelled'), true)) {
    $status = 'open';
}

$now = date('Y-m-d H:i:s');
$existing = db_stmt_fetch_one(
    $con,
    'SELECT `parcelId` FROM `takeaway_open_parcels` WHERE `licenseId`=? AND `parcelNetworkStatus`=? LIMIT 1',
    'ss',
    $userId,
    $network
);

if ($existing !== null) {
    $ok = db_stmt_execute(
        $con,
        'UPDATE `takeaway_open_parcels` SET `customerName`=?, `customerMobile`=?, `parcelStatus`=?, `updatedAt`=?, `device_id`=? WHERE `parcelId`=?',
        'sssssi',
        $customerName,
        $customerMobile,
        $status,
        $now,
        (string) $deviceId,
        (int) $existing['parcelId']
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'update successful!' : 'update failed';
} else {
    $ok = db_stmt_execute(
        $con,
        'INSERT INTO `takeaway_open_parcels`
         (`licenseId`,`organization_id`,`branch_id`,`device_id`,`parcelNetworkStatus`,`customerName`,`customerMobile`,`parcelStatus`,`createdAt`,`updatedAt`)
         VALUES (?,?,?,?,?,?,?,?,?,?)',
        'ssssssssss',
        (string) $userId,
        $orgId === null ? '' : (string) $orgId,
        $branchId === null ? '' : (string) $branchId,
        (string) $deviceId,
        $network,
        $customerName,
        $customerMobile,
        $status,
        $now,
        $now
    );
    $response['status'] = $ok ? '1' : '0';
    $response['message'] = $ok ? 'insert successful!' : 'insert failed';
}

$response['parcelNetworkStatus'] = $network;
echo json_encode($response);
