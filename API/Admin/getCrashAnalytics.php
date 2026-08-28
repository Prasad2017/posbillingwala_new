<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/admin_tables.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => 'true', 'byApp' => array(), 'overTime' => array(), 'topErrors' => array(), 'totalCrashes' => '0');
admin_require_auth($con, $response);
admin_ensure_support_crash_tables($con);

$total = (int) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM admin_crash_logs", '');
$byAppRows = db_stmt_fetch_all($con, "SELECT app_name, COUNT(*) AS c FROM admin_crash_logs GROUP BY app_name", '');
$byApp = array();
foreach ($byAppRows as $r) {
    $c = (int) $r['c'];
    $byApp[] = array(
        'label' => (string) $r['app_name'],
        'count' => (string) $c,
        'percent' => $total > 0 ? (string) round(($c / $total) * 100, 1) : '0'
    );
}
$overTime = array();
for ($i = 6; $i >= 0; $i--) {
    $d = date('Y-m-d', strtotime("-{$i} days"));
    $c = (int) db_stmt_scalar_int($con, "SELECT COUNT(*) AS c FROM admin_crash_logs WHERE DATE(created_at)=?", 's', $d);
    $overTime[] = array('date' => $d, 'total' => (string) $c);
}
$top = db_stmt_fetch_all(
    $con,
    "SELECT error_title, SUM(occurrences) AS c FROM admin_crash_logs GROUP BY error_title ORDER BY c DESC LIMIT 5",
    ''
);
$topErrors = array();
foreach ($top as $r) {
    $c = (int) $r['c'];
    $topErrors[] = array(
        'label' => (string) $r['error_title'],
        'count' => (string) $c,
        'percent' => $total > 0 ? (string) round(($c / max(1, $total)) * 100, 1) : '0'
    );
}
$response = array(
    'status' => 'true',
    'totalCrashes' => (string) $total,
    'periodLabel' => date('d M Y', strtotime('-6 days')) . ' - ' . date('d M Y'),
    'byApp' => $byApp,
    'overTime' => $overTime,
    'topErrors' => $topErrors
);
mysqli_close($con);
echo json_encode($response);
