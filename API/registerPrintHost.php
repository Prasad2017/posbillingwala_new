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
pos_api_json(array('status' => '1', 'message' => 'Print host online'));
