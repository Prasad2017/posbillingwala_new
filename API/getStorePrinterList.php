<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'printer.view');
$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM `store_printers` WHERE `licenseId`=? ORDER BY `isDefault` DESC, `printerName` ASC',
    'i',
    (int) $licenceId
);
$list = array();
foreach ($rows as $row) {
    $list[] = pos_printer_public($row);
}
$um = pos_licence_um_row($con, $licenceId);
pos_api_json(array(
    'status' => '1',
    'printerResponse' => $list,
    'maxPrinters' => (string) $um['maxPrinters'],
));
