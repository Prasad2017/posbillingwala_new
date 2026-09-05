<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request', 'tokens' => array());
mess_common_ensure_schema($con);
date_default_timezone_set('Asia/Kolkata');

$userId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
pos_require_auth($con, $userId, $response);

$deviceId = isset($_REQUEST['android_device_id']) ? trim((string) $_REQUEST['android_device_id']) : '';
$today = date('Y-m-d');

// Tokens pending print for this shop; prefer device-scoped, also include null device for recovery
if ($deviceId !== '') {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT * FROM mess_meal_token
         WHERE userId = ? AND token_date = ?
           AND print_status IN ('PRINT_PENDING','PRINT_FAILED','CREATED')
           AND (print_device_id IS NULL OR print_device_id = '' OR print_device_id = ?)
         ORDER BY id ASC",
        'iss',
        (int) $userId,
        $today,
        $deviceId
    );
} else {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT * FROM mess_meal_token
         WHERE userId = ? AND token_date = ?
           AND print_status IN ('PRINT_PENDING','PRINT_FAILED','CREATED')
         ORDER BY id ASC",
        'is',
        (int) $userId,
        $today
    );
}

$tokens = array();
foreach ($rows as $row) {
    $tokens[] = array(
        'tokenId' => $row['public_id'],
        'tokenNumber' => $row['token_number'],
        'registrationNo' => $row['registration_no'],
        'mealSession' => $row['session_name'],
        'date' => $row['token_date'],
        'printStatus' => $row['print_status'],
        'createdAt' => $row['created_at'],
        'memberName' => $row['member_name'],
    );
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['tokens'] = $tokens;
echo json_encode($response);
mysqli_close($con);
