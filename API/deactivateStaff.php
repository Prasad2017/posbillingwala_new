<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'user.deactivate');
$staffId = (int) pos_api_post('id');
$err = pos_protect_final_owner($con, $licenceId, $staffId, null, 'INACTIVE');
if ($err !== null) {
    pos_api_json(array('status' => '0', 'message' => $err));
}
$ok = db_stmt_execute(
    $con,
    "UPDATE `pos_staff` SET `status`='INACTIVE' WHERE `id`=? AND `licenseId`=?",
    'ii',
    $staffId,
    (int) $licenceId
);
if (!$ok) {
    pos_api_json(array('status' => '0', 'message' => 'Unable to deactivate'));
}
pos_audit($con, $licenceId, 'User Deactivated', 'staff', $staffId, isset($actor['id']) ? $actor['id'] : 0);
pos_api_json(array('status' => '1', 'message' => 'User deactivated'));
