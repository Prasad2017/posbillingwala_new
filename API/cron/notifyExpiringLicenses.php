<?php
/**
 * Cron: daily licence-expiry push notifications (3 days before, once per day).
 *
 * Auth: shared secret via header X-Cron-Secret, or query/body `secret` / `key`.
 *
 * Example crontab (daily 09:00 Asia/Kolkata):
 *   0 9 * * * curl -fsS -H "X-Cron-Secret: YOUR_SECRET" "https://YOUR_HOST/API/cron/notifyExpiringLicenses.php"
 */

include_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../fcm_helper.php';
require_once __DIR__ . '/../fcm_tables.php';

mysqli_query($con, 'set names utf8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization, X-Cron-Secret');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$response = array(
    'status' => '0',
    'message' => 'Unauthorized',
);

$configuredSecret = '';
if (isset($cronSecret) && is_string($cronSecret) && $cronSecret !== '') {
    $configuredSecret = $cronSecret;
} else {
    $envSecret = getenv('CRON_SECRET');
    if (is_string($envSecret) && $envSecret !== '') {
        $configuredSecret = $envSecret;
    }
}

if ($configuredSecret === '') {
    http_response_code(503);
    $response['message'] = 'Cron secret not configured. Set $cronSecret in db_local.php or CRON_SECRET env.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$provided = '';
if (!empty($_SERVER['HTTP_X_CRON_SECRET'])) {
    $provided = trim((string) $_SERVER['HTTP_X_CRON_SECRET']);
} elseif (isset($_GET['secret'])) {
    $provided = trim((string) $_GET['secret']);
} elseif (isset($_GET['key'])) {
    $provided = trim((string) $_GET['key']);
} elseif (isset($_POST['secret'])) {
    $provided = trim((string) $_POST['secret']);
} elseif (isset($_POST['key'])) {
    $provided = trim((string) $_POST['key']);
}

if ($provided === '' || !hash_equals($configuredSecret, $provided)) {
    http_response_code(401);
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'GET' && $_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    $response['message'] = 'Use GET or POST';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$daysBefore = 3;
if (isset($_GET['daysBefore'])) {
    $daysBefore = (int) $_GET['daysBefore'];
} elseif (isset($_POST['daysBefore'])) {
    $daysBefore = (int) $_POST['daysBefore'];
}

$stats = fcm_notify_expiring_licenses($con, $daysBefore);

$response['status'] = '1';
$response['message'] = 'Licence expiry push notifications completed';
foreach ($stats as $key => $value) {
    $response[$key] = $value;
}

echo json_encode($response);
mysqli_close($con);
