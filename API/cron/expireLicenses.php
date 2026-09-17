<?php
/**
 * Cron: auto-expire past-due licences and refresh remaining days.
 *
 * Plain URL works for hosting "Fetch URL" cron jobs by default.
 * To require a secret, set in db_local.php:
 *   $cronRequireSecret = true;
 *   $cronSecret = 'long-random-string';
 * then call with ?secret=… or header X-Cron-Secret.
 *
 * Examples:
 *   https://YOUR_HOST/androidApp/cron/expireLicenses.php
 *   5 0 * * * php /path/to/androidApp/cron/expireLicenses.php
 */

include_once __DIR__ . '/../config.php';
include_once __DIR__ . '/../licence_expiry.php';
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

$stats = licence_cron_sync_expiry($con);

$response['status'] = '1';
$response['message'] = 'Licence expiry sync completed';
$response['today'] = $stats['today'];
$response['expiredCount'] = (string) $stats['expiredCount'];
$response['refreshedCount'] = (string) $stats['refreshedCount'];

echo json_encode($response);
mysqli_close($con);
