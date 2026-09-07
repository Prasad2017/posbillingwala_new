<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/business_template_ops.php';

$response = array('status' => '1', 'message' => 'ok', 'businessTemplateResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    pos_require_auth($con, $userId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    // Empty list when no cloud row — POS keeps pending local template on Fetch.
    $row = business_template_fetch_row($con, $userId);
    if ($row !== null) {
        $item = array();
        $item['businessType'] = isset($row['businessType']) ? $row['businessType'] : 'restaurant';
        $item['businessTemplateId'] = isset($row['businessTemplateId'])
            ? $row['businessTemplateId'] : 'restaurant_default';
        $item['businessTemplateJson'] = isset($row['businessTemplateJson'])
            ? $row['businessTemplateJson'] : '';
        $item['templateNetworkStatus'] = isset($row['templateNetworkStatus'])
            ? $row['templateNetworkStatus'] : '';
        $response['businessTemplateResponse'][] = $item;
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
