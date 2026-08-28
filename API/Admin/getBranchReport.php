<?php
/**
 * Branch report — licenses treated as branches.
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
$monthStart = date('Y-m-01');

$total = (int) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM `licenses`", '');
$active = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses`
     WHERE LOWER(IFNULL(licenseStatus,'')) NOT IN ('expire','expired','suspended','revoked')
       AND (expiryDate IS NULL OR expiryDate='' OR expiryDate>=?)",
    's',
    $today
);
$inactive = max(0, $total - $active);
$newThisMonth = (int) db_stmt_scalar_int(
    $con,
    "SELECT COUNT(*) AS c FROM `licenses` WHERE DATE(created_at)>=?",
    's',
    $monthStart
);

$top = array();
$rows = db_stmt_fetch_all(
    $con,
    "SELECT u.id AS customerId, u.name AS customerName, u.shopName, COUNT(l.id) AS branchCount
     FROM `users` u
     INNER JOIN `licenses` l ON l.userId=u.id
     WHERE u.role_id='3'
     GROUP BY u.id, u.name, u.shopName
     ORDER BY branchCount DESC
     LIMIT 8",
    ''
);
foreach ($rows as $r) {
    $top[] = array(
        'customerId' => (string) $r['customerId'],
        'customerName' => (string) ($r['customerName'] ?? ''),
        'shopName' => (string) ($r['shopName'] ?? ''),
        'branchCount' => (string) $r['branchCount']
    );
}

$t = max(1, $total);
$response = array(
    'status' => 'true',
    'totalBranches' => (string) $total,
    'activeBranches' => (string) $active,
    'inactiveBranches' => (string) $inactive,
    'newBranches' => (string) $newThisMonth,
    'activePercent' => (string) round(($active / $t) * 100, 1),
    'inactivePercent' => (string) round(($inactive / $t) * 100, 1),
    'newPercent' => (string) round(($newThisMonth / $t) * 100, 1),
    'topCustomers' => $top
);
mysqli_close($con);
echo json_encode($response);
