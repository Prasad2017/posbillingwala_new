<?php
/**
 * Device report from licenses.android_device_id + live presence.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../pos_presence.php';

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
$mins = (int) LICENCE_ONLINE_MINUTES;
$tokenSub = licence_token_last_used_subquery('l');
$loginExpr = licence_has_last_login_column($con) ? 'l.`lastLoginAt`' : 'NULL';

$total = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses`
     WHERE android_device_id IS NOT NULL AND TRIM(android_device_id)<>''",
    ''
);
$active = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses`
     WHERE android_device_id IS NOT NULL AND TRIM(android_device_id)<>''
       AND LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (expiryDate IS NULL OR expiryDate='' OR expiryDate>=?)",
    's',
    $today
);
$inactive = max(0, $total - $active);
$notUsed = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses`
     WHERE android_device_id IS NOT NULL AND TRIM(android_device_id)<>''
       AND (LOWER(IFNULL(licenseStatus,'')) IN ('expire','expired')
            OR (expiryDate IS NOT NULL AND expiryDate<>'' AND expiryDate < DATE_SUB(?, INTERVAL 30 DAY)))",
    's',
    $today
);
$online = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses` l
     WHERE l.android_device_id IS NOT NULL AND TRIM(l.android_device_id)<>''
       AND GREATEST(
         COALESCE({$loginExpr}, '1970-01-01 00:00:00'),
         COALESCE({$tokenSub}, '1970-01-01 00:00:00')
       ) >= DATE_SUB(NOW(), INTERVAL {$mins} MINUTE)",
    ''
);

$top = array();
$rows = db_stmt_fetch_all(
    $con,
    "SELECT u.id AS customerId, u.name AS customerName, u.shopName,
            COUNT(l.id) AS deviceCount
     FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE u.role_id='3' AND l.android_device_id IS NOT NULL AND TRIM(l.android_device_id)<>''
     GROUP BY u.id, u.name, u.shopName
     ORDER BY deviceCount DESC
     LIMIT 8",
    ''
);
foreach ($rows as $r) {
    $top[] = array(
        'customerId' => (string) $r['customerId'],
        'customerName' => (string) ($r['customerName'] ?? ''),
        'shopName' => (string) ($r['shopName'] ?? ''),
        'deviceCount' => (string) $r['deviceCount']
    );
}

$t = max(1, $total);
$response = array(
    'status' => 'true',
    'totalDevices' => (string) $total,
    'activeDevices' => (string) $active,
    'inactiveDevices' => (string) $inactive,
    'notUsedDevices' => (string) $notUsed,
    'onlineDevices' => (string) $online,
    'offlineDevices' => (string) max(0, $total - $online),
    'activePercent' => (string) round(($active / $t) * 100, 1),
    'inactivePercent' => (string) round(($inactive / $t) * 100, 1),
    'notUsedPercent' => (string) round(($notUsed / $t) * 100, 1),
    'onlinePercent' => (string) round(($online / $t) * 100, 1),
    'topCustomers' => $top
);
mysqli_close($con);
echo json_encode($response);
