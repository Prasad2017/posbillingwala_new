<?php
/**
 * Cron: daily licence-expiry push notifications (3 days before, once per day).
 *
 * Plain URL works for hosting "Fetch URL" cron jobs by default.
 * To require a secret, set $cronRequireSecret = true and $cronSecret in db_local.php.
 *
 * Examples:
 *   https://YOUR_HOST/androidApp/cron/notifyExpiringLicenses.php
 *   0 9 * * * php /path/to/androidApp/cron/notifyExpiringLicenses.php
 */

include_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../fcm_helper.php';
require_once __DIR__ . '/../fcm_tables.php';
require_once __DIR__ . '/cron_auth.php';

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

if (!cron_auth_guard($response)) {
    mysqli_close($con);
    exit;
}

if (PHP_SAPI !== 'cli' && $_SERVER['REQUEST_METHOD'] !== 'GET' && $_SERVER['REQUEST_METHOD'] !== 'POST') {
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
