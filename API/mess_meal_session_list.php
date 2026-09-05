<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => '0', 'message' => 'Invalid request', 'sessions' => array());
mess_common_ensure_schema($con);

$userId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
pos_require_auth($con, $userId, $response);
mess_ensure_default_sessions($con, (int) $userId);

$rows = db_stmt_fetch_all(
    $con,
    'SELECT * FROM mess_meal_session WHERE userId = ? ORDER BY sort_order ASC, id ASC',
    'i',
    (int) $userId
);

$sessions = array();
foreach ($rows as $row) {
    $sessions[] = array(
        'sessionId' => (string) $row['id'],
        'sessionName' => $row['session_name'],
        'startTime' => substr($row['start_time'], 0, 5),
        'endTime' => substr($row['end_time'], 0, 5),
        'tokenPrefix' => $row['token_prefix'],
        'isActive' => ((int) $row['is_active'] === 1) ? '1' : '0',
        'menuNotes' => $row['menu_notes'],
        'sortOrder' => (string) $row['sort_order'],
    );
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['sessions'] = $sessions;
echo json_encode($response);
mysqli_close($con);
