<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request',
    'tokens' => array(),
    'counts' => array(),
    'messMealTokens' => array(),
    'messSessionCounts' => array(),
);
mess_common_ensure_schema($con);
date_default_timezone_set('Asia/Kolkata');

$userId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
pos_require_auth($con, $userId, $response);

$date = isset($_REQUEST['date']) ? trim((string) $_REQUEST['date']) : date('Y-m-d');
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
    $date = date('Y-m-d');
}

/* DATE() so DATETIME token_date rows still match yyyy-MM-dd. */
$rows = db_stmt_fetch_all(
    $con,
    'SELECT t.*,
            m.member_mobile_number AS member_mobile,
            m.member_altenet_mobile_number AS member_alt_mobile
     FROM mess_meal_token t
     LEFT JOIN mess_member m ON m.id = t.member_id
     WHERE t.userId = ? AND DATE(t.token_date) = ?
     ORDER BY t.id DESC',
    'is',
    (int) $userId,
    $date
);
if (!is_array($rows)) {
    $rows = db_stmt_fetch_all(
        $con,
        'SELECT * FROM mess_meal_token
         WHERE userId = ? AND DATE(token_date) = ?
         ORDER BY id DESC',
        'is',
        (int) $userId,
        $date
    );
}
if (!is_array($rows)) {
    $rows = array();
}

$tokens = array();
$bySession = array();
foreach ($rows as $row) {
    $memberMobile = '';
    if (!empty($row['member_mobile'])) {
        $memberMobile = trim((string) $row['member_mobile']);
    } elseif (!empty($row['member_alt_mobile'])) {
        $memberMobile = trim((string) $row['member_alt_mobile']);
    } elseif (!empty($row['registration_no'])) {
        $memberMobile = trim((string) $row['registration_no']);
    }

    $tokenDate = isset($row['token_date']) ? substr((string) $row['token_date'], 0, 10) : $date;
    $tokens[] = array(
        'tokenId' => isset($row['public_id']) ? (string) $row['public_id'] : '',
        'tokenNumber' => isset($row['token_number']) ? (string) $row['token_number'] : '',
        'registrationNo' => isset($row['registration_no']) ? (string) $row['registration_no'] : '',
        'mealSession' => isset($row['session_name']) ? (string) $row['session_name'] : '',
        'date' => $tokenDate,
        'printStatus' => isset($row['print_status']) ? (string) $row['print_status'] : '',
        'createdAt' => isset($row['created_at']) ? (string) $row['created_at'] : '',
        'printedAt' => isset($row['printed_at']) ? (string) $row['printed_at'] : '',
        'memberName' => isset($row['member_name']) ? (string) $row['member_name'] : '',
        'memberMobile' => $memberMobile,
        'memberId' => isset($row['member_id']) ? (string) $row['member_id'] : '',
    );

    $sn = isset($row['session_name']) ? (string) $row['session_name'] : '';
    if ($sn === '') {
        $sn = 'Session';
    }
    if (!isset($bySession[$sn])) {
        $bySession[$sn] = array(
            'sessionName' => $sn,
            'generated' => 0,
            'printed' => 0,
            'pending' => 0,
            'failed' => 0,
            'cancelled' => 0,
        );
    }
    $bySession[$sn]['generated']++;
    $ps = strtoupper(isset($row['print_status']) ? (string) $row['print_status'] : '');
    if ($ps === 'PRINTED') {
        $bySession[$sn]['printed']++;
    } elseif ($ps === 'PRINT_FAILED') {
        $bySession[$sn]['failed']++;
    } elseif ($ps === 'CANCELLED') {
        $bySession[$sn]['cancelled']++;
    } else {
        $bySession[$sn]['pending']++;
    }
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['date'] = $date;
$response['tokens'] = $tokens;
$response['counts'] = array_values($bySession);
/* Android Gson / Flutter aliases. */
$response['messMealTokens'] = $tokens;
$response['messSessionCounts'] = $response['counts'];
echo json_encode($response);
mysqli_close($con);
