<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request');
mess_common_ensure_schema($con);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
pos_require_auth($con, $userId, $response);

$publicId = isset($_POST['tokenId']) ? trim((string) $_POST['tokenId']) : '';
if ($publicId === '') {
    $response['message'] = 'Missing tokenId';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$token = db_stmt_fetch_one(
    $con,
    'SELECT * FROM mess_meal_token WHERE public_id = ? AND userId = ? LIMIT 1',
    'si',
    $publicId,
    (int) $userId
);
if ($token === null) {
    $response['message'] = 'Token not found';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

db_stmt_execute(
    $con,
    "UPDATE mess_meal_token SET print_status = 'CANCELLED' WHERE public_id = ? AND userId = ? AND print_status != 'PRINTED'",
    'si',
    $publicId,
    (int) $userId
);
mess_audit($con, $userId, 'token_cancelled', null, $token['registration_no'], $publicId, null);

$response['status'] = '1';
$response['message'] = 'cancelled';
echo json_encode($response);
mysqli_close($con);
