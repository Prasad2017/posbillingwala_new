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
);
mess_common_ensure_schema($con);
date_default_timezone_set('Asia/Kolkata');

$userId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
pos_require_auth($con, $userId, $response);

$date = isset($_REQUEST['date']) ? trim((string) $_REQUEST['date']) : date('Y-m-d');
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
    $date = date('Y-m-d');
}

$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM mess_meal_token WHERE userId = ? AND token_date = ? ORDER BY id DESC',
    'is',
    (int) $userId,
    $date
);

$tokens = array();
$bySession = array();
foreach ($rows as $row) {
    $tokens[] = array(
        'tokenId' => $row['public_id'],
        'tokenNumber' => $row['token_number'],
        'registrationNo' => $row['registration_no'],
        'mealSession' => $row['session_name'],
        'date' => $row['token_date'],
        'printStatus' => $row['print_status'],
        'createdAt' => $row['created_at'],
        'printedAt' => $row['printed_at'],
        'memberName' => $row['member_name'],
    );

    $sn = $row['session_name'];
    if (!isset($bySession[$sn])) {
        $bySession[$sn] = array(
            'sessionName' => $sn,
            'generated' => 0,
            'printed' => 0,
            'pending' => 0,
            'failed' => 0,
        );
    }
    $bySession[$sn]['generated']++;
    $ps = strtoupper($row['print_status']);
    if ($ps === 'PRINTED') {
        $bySession[$sn]['printed']++;
    } elseif ($ps === 'PRINT_FAILED') {
        $bySession[$sn]['failed']++;
    } else {
        $bySession[$sn]['pending']++;
    }
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['date'] = $date;
$response['tokens'] = $tokens;
$response['counts'] = array_values($bySession);
echo json_encode($response);
mysqli_close($con);
