<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'printer.manage');
$um = pos_licence_um_row($con, $licenceId);
$max = (int) $um['maxPrinters'];
if ($max > 0 && pos_count_enabled_printers($con, $licenceId) >= $max) {
    pos_api_json(array('status' => '0', 'message' => 'Printer limit reached for this licence'));
}

$name = pos_api_post('printerName');
$connection = pos_normalize_connection_type(pos_api_post('connectionType', 'BLUETOOTH'));
if ($name === '') {
    pos_api_json(array('status' => '0', 'message' => 'Printer name is required'));
}
$paperSize = pos_normalize_paper_size(pos_api_post('paperSize', '2-Inch'));

$id = db_stmt_insert_id(
    $con,
    'INSERT INTO `store_printers`
     (`organization_id`, `licenseId`, `deviceId`, `printerName`, `printerType`, `connectionType`,
      `ipAddress`, `port`, `bluetoothAddress`, `usbIdentifier`, `usbName`, `paperSize`, `purpose`, `area`,
      `status`, `enabled`, `isDefault`, `isBackup`, `primaryPrinterId`)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \'OFFLINE\', 1, ?, ?, ?)',
    'iisssssissssssiii',
    (int) $um['userId'],
    (int) $licenceId,
    pos_api_post('deviceId'),
    $name,
    pos_api_post('printerType', 'THERMAL'),
    $connection,
    pos_api_post('ipAddress'),
    (int) pos_api_post('port', '9100'),
    pos_api_post('bluetoothAddress'),
    pos_api_post('usbIdentifier'),
    pos_api_post('usbName'),
    $paperSize,
    strtoupper(pos_api_post('purpose', 'KOT')),
    strtoupper(pos_api_post('area', 'KITCHEN')),
    pos_api_post('isDefault') === '1' ? 1 : 0,
    pos_api_post('isBackup') === '1' ? 1 : 0,
    (int) pos_api_post('primaryPrinterId', '0')
);
if ($id === false) {
    pos_api_json(array('status' => '0', 'message' => 'Unable to save printer'));
}
pos_audit($con, $licenceId, 'Printer Added', 'printer', $id, isset($actor['id']) ? $actor['id'] : 0);
$row = pos_printer_belongs($con, $licenceId, $id);
pos_api_json(array('status' => '1', 'message' => 'Printer saved', 'printer' => pos_printer_public($row)));
