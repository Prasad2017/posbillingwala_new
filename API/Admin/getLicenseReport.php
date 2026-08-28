<?php
/**
 * License report KPIs + donut + expiry windows.
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
$in30 = date('Y-m-d', strtotime('+30 days'));

$active = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses` l
     WHERE LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)
       AND NOT (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')",
    's',
    $today
);
$trial = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses` l
     WHERE (l.licenseType IN ('Demo','Trial') OR l.licenseValidity='7')
       AND LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (l.expiryDate IS NULL OR l.expiryDate='' OR l.expiryDate>=?)",
    's',
    $today
);
$expiring = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses` l
     WHERE LOWER(IFNULL(l.licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND l.expiryDate IS NOT NULL AND l.expiryDate<>''
       AND l.expiryDate>=? AND l.expiryDate<=?",
    'ss',
    $today,
    $in30
);
$expired = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses` l
     WHERE LOWER(IFNULL(l.licenseStatus,'')) IN ('expire','expired')
        OR (l.expiryDate IS NOT NULL AND l.expiryDate<>'' AND l.expiryDate<?)",
    's',
    $today
);
$total = max(1, $active + $trial + $expiring + $expired);
$pct = function ($n) use ($total) {
    return (string) round(($n / $total) * 100.0, 1);
};

$windows = array();
$buckets = array(
    array(0, 7, 'Next 7 days'),
    array(8, 15, '8 - 15 days'),
    array(16, 30, '16 - 30 days')
);
foreach ($buckets as $b) {
    $from = date('Y-m-d', strtotime('+' . $b[0] . ' days'));
    $to = date('Y-m-d', strtotime('+' . $b[1] . ' days'));
    $c = (int) db_stmt_scalar_int(
        $con,
        "SELECT COUNT(*) AS c FROM `licenses`
         WHERE expiryDate>=? AND expiryDate<=?
           AND LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')",
        'ss',
        $from,
        $to
    );
    $windows[] = array(
        'label' => $b[2] . ' (' . date('d M', strtotime($from)) . ' - ' . date('d M Y', strtotime($to)) . ')',
        'count' => (string) $c
    );
}

$response = array(
    'status' => 'true',
    'activeLicenses' => (string) $active,
    'trialLicenses' => (string) $trial,
    'expiringLicenses' => (string) $expiring,
    'expiredLicenses' => (string) $expired,
    'totalLicenses' => (string) ($active + $trial + $expiring + $expired),
    'activePercent' => $pct($active),
    'trialPercent' => $pct($trial),
    'expiringPercent' => $pct($expiring),
    'expiredPercent' => $pct($expired),
    'expiryWindows' => $windows
);
mysqli_close($con);
echo json_encode($response);
