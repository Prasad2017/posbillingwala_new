<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'user.create');
pos_permissions_seed_defaults($con);

if (!pos_um_enabled($con, $licenceId)) {
    pos_api_json(array('status' => '0', 'message' => 'Enable User Management on this licence first'));
}

$um = pos_licence_um_row($con, $licenceId);
$name = pos_api_post('name');
$mobile = pos_normalize_mobile(pos_api_post('mobileNumber'));
$address = pos_api_post('address');
$image = pos_api_post('profileImage');
$role = strtoupper(pos_api_post('role'));
$pin = pos_api_post('appLoginPin');
$confirm = pos_api_post('confirmPin', $pin);
$overrides = pos_normalize_overrides(isset($_POST['permissionOverrides']) ? $_POST['permissionOverrides'] : array());

if ($name === '' || strlen($name) > 120) {
    pos_api_json(array('status' => '0', 'message' => 'Name is required'));
}
if (!pos_mobile_valid($mobile)) {
    pos_api_json(array('status' => '0', 'message' => 'Enter a valid 10-digit mobile number'));
}
if (!pos_role_valid($role)) {
    pos_api_json(array('status' => '0', 'message' => 'Select a valid role'));
}
if (!pos_pin_valid_format($pin) || $pin !== $confirm) {
    pos_api_json(array('status' => '0', 'message' => 'PIN must be 4 or 6 digits and match confirm PIN'));
}

$dup = db_stmt_fetch_one(
    $con,
    "SELECT `id` FROM `pos_staff` WHERE `licenseId`=? AND `mobileNumber`=? AND `status`<>'DELETED' LIMIT 1",
    'is',
    (int) $licenceId,
    $mobile
);
if ($dup !== null) {
    pos_api_json(array('status' => '0', 'message' => 'Mobile number already used in this store'));
}

if (pos_count_active_staff($con, $licenceId) >= max(1, (int) $um['maxUsers'])) {
    pos_api_json(array('status' => '0', 'message' => 'User limit reached for this licence'));
}

$id = db_stmt_insert_id(
    $con,
    'INSERT INTO `pos_staff` (`organization_id`, `licenseId`, `name`, `mobileNumber`, `address`, `profileImage`, `role`, `pinHash`, `status`)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, \'ACTIVE\')',
    'iissssss',
    (int) $um['userId'],
    (int) $licenceId,
    $name,
    $mobile,
    $address,
    $image,
    $role,
    pos_pin_hash($pin)
);
if ($id === false) {
    pos_api_json(array('status' => '0', 'message' => 'Unable to save user'));
}
pos_save_staff_overrides($con, $id, $overrides);
pos_bump_permission_version($con, $licenceId);
pos_audit($con, $licenceId, 'User Created', 'staff', $id, isset($actor['id']) ? $actor['id'] : 0, array('role' => $role, 'mobile' => $mobile));

$row = pos_staff_by_id($con, $licenceId, $id);
$effective = pos_effective_permissions($con, $role, $overrides);
pos_api_json(array(
    'status' => '1',
    'message' => 'User saved',
    'staff' => pos_staff_public($row, $effective, $overrides),
));
