<?php
/**
 * Update dealer profile fields.
 * POST: userId, dealerName, dealerMobileNumber, dealerAddress, dealerEmail, dealerAadhaarNumber
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../licence_expiry.php';
require_once __DIR__ . '/../user_identity.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'update failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    $response['message'] = 'Use POST';
    echo json_encode($response);
    exit;
}

admin_require_auth($con, $response);
mysqli_query($con, 'set names utf8');

$userId = isset($_POST['userId']) ? trim($_POST['userId']) : '';
$dealerName = isset($_POST['dealerName']) ? trim($_POST['dealerName']) : '';
$dealerMobileNumber = isset($_POST['dealerMobileNumber']) ? trim($_POST['dealerMobileNumber']) : '';
$dealerAddress = isset($_POST['dealerAddress']) ? trim($_POST['dealerAddress']) : '';
$dealerEmail = isset($_POST['dealerEmail']) ? trim($_POST['dealerEmail']) : '';
$dealerAadhaarNumber = isset($_POST['dealerAadhaarNumber'])
    ? preg_replace('/\D/', '', trim($_POST['dealerAadhaarNumber']))
    : '';

if ($userId === '' || $dealerName === '' || $dealerMobileNumber === '') {
    $response['message'] = 'userId, name and mobile are required.';
    echo json_encode($response);
    exit;
}
if ($dealerAadhaarNumber !== '' && strlen($dealerAadhaarNumber) !== 12) {
    $response['message'] = 'Aadhaar must be 12 digits.';
    echo json_encode($response);
    exit;
}

$uid = (int) $userId;
$existing = db_stmt_fetch_one(
    $con,
    "SELECT id FROM `users` WHERE id=? AND role_id='2' LIMIT 1",
    'i',
    $uid
);
if ($existing === null) {
    $response['message'] = 'Dealer not found.';
    echo json_encode($response);
    exit;
}

if ($dealerAadhaarNumber !== '') {
    if (user_dealer_aadhar_taken($con, $dealerAadhaarNumber, $uid)) {
        $response['message'] = 'Aadhaar already registered for another dealer.';
        echo json_encode($response);
        exit;
    }
}

$dealerMobileDigits = licence_normalize_contact($dealerMobileNumber);
if (strlen($dealerMobileDigits) < 10) {
    $response['message'] = 'Please enter a valid 10-digit mobile number.';
    echo json_encode($response);
    exit;
}

if (user_dealer_mobile_taken($con, $dealerMobileDigits, $uid)) {
    $response['message'] = 'This mobile number is already registered for another dealer.';
    echo json_encode($response);
    exit;
}

$ok = db_stmt_execute(
    $con,
    "UPDATE `users`
     SET `name`=?, `contact_number`=?, `address`=?, `email`=?, `aadhar_number`=?
     WHERE `id`=? AND `role_id`='2'",
    'sssssi',
    $dealerName,
    $dealerMobileDigits,
    $dealerAddress,
    $dealerEmail,
    $dealerAadhaarNumber,
    $uid
);

if ($ok) {
    $response['status'] = '1';
    $response['message'] = 'update successful!';
} else {
    $response['message'] = 'Unable to update dealer.';
}

mysqli_close($con);
echo json_encode($response);
