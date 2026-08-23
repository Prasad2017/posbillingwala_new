<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $userId = isset($_POST['userId']) ? $_POST['userId'] : '';
    $dealerName = isset($_POST['dealerName']) ? $_POST['dealerName'] : '';
    $dealerMobileNumber = isset($_POST['dealerMobileNumber']) ? $_POST['dealerMobileNumber'] : '';
    $dealerAddress = isset($_POST['dealerAddress']) ? $_POST['dealerAddress'] : '';
    $dealerEmail = isset($_POST['dealerEmail']) ? $_POST['dealerEmail'] : '';
    $dealerAadhaarNumber = isset($_POST['dealerAadhaarNumber']) ? $_POST['dealerAadhaarNumber'] : '';

    $userIdEsc = mysqli_real_escape_string($con, $userId);
    $dealerNameEsc = mysqli_real_escape_string($con, $dealerName);
    $dealerMobileNumberEsc = mysqli_real_escape_string($con, $dealerMobileNumber);
    $dealerAddressEsc = mysqli_real_escape_string($con, $dealerAddress);
    $dealerEmailEsc = mysqli_real_escape_string($con, $dealerEmail);
    $dealerAadhaarNumberEsc = mysqli_real_escape_string($con, $dealerAadhaarNumber);

    $sth = "UPDATE `users` SET `name`='$dealerNameEsc', `contact_number`='$dealerMobileNumberEsc', `address`='$dealerAddressEsc',
            `email`='$dealerEmailEsc', `aadhar_number`='$dealerAadhaarNumberEsc' WHERE `id`='$userIdEsc'";

    if (mysqli_query($con, $sth)) {
        $response['status'] = '1';
        $response['message'] = 'update successful!';
    } else {
        $response['status'] = '0';
        $response['message'] = 'update failed...';
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
