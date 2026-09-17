<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

$licenceId = pos_api_require_licence($con);
$deviceId = pos_api_post('android_device_id');
if ($deviceId === '') {
    $deviceId = pos_api_post('deviceId');
}
$result = pos_device_register(
    $con,
    $licenceId,
    $deviceId,
    pos_api_post('deviceName'),
    pos_api_post('platform', 'ANDROID'),
    pos_api_post('appVersion'),
    pos_api_post('osVersion')
);
if (empty($result['ok'])) {
    pos_api_json(array('status' => '0', 'message' => isset($result['message']) ? $result['message'] : 'Failed'));
}
pos_api_json(array('status' => '1', 'message' => 'Device registered', 'id' => (string) $result['id']));
