<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'user.reset_pin');
$staffId = (int) pos_api_post('id');
$pin = pos_api_post('appLoginPin');
$confirm = pos_api_post('confirmPin', $pin);
if (!pos_pin_valid_format($pin) || $pin !== $confirm) {
    pos_api_json(array('status' => '0', 'message' => 'PIN must be 4 or 6 digits and match confirm PIN'));
}
$row = pos_staff_by_id($con, $licenceId, $staffId);
if ($row === null) {
    pos_api_json(array('status' => '0', 'message' => 'User not found'));
}
db_stmt_execute(
    $con,
    'UPDATE `pos_staff` SET `pinHash`=? WHERE `id`=? AND `licenseId`=?',
    'sii',
    pos_pin_hash($pin),
    $staffId,
    (int) $licenceId
);
pos_audit($con, $licenceId, 'PIN Reset', 'staff', $staffId, isset($actor['id']) ? $actor['id'] : 0);
pos_api_json(array('status' => '1', 'message' => 'PIN reset'));
