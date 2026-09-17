<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$actor = pos_require_permission($con, $licenceId, 'printer.manage');
$raw = isset($_POST['routes']) ? $_POST['routes'] : '[]';
$routes = is_array($raw) ? $raw : json_decode($raw, true);
if (!is_array($routes)) {
    pos_api_json(array('status' => '0', 'message' => 'Invalid routes'));
}
db_stmt_execute($con, 'DELETE FROM `printer_routes` WHERE `licenseId`=?', 'i', (int) $licenceId);
foreach ($routes as $route) {
    if (!is_array($route)) {
        continue;
    }
    $printerId = isset($route['printerId']) ? (int) $route['printerId'] : 0;
    if ($printerId <= 0 || pos_printer_belongs($con, $licenceId, $printerId) === null) {
        continue;
    }
    db_stmt_execute(
        $con,
        'INSERT INTO `printer_routes` (`licenseId`, `printerId`, `documentType`, `foodTypeCode`, `categoryId`, `subcategoryId`, `productId`)
         VALUES (?, ?, ?, ?, ?, ?, ?)',
        'iissiii',
        (int) $licenceId,
        $printerId,
        strtoupper(isset($route['documentType']) ? $route['documentType'] : 'KOT'),
        isset($route['foodTypeCode']) ? $route['foodTypeCode'] : '',
        isset($route['categoryId']) ? (int) $route['categoryId'] : 0,
        isset($route['subcategoryId']) ? (int) $route['subcategoryId'] : 0,
        isset($route['productId']) ? (int) $route['productId'] : 0
    );
}
pos_audit($con, $licenceId, 'Printer Updated', 'printer_route', $licenceId, isset($actor['id']) ? $actor['id'] : 0);
pos_api_json(array('status' => '1', 'message' => 'Routing saved'));
