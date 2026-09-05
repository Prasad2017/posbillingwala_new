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

$response = array('status' => '0', 'message' => 'Invalid request', 'qr' => null);
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
$messLabel = isset($_POST['messLabel']) ? trim((string) $_POST['messLabel']) : '';
$branchLabel = isset($_POST['branchLabel']) ? trim((string) $_POST['branchLabel']) : '';

$prev = db_stmt_fetch_one(
    $con,
    "SELECT * FROM mess_qr WHERE userId = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1",
    'i',
    $shopUserId
);
$keepPrintDevice = $prev && !empty($prev['print_device_id']) ? $prev['print_device_id'] : '';
if ($messLabel === '' && $prev) {
    $messLabel = (string) $prev['mess_label'];
}
if ($branchLabel === '' && $prev) {
    $branchLabel = (string) $prev['branch_label'];
}
if ($messLabel === '') {
    $messLabel = 'Mess';
}
if ($branchLabel === '') {
    $branchLabel = 'Main Branch';
}

db_stmt_execute(
    $con,
    "UPDATE mess_qr SET status = 'INACTIVE', deactivated_at = NOW() WHERE userId = ? AND status = 'ACTIVE'",
    'i',
    $shopUserId
);

$publicToken = mess_random_token(24);
$ok = db_stmt_execute(
    $con,
    'INSERT INTO mess_qr (userId, public_token, status, print_device_id, mess_label, branch_label)
     VALUES (?, ?, \'ACTIVE\', ?, ?, ?)',
    'issss',
    $shopUserId,
    $publicToken,
    $keepPrintDevice,
    $messLabel,
    $branchLabel
);

if (!$ok) {
    $response['message'] = 'Unable to regenerate QR right now.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

mess_audit($con, $shopUserId, 'qr_regenerated', $publicToken, null, null, 'owner old invalidated');

$response['status'] = '1';
$response['message'] = 'regenerated';
$response['qr'] = array(
    'publicToken' => $publicToken,
    'status' => 'ACTIVE',
    'qrUrl' => mess_public_qr_url($publicToken),
    'messLabel' => $messLabel,
    'branchLabel' => $branchLabel,
    'printDeviceId' => $keepPrintDevice,
    'createdAt' => date('Y-m-d H:i:s'),
    'licenceId' => (string) $shopUserId,
);
echo json_encode($response);
mysqli_close($con);
