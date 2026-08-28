<?php
/**
 * Customer report KPIs + status mix + growth bars.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'false');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');
date_default_timezone_set('Asia/Kolkata');
$today = date('Y-m-d');

$total = (int) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `users` WHERE role_id='3'", '');
$active = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE u.role_id='3'
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)
       AND NOT (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')",
    's',
    $today
);
$trial = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(DISTINCT u.id) AS c FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE u.role_id='3'
       AND (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    's',
    $today
);
$expired = max(0, $total - $active - $trial);
$pct = function ($n, $t) {
    return $t > 0 ? (string) round(($n / $t) * 100.0, 1) : '0';
};

$growth = array();
for ($i = 6; $i >= 0; $i--) {
    $d = date('Y-m-d', strtotime("-{$i} days"));
    $c = (int) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `users` WHERE role_id='3' AND DATE(created_at)=?",
        's',
        $d
    );
    $growth[] = array('label' => date('d M', strtotime($d)), 'count' => (string) $c);
}

$response = array(
    'status' => 'true',
    'totalCustomer' => (string) $total,
    'activeCustomer' => (string) $active,
    'trialCustomer' => (string) $trial,
    'expiredCustomer' => (string) $expired,
    'activePercent' => $pct($active, $total),
    'trialPercent' => $pct($trial, $total),
    'expiredPercent' => $pct($expired, $total),
    'growthBars' => $growth
);
mysqli_close($con);
echo json_encode($response);
