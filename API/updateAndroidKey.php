<?php
include_once('config.php');
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/licence_expiry.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Content-Type: application/json');

$response = array('status' => '0', 'message' => 'Device bind failed');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    mysqli_query($con, 'set names utf8');

    $androidId = isset($_POST['androidId']) ? trim($_POST['androidId']) : '';
    $android_device_name = isset($_POST['android_device_name']) ? trim($_POST['android_device_name']) : '';
    $app_licence_key = isset($_POST['app_licence_key']) ? trim($_POST['app_licence_key']) : '';

    if ($androidId !== '' && $app_licence_key !== '') {
        $response = licence_on_device_bind($con, $app_licence_key, $androidId, $android_device_name);
    } else {
        $response['message'] = 'Missing device id or licence key';
    }
}

echo json_encode($response);
?>
