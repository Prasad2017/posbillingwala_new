<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'user.change_role');
$staffId = (int) pos_api_post('id');
$role = strtoupper(pos_api_post('role'));
$keepOverrides = pos_api_post('keepCompatibleOverrides') === '1';
if (!pos_role_valid($role)) {
    pos_api_json(array('status' => '0', 'message' => 'Select a valid role'));
}
$err = pos_protect_final_owner($con, $licenceId, $staffId, $role, null);
if ($err !== null) {
    pos_api_json(array('status' => '0', 'message' => $err));
}
db_stmt_execute(
    $con,
    'UPDATE `pos_staff` SET `role`=? WHERE `id`=? AND `licenseId`=?',
    'sii',
    $role,
    $staffId,
    (int) $licenceId
);
if (!$keepOverrides) {
    pos_save_staff_overrides($con, $staffId, array());
}
pos_bump_permission_version($con, $licenceId);
pos_audit($con, $licenceId, 'Role Changed', 'staff', $staffId, isset($actor['id']) ? $actor['id'] : 0, array('role' => $role));
$row = pos_staff_by_id($con, $licenceId, $staffId);
$overrides = pos_load_staff_overrides($con, $staffId);
$effective = pos_effective_permissions($con, $role, $overrides);
pos_api_json(array(
    'status' => '1',
    'message' => 'Role updated. Previous overrides were reset to the new role defaults.',
    'staff' => pos_staff_public($row, $effective, $overrides),
    'defaults' => pos_role_defaults($con, $role),
));
