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
if ($messLabel === '') {
    $messLabel = isset($resolved['licence']['userName']) ? trim((string) $resolved['licence']['userName']) : 'Mess';
}
if ($branchLabel === '') {
    $branchLabel = 'Main Branch';
}

$existing = db_stmt_fetch_one(
    $con,
    "SELECT * FROM mess_qr WHERE userId = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1",
    'i',
    $shopUserId
);

if ($existing) {
    // Owner must not steal POS print device — only update labels if provided.
    if ($messLabel !== '' || $branchLabel !== '') {
        db_stmt_execute(
            $con,
            "UPDATE mess_qr SET
                mess_label = IF(? = '', mess_label, ?),
                branch_label = IF(? = '', branch_label, ?)
             WHERE id = ?",
            'ssssi',
            $messLabel,
            $messLabel,
            $branchLabel,
            $branchLabel,
            (int) $existing['id']
        );
        if ($messLabel !== '') {
            $existing['mess_label'] = $messLabel;
        }
        if ($branchLabel !== '') {
            $existing['branch_label'] = $branchLabel;
        }
    }
    mess_audit($con, $shopUserId, 'qr_get_existing', $existing['public_token'], null, null, 'owner already active');
    $response['status'] = '1';
    $response['message'] = 'already active';
    $response['qr'] = owner_mess_qr_payload($existing);
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$publicToken = mess_random_token(24);
$ok = db_stmt_execute(
    $con,
    'INSERT INTO mess_qr (userId, public_token, status, print_device_id, mess_label, branch_label)
     VALUES (?, ?, \'ACTIVE\', ?, ?, ?)',
    'issss',
    $shopUserId,
    $publicToken,
    '',
    $messLabel,
    $branchLabel
);

if (!$ok) {
    $response['message'] = 'Unable to generate QR right now.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

mess_ensure_default_sessions($con, $shopUserId);
mess_audit($con, $shopUserId, 'qr_generated', $publicToken, null, null, 'owner');

$response['status'] = '1';
$response['message'] = 'generated';
$response['qr'] = array(
    'publicToken' => $publicToken,
    'status' => 'ACTIVE',
    'qrUrl' => mess_public_qr_url($publicToken),
    'messLabel' => $messLabel,
    'branchLabel' => $branchLabel,
    'printDeviceId' => '',
    'createdAt' => date('Y-m-d H:i:s'),
    'licenceId' => (string) $shopUserId,
);
echo json_encode($response);
mysqli_close($con);
