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

$sessionId = isset($_POST['sessionId']) ? trim((string) $_POST['sessionId']) : '';
$sessionName = isset($_POST['sessionName']) ? trim((string) $_POST['sessionName']) : '';
$startTime = isset($_POST['startTime']) ? trim((string) $_POST['startTime']) : '';
$endTime = isset($_POST['endTime']) ? trim((string) $_POST['endTime']) : '';
$tokenPrefix = isset($_POST['tokenPrefix']) ? strtoupper(trim((string) $_POST['tokenPrefix'])) : 'L';
$isActive = isset($_POST['isActive']) ? (trim((string) $_POST['isActive']) === '1' ? 1 : 0) : 1;
$menuNotes = isset($_POST['menuNotes']) ? trim((string) $_POST['menuNotes']) : '';
$sortOrder = isset($_POST['sortOrder']) ? (int) $_POST['sortOrder'] : 0;

if ($sessionName === '' || $startTime === '' || $endTime === '') {
    $response['message'] = 'Missing session fields';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if (strlen($startTime) === 5) {
    $startTime .= ':00';
}
if (strlen($endTime) === 5) {
    $endTime .= ':00';
}
if ($tokenPrefix === '') {
    $tokenPrefix = 'T';
}

if ($sessionId !== '' && (int) $sessionId > 0) {
    $owned = db_stmt_fetch_one(
        $con,
        'SELECT id FROM mess_meal_session WHERE id = ? AND userId = ? LIMIT 1',
        'ii',
        (int) $sessionId,
        (int) $userId
    );
    if ($owned === null) {
        $response['message'] = 'Session not found';
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }
    db_stmt_execute(
        $con,
        'UPDATE mess_meal_session SET session_name = ?, start_time = ?, end_time = ?, token_prefix = ?, is_active = ?, menu_notes = ?, sort_order = ? WHERE id = ? AND userId = ?',
        'ssssisiii',
        $sessionName,
        $startTime,
        $endTime,
        $tokenPrefix,
        $isActive,
        $menuNotes,
        $sortOrder,
        (int) $sessionId,
        (int) $userId
    );
    $response['sessionId'] = (string) (int) $sessionId;
} else {
    $id = db_stmt_insert_id(
        $con,
        'INSERT INTO mess_meal_session (userId, session_name, start_time, end_time, token_prefix, is_active, menu_notes, sort_order)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
        'issssisi',
        (int) $userId,
        $sessionName,
        $startTime,
        $endTime,
        $tokenPrefix,
        $isActive,
        $menuNotes,
        $sortOrder
    );
    $response['sessionId'] = $id ? (string) $id : '';
}

$response['status'] = '1';
$response['message'] = 'saved';
echo json_encode($response);
mysqli_close($con);
