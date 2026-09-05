<?php
include_once('config.php');
require_once __DIR__ . '/owner_mess_qr_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization');
header('Access-Control-Allow-Methods: POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$response = array('status' => '0', 'message' => 'Invalid request');
mess_common_ensure_schema($con);

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$ownerUserId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
$licenceId = isset($_POST['licenceId']) ? trim((string) $_POST['licenceId']) : '';
$resolved = owner_mess_qr_resolve_licence($con, $ownerUserId, $licenceId, $response);
if ($resolved === null) {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$shopUserId = $resolved['licenceId'];
$status = isset($_POST['status']) ? strtoupper(trim((string) $_POST['status'])) : '';
if ($status !== 'ACTIVE' && $status !== 'INACTIVE') {
    $response['message'] = 'Invalid status';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($status === 'INACTIVE') {
    db_stmt_execute(
        $con,
        "UPDATE mess_qr SET status = 'INACTIVE', deactivated_at = NOW() WHERE userId = ? AND status = 'ACTIVE'",
        'i',
        $shopUserId
    );
    mess_audit($con, $shopUserId, 'qr_deactivated', null, null, null, 'owner');
    $response['status'] = '1';
    $response['message'] = 'deactivated';
    $response['qrStatus'] = 'INACTIVE';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$latest = db_stmt_fetch_one(
    $con,
    'SELECT * FROM mess_qr WHERE userId = ? ORDER BY id DESC LIMIT 1',
    'i',
    $shopUserId
);
if ($latest === null) {
    $response['message'] = 'No QR found. Generate first.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

db_stmt_execute(
    $con,
    "UPDATE mess_qr SET status = 'INACTIVE', deactivated_at = NOW() WHERE userId = ? AND status = 'ACTIVE' AND id != ?",
    'ii',
    $shopUserId,
    (int) $latest['id']
);
db_stmt_execute(
    $con,
    "UPDATE mess_qr SET status = 'ACTIVE', deactivated_at = NULL WHERE id = ?",
    'i',
    (int) $latest['id']
);

mess_audit($con, $shopUserId, 'qr_activated', $latest['public_token'], null, null, 'owner');
$response['status'] = '1';
$response['message'] = 'activated';
$response['qrStatus'] = 'ACTIVE';
$response['qr'] = owner_mess_qr_payload($latest);
echo json_encode($response);
mysqli_close($con);
