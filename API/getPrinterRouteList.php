<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'printer.view');
$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM `printer_routes` WHERE `licenseId`=? ORDER BY `id` ASC',
    'i',
    (int) $licenceId
);
pos_api_json(array('status' => '1', 'routeResponse' => $rows));
