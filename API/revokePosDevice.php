<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'device.manage');
$id = (int) pos_api_post('id');
$ok = db_stmt_execute(
    $con,
    "UPDATE `pos_devices` SET `status`='REVOKED' WHERE `id`=? AND `licenseId`=?",
    'ii',
    $id,
    (int) $licenceId
);
if (!$ok) {
    pos_api_json(array('status' => '0', 'message' => 'Unable to revoke device'));
}
pos_audit($con, $licenceId, 'Device Revoked', 'device', $id, isset($actor['id']) ? $actor['id'] : 0);
pos_api_json(array('status' => '1', 'message' => 'Device revoked'));
