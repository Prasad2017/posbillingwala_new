<?php
include_once('config.php');
require_once __DIR__ . '/auth_tokens.php';
require_once __DIR__ . '/db_prepared.php';

mysqli_query($con, 'set names utf8');
header('Content-Type: application/json; charset=utf-8');

$response = array('status' => 'false', 'message' => 'Logout failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST' && $_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

$plainToken = auth_token_from_request();
if ($plainToken !== null && $plainToken !== '') {
    auth_token_revoke($con, $plainToken);
    $response['status'] = 'true';
    $response['message'] = 'Logout successfully';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

// Legacy: clear device binding only when licence key matches authenticated device fields
$licenseKey = isset($_GET['licenceKey']) ? trim((string) $_GET['licenceKey']) : '';
if ($licenseKey === '' && isset($_POST['licenceKey'])) {
    $licenseKey = trim((string) $_POST['licenceKey']);
}

if ($licenseKey === '') {
    $response['message'] = 'Unauthorized — token or licence key required';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

// Without a Bearer token, refuse device unbind (prevents open logout abuse)
$response['message'] = 'Unauthorized — login required';
echo json_encode($response);
mysqli_close($con);
?>
