<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'printer.view');
$id = (int) pos_api_post('id');
$job = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `print_jobs` WHERE `id`=? AND `licenseId`=? LIMIT 1',
    'ii',
    $id,
    (int) $licenceId
);
if ($job === null) {
    pos_api_json(array('status' => '0', 'message' => 'Print job not found'));
}
pos_api_json(array('status' => '1', 'printJob' => $job));
