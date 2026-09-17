<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';

pos_api_headers();
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    pos_api_json(array('status' => '0', 'message' => 'Use Post Method'));
}

$licenceId = pos_api_require_licence($con);
pos_require_permission($con, $licenceId, 'user.edit');

$staffId = (int) pos_api_post('staffId');
$salary = (float) pos_api_post('monthlySalary', '0');
if ($staffId <= 0 || $salary < 0) {
    pos_api_json(array('status' => '0', 'message' => 'Enter valid staff and salary'));
}

$row = db_stmt_fetch_one(
    $con,
    'SELECT * FROM `pos_staff` WHERE `id`=? AND `licenseId`=? LIMIT 1',
    'ii',
    $staffId,
    (int) $licenceId
);
if ($row === null) {
    pos_api_json(array('status' => '0', 'message' => 'Staff not found'));
}

db_stmt_execute(
    $con,
    'UPDATE `pos_staff` SET `monthlySalary`=? WHERE `id`=? AND `licenseId`=?',
    'dii',
    $salary,
    $staffId,
    (int) $licenceId
);
pos_audit($con, $licenceId, 'Salary Updated', 'staff', $staffId, pos_posted_staff_id(), array(
    'monthlySalary' => $salary,
));

$row['monthlySalary'] = $salary;
pos_api_json(array(
    'status' => '1',
    'message' => 'Salary saved',
    'staff' => pos_staff_public($row),
));
