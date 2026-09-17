<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'printer.manage');
$id = (int) pos_api_post('id');
db_stmt_execute(
    $con,
    'UPDATE `store_printers` SET `enabled`=0, `status`=\'DISABLED\' WHERE `id`=? AND `licenseId`=?',
    'ii',
    $id,
    (int) $licenceId
);
pos_audit($con, $licenceId, 'Printer Disabled', 'printer', $id, isset($actor['id']) ? $actor['id'] : 0);
pos_api_json(array('status' => '1', 'message' => 'Printer disabled'));
