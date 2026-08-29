<?php
include_once 'config.php';
require_once __DIR__ . '/../db_prepared.php';

mysqli_query($con, 'set names utf8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: X-Requested-With, Authorization, Content-Type');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Content-Type: application/json; charset=utf-8');

$response = array(
    'status' => '0',
    'message' => 'Unable to reset password.',
);

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    echo json_encode($response);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use Post Method';
    echo json_encode($response);
    exit;
}

$contactNumber = isset($_POST['contactNumber']) ? trim((string) $_POST['contactNumber']) : '';
$aadhaarNumber = isset($_POST['aadhaarNumber']) ? trim((string) $_POST['aadhaarNumber']) : '';
$newPassword = isset($_POST['newPassword']) ? (string) $_POST['newPassword'] : '';

$contactNumber = preg_replace('/\D+/', '', $contactNumber);
$aadhaarNumber = preg_replace('/\D+/', '', $aadhaarNumber);

if ($contactNumber === '' || strlen($contactNumber) < 10) {
    $response['message'] = 'Enter valid mobile number';
    echo json_encode($response);
    exit;
}

if ($aadhaarNumber === '' || strlen($aadhaarNumber) !== 12) {
    $response['message'] = 'Enter valid 12 digit Aadhaar number';
    echo json_encode($response);
    exit;
}

if ($newPassword === '') {
    $response['message'] = 'Enter new password';
    echo json_encode($response);
    exit;
}

if (strlen($newPassword) < 6) {
    $response['message'] = 'Password must be at least 6 characters';
    echo json_encode($response);
    exit;
}

$dealer = db_stmt_fetch_one(
    $con,
    "SELECT `id`, `is_active`, `role_id`
     FROM `users`
     WHERE `contact_number`=? AND `aadhar_number`=? AND `role_id`='2'
     LIMIT 1",
    'ss',
    $contactNumber,
    $aadhaarNumber
);

if ($dealer === null) {
    $response['message'] = 'Mobile number and Aadhaar do not match our records.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

if ((int) $dealer['is_active'] !== 1) {
    $response['message'] = 'Your dealer account is disabled. Please contact support.';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$hash = password_hash($newPassword, PASSWORD_BCRYPT);
$ok = db_stmt_execute(
    $con,
    'UPDATE `users` SET `password`=? WHERE `id`=? AND `role_id`=\'2\'',
    'si',
    $hash,
    (int) $dealer['id']
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'Password reset successfully. You can login with your new password.';
} else {
    $response['message'] = 'Unable to reset password.';
}

mysqli_close($con);
echo json_encode($response);
