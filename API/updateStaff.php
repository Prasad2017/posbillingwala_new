<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'user.edit');
$staffId = (int) pos_api_post('id');
$row = pos_staff_by_id($con, $licenceId, $staffId);
if ($row === null) {
    pos_api_json(array('status' => '0', 'message' => 'User not found'));
}

$name = pos_api_post('name', $row['name']);
$mobile = pos_normalize_mobile(pos_api_post('mobileNumber', $row['mobileNumber']));
$address = pos_api_post('address', isset($row['address']) ? $row['address'] : '');
$image = pos_api_post('profileImage', isset($row['profileImage']) ? $row['profileImage'] : '');
$status = strtoupper(pos_api_post('status', $row['status']));
if (!in_array($status, array('ACTIVE', 'INACTIVE', 'BLOCKED'), true)) {
    $status = $row['status'];
}

if ($name === '' || strlen($name) > 120) {
    pos_api_json(array('status' => '0', 'message' => 'Name is required'));
}
if (!pos_mobile_valid($mobile)) {
    pos_api_json(array('status' => '0', 'message' => 'Enter a valid 10-digit mobile number'));
}

$dup = db_stmt_fetch_one(
    $con,
    "SELECT `id` FROM `pos_staff` WHERE `licenseId`=? AND `mobileNumber`=? AND `id`<>? LIMIT 1",
    'isi',
    (int) $licenceId,
    $mobile,
    $staffId
);
if ($dup !== null) {
    pos_api_json(array('status' => '0', 'message' => 'Mobile number already used in this store'));
}

$err = pos_protect_final_owner($con, $licenceId, $staffId, null, $status);
if ($err !== null) {
    pos_api_json(array('status' => '0', 'message' => $err));
}

$overridesRaw = isset($_POST['permissionOverrides']) ? $_POST['permissionOverrides'] : null;
if ($overridesRaw !== null) {
    $overrides = pos_normalize_overrides($overridesRaw);
    pos_save_staff_overrides($con, $staffId, $overrides);
    pos_bump_permission_version($con, $licenceId);
}

db_stmt_execute(
    $con,
    'UPDATE `pos_staff` SET `name`=?, `mobileNumber`=?, `address`=?, `profileImage`=?, `status`=? WHERE `id`=? AND `licenseId`=?',
    'sssssii',
    $name,
    $mobile,
    $address,
    $image,
    $status,
    $staffId,
    (int) $licenceId
);
pos_audit($con, $licenceId, 'User Updated', 'staff', $staffId, isset($actor['id']) ? $actor['id'] : 0);

$updated = pos_staff_by_id($con, $licenceId, $staffId);
$overrides = pos_load_staff_overrides($con, $staffId);
$effective = pos_effective_permissions($con, $updated['role'], $overrides);
pos_api_json(array(
    'status' => '1',
    'message' => 'User updated',
    'staff' => pos_staff_public($updated, $effective, $overrides),
));
