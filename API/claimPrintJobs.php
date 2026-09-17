<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$deviceId = pos_api_post('android_device_id');
if ($deviceId === '') {
    $deviceId = pos_api_post('deviceId');
}
if ($deviceId === '') {
    pos_api_json(array('status' => '0', 'message' => 'Device id required'));
}
pos_print_host_heartbeat($con, $licenceId, $deviceId, pos_api_post('platform', 'ANDROID'));
$jobs = pos_print_job_claim_for_host($con, $licenceId, $deviceId, (int) pos_api_post('limit', '10'));
$list = array();
foreach ($jobs as $job) {
    $printer = pos_printer_belongs($con, $licenceId, $job['printerId']);
    $job['printer'] = $printer ? pos_printer_public($printer) : null;
    $list[] = $job;
}
pos_api_json(array('status' => '1', 'printJobResponse' => $list));
