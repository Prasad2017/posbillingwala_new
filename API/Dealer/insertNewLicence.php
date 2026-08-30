<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../auth_tokens.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    if (!auth_actor_token_valid_or_legacy($con, 'dealer')) {
        $response['status'] = '0';
        $response['message'] = 'Invalid or expired auth token';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

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
    $mess = isset($_POST['mess']) ? $_POST['mess'] : '0';
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
    $defaultMpin = licence_default_mpin();

    $sql = "INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `mpin`, `fastBilling`, `takeAway`, `dineIn`, `mess`)
            VALUES ('$customerId', '$licenseKey', '$licenseValidity', '$licenseType', 'active', '$expiryDate', '$paymentStatus', '$amount', 'franchise', '$branchLabel', '$defaultMpin', '$fastBilling', '$takeAway', '$dineIn', '$mess')";
    if (mysqli_query($con, $sql)) {
        $response['status'] = 'true';
        $response['message'] = 'Franchise branch registered. Same customer account — new licence key issued.';
        $response['licenseKey'] = $licenseKey;
        $response['mpin'] = $defaultMpin;
        $response['branchLabel'] = licence_branch_label('franchise', $branchLabel);
    } else {
        $response['status'] = 'false';
        $response['message'] = 'licence registration failed...';
    }
}
header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
