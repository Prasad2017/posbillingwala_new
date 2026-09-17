<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
if (pos_um_enabled($con, $licenceId)) {
    pos_seed_owner_from_licence($con, $licenceId);
}
pos_require_permission($con, $licenceId, 'user.view');
pos_permissions_seed_defaults($con);

$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM `pos_staff` WHERE `licenseId`=? ORDER BY `role` ASC, `name` ASC',
    'i',
    (int) $licenceId
);
$list = array();
foreach ($rows as $row) {
    $overrides = pos_load_staff_overrides($con, $row['id']);
    $effective = pos_effective_permissions($con, $row['role'], $overrides);
    $list[] = pos_staff_public($row, $effective, $overrides);
}
$um = pos_licence_um_row($con, $licenceId);
pos_api_json(array(
    'status' => '1',
    'staffResponse' => $list,
    'maxUsers' => (string) $um['maxUsers'],
    'activeCount' => (string) pos_count_active_staff($con, $licenceId),
    'permissionVersion' => (string) $um['permissionVersion'],
));
