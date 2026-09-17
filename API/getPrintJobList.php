<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'printer.view');
$status = strtoupper(pos_api_post('status'));
$sql = 'SELECT * FROM `print_jobs` WHERE `licenseId`=?';
$types = 'i';
$params = array((int) $licenceId);
if ($status !== '') {
    $sql .= ' AND `status`=?';
    $types .= 's';
    $params[] = $status;
}
$sql .= ' ORDER BY `id` DESC LIMIT 100';
$rows = db_stmt_fetch_all($con, $sql, $types, ...$params);
pos_api_json(array('status' => '1', 'printJobResponse' => $rows));
