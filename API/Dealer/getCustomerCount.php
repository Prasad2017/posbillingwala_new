<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array(
    'status' => 'false',
    'message' => '0',
    'totalCustomer' => '0',
    'activeCustomer' => '0',
    'trialCustomer' => '0',
    'expiredCustomer' => '0',
    'activePercent' => '0',
    'trialPercent' => '0',
    'expiredPercent' => '0',
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

dealer_require_auth($con, $response);
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');
$today = date('Y-m-d');
$dealerId = isset($_GET['userId']) ? (int) $_GET['userId'] : 0;

if ($dealerId <= 0) {
    echo json_encode($response);
    exit;
}

$total = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `users` WHERE `role_id`='3' AND `dealerId`=?",
    'i',
    $dealerId
);
$active = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE u.role_id='3' AND u.dealerId=?
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)
       AND NOT (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')",
    'is',
    $dealerId,
    $today
);
$trial = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE u.role_id='3' AND u.dealerId=?
       AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    'is',
    $dealerId,
    $today
);
$expired = max(0, $total - $active - $trial);
$pct = function ($n) use ($total) {
    return $total > 0 ? (string) round(($n / $total) * 100.0, 1) : '0';
};

$response = array(
    'status' => 'true',
    'message' => (string) $total,
    'totalCustomer' => (string) $total,
    'activeCustomer' => (string) $active,
    'trialCustomer' => (string) $trial,
    'expiredCustomer' => (string) $expired,
    'activePercent' => $pct($active),
    'trialPercent' => $pct($trial),
    'expiredPercent' => $pct($expired),
);

mysqli_close($con);
echo json_encode($response);
