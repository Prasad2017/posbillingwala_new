<?php
include_once "config.php";
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/db_prepared.php';
require_once __DIR__ . '/pos_auth_guard.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use Post Method';
    echo json_encode($response);
    exit;
}

$mpin = isset($_POST['mpin']) ? trim((string) $_POST['mpin']) : '';
$app_licence_key = isset($_POST['app_licence_key']) ? trim((string) $_POST['app_licence_key']) : '';

if ($mpin === '' || $app_licence_key === '') {
    $response['message'] = 'mpin and licence key required';
    echo json_encode($response);
    exit;
}

// Must be authenticated as this licence (or owner org matching posted empty → token only)
$licenceId = pos_require_auth($con, '', array('status' => '0', 'message' => 'Unauthorized'));

$row = db_stmt_fetch_one(
    $con,
    'SELECT `id`, `licenseKey` FROM `licenses` WHERE `id`=? LIMIT 1',
    'i',
    (int) $licenceId
);

if ($row === null || (string) $row['licenseKey'] !== $app_licence_key) {
    $response['message'] = 'Licence mismatch';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$ok = db_stmt_execute(
    $con,
    'UPDATE `licenses` SET `mpin`=? WHERE `id`=?',
    'si',
    $mpin,
    (int) $licenceId
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = ' successful!';
} else {
    $response['message'] = ' failed!';
}

echo json_encode($response);
mysqli_close($con);
?>
