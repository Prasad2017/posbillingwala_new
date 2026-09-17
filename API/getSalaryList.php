<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

pos_api_headers();
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    pos_api_json(array('status' => '0', 'message' => 'Use Post Method'));
}

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'user.view');
pos_schema_ensure($con);

$month = pos_api_post('salaryMonth', date('Y-m'));
if (!preg_match('/^\d{4}-\d{2}$/', $month)) {
    $month = date('Y-m');
}

$staffRows = db_stmt_fetch_all(
    $con,
    "SELECT * FROM `pos_staff` WHERE `licenseId`=? AND UPPER(`status`)='ACTIVE' ORDER BY `name` ASC",
    'i',
    (int) $licenceId
);
$payments = db_stmt_fetch_all(
    $con,
    'SELECT * FROM `pos_salary_payment` WHERE `licenseId`=? AND `salaryMonth`=?',
    'is',
    (int) $licenceId,
    $month
);
$payByStaff = array();
foreach ($payments as $p) {
    $payByStaff[(string) $p['staffId']] = $p;
}

$list = array();
$totalDue = 0.0;
$totalPaid = 0.0;
foreach ($staffRows as $row) {
    $sid = (string) $row['id'];
    $due = isset($row['monthlySalary']) ? (float) $row['monthlySalary'] : 0.0;
    $pay = isset($payByStaff[$sid]) ? $payByStaff[$sid] : null;
    $paidAmount = $pay !== null ? (float) $pay['amount'] : 0.0;
    $status = $pay !== null ? 'PAID' : 'PENDING';
    $totalDue += $due;
    $totalPaid += $paidAmount;
    $list[] = array(
        'staffId' => $sid,
        'staffName' => $row['name'],
        'role' => $row['role'],
        'monthlySalary' => number_format($due, 2, '.', ''),
        'paymentStatus' => $status,
        'paidAmount' => number_format($paidAmount, 2, '.', ''),
        'paidOn' => $pay !== null ? (string) $pay['paidOn'] : '',
        'note' => $pay !== null ? (string) $pay['note'] : '',
        'paymentId' => $pay !== null ? (string) $pay['id'] : '',
    );
}

pos_api_json(array(
    'status' => '1',
    'message' => 'ok',
    'salaryMonth' => $month,
    'totalDue' => number_format($totalDue, 2, '.', ''),
    'totalPaid' => number_format($totalPaid, 2, '.', ''),
    'pendingCount' => (string) count(array_filter($list, function ($r) {
        return $r['paymentStatus'] === 'PENDING';
    })),
    'salaryResponse' => $list,
));
