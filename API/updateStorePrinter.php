<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'printer.manage');
$id = (int) pos_api_post('id');
$row = pos_printer_belongs($con, $licenceId, $id);
if ($row === null) {
    pos_api_json(array('status' => '0', 'message' => 'Printer not found'));
}
$paperSize = pos_normalize_paper_size(pos_api_post('paperSize', isset($row['paperSize']) ? $row['paperSize'] : '2-Inch'));
$connection = pos_normalize_connection_type(
    pos_api_post('connectionType', $row['connectionType']),
    $row['connectionType']
);
db_stmt_execute(
    $con,
    'UPDATE `store_printers` SET
        `printerName`=?, `connectionType`=?, `ipAddress`=?, `port`=?, `bluetoothAddress`=?,
        `usbIdentifier`=?, `usbName`=?, `paperSize`=?, `purpose`=?, `area`=?,
        `deviceId`=?, `enabled`=?, `isDefault`=?, `isBackup`=?, `primaryPrinterId`=?, `status`=?
     WHERE `id`=? AND `licenseId`=?',
    'sssisssssssiiiisii',
    pos_api_post('printerName', $row['printerName']),
    $connection,
    pos_api_post('ipAddress', $row['ipAddress']),
    (int) pos_api_post('port', (string) $row['port']),
    pos_api_post('bluetoothAddress', $row['bluetoothAddress']),
    pos_api_post('usbIdentifier', $row['usbIdentifier']),
    pos_api_post('usbName', $row['usbName']),
    $paperSize,
    strtoupper(pos_api_post('purpose', $row['purpose'])),
    strtoupper(pos_api_post('area', $row['area'])),
    pos_api_post('deviceId', $row['deviceId']),
    pos_api_post('enabled', (string) $row['enabled']) === '0' ? 0 : 1,
    pos_api_post('isDefault', (string) $row['isDefault']) === '1' ? 1 : 0,
    pos_api_post('isBackup', (string) $row['isBackup']) === '1' ? 1 : 0,
    (int) pos_api_post('primaryPrinterId', (string) $row['primaryPrinterId']),
    strtoupper(pos_api_post('status', $row['status'])),
    $id,
    (int) $licenceId
);
pos_audit($con, $licenceId, 'Printer Updated', 'printer', $id, isset($actor['id']) ? $actor['id'] : 0);
$updated = pos_printer_belongs($con, $licenceId, $id);
pos_api_json(array('status' => '1', 'message' => 'Printer updated', 'printer' => pos_printer_public($updated)));
