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
        "SELECT t.*,
                m.member_mobile_number AS member_mobile,
                m.member_altenet_mobile_number AS member_alt_mobile
         FROM mess_meal_token t
         LEFT JOIN mess_member m ON m.id = t.member_id
         WHERE t.userId = ? AND t.token_date = ?
           AND t.print_status IN ('PRINT_PENDING','PRINT_FAILED','CREATED')
           AND (t.print_device_id IS NULL OR t.print_device_id = '' OR t.print_device_id = ?)
         ORDER BY t.id ASC",
        'iss',
        (int) $userId,
        $today,
        $deviceId
    );
} else {
    $rows = db_stmt_fetch_all(
        $con,
        "SELECT t.*,
                m.member_mobile_number AS member_mobile,
                m.member_altenet_mobile_number AS member_alt_mobile
         FROM mess_meal_token t
         LEFT JOIN mess_member m ON m.id = t.member_id
         WHERE t.userId = ? AND t.token_date = ?
           AND t.print_status IN ('PRINT_PENDING','PRINT_FAILED','CREATED')
         ORDER BY t.id ASC",
        'is',
        (int) $userId,
        $today
    );
}

$tokens = array();
foreach ($rows as $row) {
    $memberMobile = '';
    if (!empty($row['member_mobile'])) {
        $memberMobile = trim((string) $row['member_mobile']);
    } elseif (!empty($row['member_alt_mobile'])) {
        $memberMobile = trim((string) $row['member_alt_mobile']);
    } elseif (!empty($row['registration_no'])) {
        $memberMobile = trim((string) $row['registration_no']);
    }
    $tokens[] = array(
        'tokenId' => $row['public_id'],
        'tokenNumber' => $row['token_number'],
        'registrationNo' => $row['registration_no'],
        'mealSession' => $row['session_name'],
        'date' => $row['token_date'],
        'printStatus' => $row['print_status'],
        'createdAt' => $row['created_at'],
        'memberName' => $row['member_name'],
        'memberMobile' => $memberMobile,
    );
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['tokens'] = $tokens;
echo json_encode($response);
mysqli_close($con);
