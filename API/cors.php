<?php
/**
 * CORS for browser clients calling androidApp endpoints (Flutter web, admin, etc.).
 * Matches Login.php allowlists; also allows Accept for fetch/XHR.
 */
if (defined('POS_CORS_APPLIED')) {
    return;
}
define('POS_CORS_APPLIED', true);

if (php_sapi_name() === 'cli') {
    return;
}

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Authorization, Content-Type, Accept, X-Requested-With');

if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}
