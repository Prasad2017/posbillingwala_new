<?php
include_once('config.php');
require_once __DIR__ . '/owner_mess_qr_helpers.php';

mysqli_query($con, 'set names utf8mb4');
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$response = array('status' => '0', 'message' => 'Invalid request', 'hasQr' => '0', 'qr' => null);
mess_common_ensure_schema($con);

$ownerUserId = isset($_REQUEST['userId']) ? trim((string) $_REQUEST['userId']) : '';
$licenceId = isset($_REQUEST['licenceId']) ? trim((string) $_REQUEST['licenceId']) : '';
$resolved = owner_mess_qr_resolve_licence($con, $ownerUserId, $licenceId, $response);
if ($resolved === null) {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$shopUserId = $resolved['licenceId'];
mess_ensure_default_sessions($con, $shopUserId);

$row = db_stmt_fetch_one(
    $con,
    "SELECT * FROM mess_qr WHERE userId = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1",
    'i',
    $shopUserId
);

$response['status'] = '1';
$response['message'] = $row ? 'ok' : 'no active qr';
$response['hasQr'] = $row ? '1' : '0';
$response['qr'] = owner_mess_qr_payload($row);
$response['licenceId'] = (string) $shopUserId;
echo json_encode($response);
mysqli_close($con);
