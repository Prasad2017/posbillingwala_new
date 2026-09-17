<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'user.view');
$staffId = (int) pos_api_post('id');
$row = pos_staff_by_id($con, $licenceId, $staffId);
if ($row === null) {
    pos_api_json(array('status' => '0', 'message' => 'User not found'));
}
$overrides = pos_load_staff_overrides($con, $row['id']);
$effective = pos_effective_permissions($con, $row['role'], $overrides);
$devices = db_stmt_fetch_all(
    $con,
    "SELECT `deviceId`, `deviceName`, `platform`, `status`, `lastSeenAt` FROM `pos_devices` WHERE `licenseId`=? ORDER BY `lastSeenAt` DESC",
    'i',
    (int) $licenceId
);
pos_api_json(array(
    'status' => '1',
    'staff' => pos_staff_public($row, $effective, $overrides),
    'devices' => $devices,
));
