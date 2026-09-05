<?php
/**
 * Quick FCM config check (Admin auth required).
 * GET /Admin/fcmStatus.php
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../fcm_helper.php';
require_once __DIR__ . '/../fcm_project.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'failed');

admin_require_auth($con, $response);

$cfg = fcm_is_configured();
$tokenCount = 0;
$col = @mysqli_query($con, "SELECT COUNT(*) AS c FROM licenses WHERE fcm_token IS NOT NULL AND TRIM(fcm_token) <> ''");
if ($col) {
    $row = mysqli_fetch_assoc($col);
    $tokenCount = $row ? (int) $row['c'] : 0;
}

echo json_encode(array(
    'status' => !empty($cfg['ok']) ? '1' : '0',
    'message' => $cfg['message'],
    'fcmOk' => !empty($cfg['ok']) ? '1' : '0',
    'fcmMode' => $cfg['mode'],
    'fcmProjectId' => $cfg['projectId'],
    'expectedProjectId' => FCM_DEFAULT_PROJECT_ID,
    'expectedFromGoogleServices' => 'pos-billingwala',
    'devicesWithToken' => (string) $tokenCount,
));
mysqli_close($con);
