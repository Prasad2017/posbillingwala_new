<?php
/**
 * GET mess shop setting (payer mode) for a POS licence/userId.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request',
    'payerMode' => 'user',
);

mess_common_ensure_schema($con);

$userId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
pos_require_auth($con, $userId, $response);

$mode = mess_get_payer_mode($con, (int) $userId);
$response['status'] = '1';
$response['message'] = 'ok';
$response['payerMode'] = $mode;
echo json_encode($response);
mysqli_close($con);
