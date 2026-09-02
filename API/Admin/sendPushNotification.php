<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../fcm_helper.php';
require_once __DIR__ . '/../fcm_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);

$title = isset($_POST['title']) ? trim((string) $_POST['title']) : '';
$body = isset($_POST['message']) ? trim((string) $_POST['message']) : '';
if ($body === '' && isset($_POST['body'])) {
    $body = trim((string) $_POST['body']);
}
$target = isset($_POST['target']) ? trim((string) $_POST['target']) : 'active';
$licenseIds = isset($_POST['licenseIds']) ? trim((string) $_POST['licenseIds']) : '';
$url = isset($_POST['url']) ? trim((string) $_POST['url']) : '';
$imageUrl = isset($_POST['imageUrl']) ? trim((string) $_POST['imageUrl']) : '';

$extraData = array();
if ($url !== '') {
    $extraData['url'] = $url;
}
if ($imageUrl !== '') {
    $extraData['image_url'] = $imageUrl;
}

$result = fcm_broadcast_promotional($con, $title, $body, $target, $licenseIds, $extraData);
echo json_encode($result);
mysqli_close($con);
