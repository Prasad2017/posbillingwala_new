<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_permissions_seed_defaults($con);
$role = strtoupper(pos_api_post('role', 'WAITER'));
if (!pos_role_valid($role)) {
    $role = 'WAITER';
}
pos_api_json(array(
    'status' => '1',
    'role' => $role,
    'roleLabel' => pos_fixed_roles()[$role],
    'roles' => pos_fixed_roles(),
    'catalog' => pos_permission_catalog(),
    'defaults' => pos_role_defaults($con, $role),
));
