<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

pos_api_headers();
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    pos_api_json(array('status' => '0', 'message' => 'Use Post Method'));
}

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'user.edit');
pos_schema_ensure($con);

$staffId = (int) pos_api_post('staffId');
$month = pos_api_post('salaryMonth', date('Y-m'));
$amount = (float) pos_api_post('amount', '0');
$paidOn = pos_api_post('paidOn', date('Y-m-d'));
$note = pos_api_post('note');

if ($staffId <= 0 || !preg_match('/^\d{4}-\d{2}$/', $month)) {
    pos_api_json(array('status' => '0', 'message' => 'Enter staff and month (YYYY-MM)'));
}
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $paidOn)) {
    $paidOn = date('Y-m-d');
}

$staff = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `pos_staff` WHERE `id`=? AND `licenseId`=? LIMIT 1',
    'ii',
    $staffId,
    (int) $licenceId
);
if ($staff === null) {
    pos_api_json(array('status' => '0', 'message' => 'Staff not found'));
}
if ($amount <= 0) {
    $amount = isset($staff['monthlySalary']) ? (float) $staff['monthlySalary'] : 0;
}
if ($amount <= 0) {
    pos_api_json(array('status' => '0', 'message' => 'Set monthly salary first'));
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_salary_payment` WHERE `staffId`=? AND `salaryMonth`=? LIMIT 1',
    'is',
    $staffId,
    $month
);
if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_salary_payment` SET `amount`=?, `paidOn`=?, `note`=? WHERE `id`=?',
        'dssi',
        $amount,
        $paidOn,
        $note,
        (int) $existing['id']
    );
    $paymentId = (int) $existing['id'];
} else {
    $paymentId = db_stmt_insert_id(
        $con,
        'INSERT INTO `pos_salary_payment` (`licenseId`, `staffId`, `salaryMonth`, `amount`, `paidOn`, `note`)
         VALUES (?, ?, ?, ?, ?, ?)',
        'iisdss',
        (int) $licenceId,
        $staffId,
        $month,
        $amount,
        $paidOn,
        $note
    );
    if ($paymentId === false) {
        pos_api_json(array('status' => '0', 'message' => 'Unable to save payment'));
    }
}

pos_audit($con, $licenceId, 'Salary Paid', 'staff', $staffId, pos_posted_staff_id(), array(
    'salaryMonth' => $month,
    'amount' => $amount,
));

pos_api_json(array(
    'status' => '1',
    'message' => 'Salary payment saved',
    'paymentId' => (string) $paymentId,
    'staffId' => (string) $staffId,
    'salaryMonth' => $month,
    'amount' => number_format($amount, 2, '.', ''),
    'paidOn' => $paidOn,
    'note' => $note,
));
