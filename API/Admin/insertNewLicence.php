<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $name = $_POST['name'];
    $contact_number = $_POST['contact_number'];
    $address = $_POST['address'];
    $shopName = $_POST['shopName'];
    $licenseKey = $_POST['licenseKey'];
    $licenseValidity = $_POST['licenseValidity'];
    $licenseType = $_POST['licenseType'];
    $amount = $_POST['amount'];
    $fastBilling = $_POST['fastBilling'];
    $takeAway = $_POST['takeAway'];
    $dineIn = $_POST['dineIn'];
    $customerId = $_POST['customerId'];
    $branchName = isset($_POST['branchName']) ? trim($_POST['branchName']) : '';

    date_default_timezone_set('Asia/Kolkata');
    $date = date('Y-m-d');

    $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
    if ($licenseType == 'Demo') {
        $paymentStatus = '';
    } else {
        $paymentStatus = 'cash';
    }

    $branchLabel = $branchName !== '' ? $branchName : $shopName;
    $expiryDate = date('Y-m-d', strtotime($date . ' +' . $licenseValidity . ' day'));

    $sql = "INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `fastBilling`, `takeAway`, `dineIn`)
            VALUES ('$customerId', '$licenseKey', '$licenseValidity', '$licenseType', 'active', '$expiryDate', '$paymentStatus', '$amount', 'franchise', '$branchLabel', '$fastBilling', '$takeAway', '$dineIn')";
    if (mysqli_query($con, $sql)) {
        $response['status'] = 'true';
        $response['message'] = 'Franchise branch registered. Same customer account — new licence key issued.';
        $response['branchLabel'] = licence_branch_label('franchise', $branchLabel);
    } else {
        $response['status'] = 'false';
        $response['message'] = 'licence registration failed...';
    }
}
header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
