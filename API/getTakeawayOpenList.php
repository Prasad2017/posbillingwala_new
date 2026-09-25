<?php
/**
 * List open takeaway parcels for web POS.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/pos_staff.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('status' => '0', 'message' => 'Failed', 'parcelResponse' => array());

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

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

$postedUserId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = pos_require_auth($con, $postedUserId, array('status' => '0', 'message' => 'Unauthorized'));
pos_require_permission($con, $userId, 'takeaway.view');

$statusFilter = isset($_GET['parcelStatus']) ? trim((string) $_GET['parcelStatus']) : 'open';
$esc = mysqli_real_escape_string($con, (string) $userId);
$where = "`licenseId`='$esc'";
if ($statusFilter !== 'all') {
    $where .= " AND `parcelStatus`='" . mysqli_real_escape_string($con, $statusFilter) . "'";
}

$rows = array();
$q = mysqli_query($con, "SELECT * FROM `takeaway_open_parcels` WHERE $where ORDER BY `createdAt` DESC LIMIT 200");
if ($q) {
    while ($row = mysqli_fetch_assoc($q)) {
        $rows[] = $row;
    }
    mysqli_free_result($q);
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['parcelResponse'] = $rows;
echo json_encode($response);
