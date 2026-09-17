<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'device.view');
$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM `pos_devices` WHERE `licenseId`=? ORDER BY `lastSeenAt` DESC',
    'i',
    (int) $licenceId
);
$um = pos_licence_um_row($con, $licenceId);
pos_api_json(array(
    'status' => '1',
    'deviceResponse' => $rows,
    'maxDevices' => (string) $um['maxDevices'],
    'activeCount' => (string) pos_count_active_devices($con, $licenceId),
));
