<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$staffId = pos_posted_staff_id();
if ($staffId <= 0) {
    $staffId = (int) pos_api_post('id');
}
if (!pos_um_enabled($con, $licenceId)) {
    $all = array();
    foreach (pos_permission_keys() as $key) {
        $all[$key] = 1;
    }
    pos_api_json(array(
        'status' => '1',
        'userManagementEnabled' => '0',
        'role' => 'OWNER',
        'effectivePermissions' => $all,
        'permissionVersion' => (string) pos_licence_um_row($con, $licenceId)['permissionVersion'],
    ));
}
$staff = pos_staff_by_id($con, $licenceId, $staffId);
if ($staff === null || strtoupper($staff['status']) !== 'ACTIVE') {
    pos_api_json(array('status' => '0', 'message' => 'Staff inactive or not found'));
}
$overrides = pos_load_staff_overrides($con, $staffId);
$effective = pos_effective_permissions($con, $staff['role'], $overrides);
$um = pos_licence_um_row($con, $licenceId);
pos_api_json(array(
    'status' => '1',
    'userManagementEnabled' => '1',
    'permissionVersion' => (string) $um['permissionVersion'],
    'staff' => pos_staff_public($staff, $effective, $overrides),
    'effectivePermissions' => $effective,
));
