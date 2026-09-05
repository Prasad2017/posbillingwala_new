<?php
/**
 * Owner: today's Mess meal token counts across licences/branches for this owner.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../mess_common_helpers.php';
require_once __DIR__ . '/../db_prepared.php';

owner_require_auth($con);
mess_common_ensure_schema($con);
date_default_timezone_set('Asia/Kolkata');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Invalid request',
    'counts' => array(),
    'tokens' => array(),
);

$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = auth_user_id_from_request($con, $userId, 'owner');
if ($userId === null) {
    $response['message'] = 'Invalid or expired auth token';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$date = isset($_GET['date']) ? trim((string) $_GET['date']) : date('Y-m-d');
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
    $date = date('Y-m-d');
}

// Owner userId maps to organization; POS tokens use licence id as userId.
// Aggregate tokens for all licences owned by this user, OR tokens where mess_qr.userId is a licence of this owner.
$licenceRows = db_stmt_fetch_all(
    $con,
    'SELECT id FROM licenses WHERE userId = ?',
    'i',
    (int) $userId
);
$licenceIds = array();
foreach ($licenceRows as $lr) {
    $licenceIds[] = (int) $lr['id'];
}
// Also include direct owner id in case shops use owner id
$licenceIds[] = (int) $userId;
$licenceIds = array_values(array_unique($licenceIds));

if (empty($licenceIds)) {
    $response['status'] = '1';
    $response['message'] = 'ok';
    $response['date'] = $date;
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$placeholders = implode(',', array_fill(0, count($licenceIds), '?'));
$types = str_repeat('i', count($licenceIds)) . 's';
$params = $licenceIds;
$params[] = $date;

$rows = db_stmt_fetch_all(
    $con,
    "SELECT * FROM mess_meal_token WHERE userId IN ($placeholders) AND token_date = ? ORDER BY id DESC",
    $types,
    ...$params
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
        'branchUserId' => (string) $row['userId'],
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
$response['counts'] = array_values($bySession);
$response['tokens'] = $tokens;
echo json_encode($response);
mysqli_close($con);
