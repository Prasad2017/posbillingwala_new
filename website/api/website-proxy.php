<?php
/**
 * Same-origin proxy: browser → posbillingwala.com/api/website/* → admin API.
 *
 * Avoids browser failures when admin.posbillingwala.com has a self-signed /
 * untrusted SSL cert (fetch fails and the site shows a generic "CORS" message).
 *
 * Only whitelisted public website endpoints are forwarded.
 */

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate');
header('X-Content-Type-Options: nosniff');

$upstreamBase = getenv('PBW_ADMIN_API_BASE') ?: 'https://admin.posbillingwala.com/api/website';
$upstreamBase = rtrim($upstreamBase, '/');

$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
if ($method === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Accept, Content-Type');
    http_response_code(204);
    exit;
}

$path = isset($_GET['__path']) ? (string) $_GET['__path'] : '';
$path = trim($path, '/');

// Fallback if rewrite did not pass __path (e.g. direct script invoke).
if ($path === '' && !empty($_SERVER['PATH_INFO'])) {
    $path = trim((string) $_SERVER['PATH_INFO'], '/');
}

$allowedGet = [
    'clients' => true,
    'testimonials' => true,
    'dealers' => true,
    'pricing' => true,
    'products' => true,
    'settings' => true,
];

$isPage = (bool) preg_match('#^pages/[a-z0-9\-]+$#i', $path);
$isContact = ($path === 'contact');

if ($method === 'GET' && (isset($allowedGet[$path]) || $isPage)) {
    // ok
} elseif ($method === 'POST' && $isContact) {
    // ok
} else {
    http_response_code(404);
    echo json_encode([
        'success' => false,
        'message' => 'Unknown or disallowed website API path.',
    ]);
    exit;
}

$url = $upstreamBase . '/' . $path;

$body = null;
$contentType = $_SERVER['CONTENT_TYPE'] ?? $_SERVER['HTTP_CONTENT_TYPE'] ?? '';
if ($method === 'POST') {
    $body = file_get_contents('php://input');
    if ($body === false) {
        $body = '';
    }
}

$ch = curl_init($url);
$headers = ['Accept: application/json'];
if ($method === 'POST' && $contentType !== '') {
    $headers[] = 'Content-Type: ' . $contentType;
} elseif ($method === 'POST') {
    $headers[] = 'Content-Type: application/json';
}

curl_setopt_array($ch, [
    CURLOPT_CUSTOMREQUEST => $method,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_MAXREDIRS => 3,
    CURLOPT_CONNECTTIMEOUT => 8,
    CURLOPT_TIMEOUT => 20,
    CURLOPT_HTTPHEADER => $headers,
    // Admin subdomain may use a self-signed cert until AutoSSL / Let's Encrypt is issued.
    CURLOPT_SSL_VERIFYPEER => false,
    CURLOPT_SSL_VERIFYHOST => 0,
]);

if ($method === 'POST') {
    curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
}

$response = curl_exec($ch);
$errno = curl_errno($ch);
$error = curl_error($ch);
$status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($response === false || $errno) {
    http_response_code(502);
    echo json_encode([
        'success' => false,
        'message' => 'Admin API unreachable from website proxy.',
        'detail' => $error ?: ('curl errno ' . $errno),
    ]);
    exit;
}

if ($status < 100) {
    $status = 502;
}

http_response_code($status);
echo $response;
