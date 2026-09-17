<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../pos_schema.php';
require_once __DIR__ . '/../pos_staff.php';
require_once __DIR__ . '/../pos_audit.php';

header('Content-Type: application/json; charset=utf-8');
admin_require_auth($con);
pos_schema_ensure($con);

$licenseId = isset($_POST['licenseId']) ? (int) $_POST['licenseId'] : 0;
if ($licenseId <= 0) {
    echo json_encode(array('status' => 'false', 'message' => 'licenseId required'));
    exit;
}

$enabled = isset($_POST['userManagementEnabled']) ? (int) $_POST['userManagementEnabled'] : 0;
$maxUsers = isset($_POST['maxUsers']) ? (int) $_POST['maxUsers'] : 10;
$maxDevices = isset($_POST['maxDevices']) ? (int) $_POST['maxDevices'] : 5;
$maxPrinters = isset($_POST['maxPrinters']) ? (int) $_POST['maxPrinters'] : 0;
if ($maxUsers < 1) {
    $maxUsers = 1;
}
if ($maxDevices < 1) {
    $maxDevices = 1;
}

db_stmt_execute(
    $con,
    'UPDATE `licenses` SET `userManagementEnabled`=?, `maxUsers`=?, `maxDevices`=?, `maxPrinters`=? WHERE `id`=?',
    'iiiii',
    $enabled ? 1 : 0,
    $maxUsers,
    $maxDevices,
    $maxPrinters,
    $licenseId
);

$action = $enabled ? 'User Management Enabled' : 'User Management Disabled';
pos_audit($con, $licenseId, $action, 'license', $licenseId, 0, array(
    'maxUsers' => $maxUsers,
    'maxDevices' => $maxDevices,
    'maxPrinters' => $maxPrinters,
));
if ($enabled) {
    pos_seed_owner_from_licence($con, $licenseId);
}

echo json_encode(array('status' => 'true', 'message' => 'Licence user management updated'));
