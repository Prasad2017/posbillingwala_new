<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../business_template_ops.php';

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

    $name = isset($_POST['name']) ? $_POST['name'] : '';
    $contact_number = isset($_POST['contact_number']) ? $_POST['contact_number'] : '';
    $address = isset($_POST['address']) ? $_POST['address'] : '';
    $shopName = isset($_POST['shopName']) ? $_POST['shopName'] : '';
    $licenseKey = isset($_POST['licenseKey']) ? $_POST['licenseKey'] : '';
    $licenseValidity = isset($_POST['licenseValidity']) ? $_POST['licenseValidity'] : '';
    $licenseType = isset($_POST['licenseType']) ? $_POST['licenseType'] : '';
    $amount = isset($_POST['amount']) ? $_POST['amount'] : '0';
    $fastBilling = isset($_POST['fastBilling']) ? $_POST['fastBilling'] : '0';
    $takeAway = isset($_POST['takeAway']) ? $_POST['takeAway'] : '0';
    $dineIn = isset($_POST['dineIn']) ? $_POST['dineIn'] : '0';
    $mess = isset($_POST['mess']) ? $_POST['mess'] : '0';
    $customerId = isset($_POST['customerId']) ? $_POST['customerId'] : '';
    $branchName = isset($_POST['branchName']) ? trim($_POST['branchName']) : '';
    $businessType = isset($_POST['businessType']) ? trim($_POST['businessType']) : 'restaurant';
    $businessTemplateId = isset($_POST['businessTemplateId'])
        ? trim($_POST['businessTemplateId']) : 'restaurant_default';
    if ($businessType === '') {
        $businessType = 'restaurant';
    }
    if ($businessTemplateId === '') {
        $businessTemplateId = 'restaurant_default';
    }

    if ((string) $fastBilling === '0' && (string) $takeAway === '0'
        && (string) $dineIn === '0' && (string) $mess === '0') {
        $mods = business_template_default_licence_modules($businessType, $businessTemplateId);
        $fastBilling = $mods['fastBilling'];
        $takeAway = $mods['takeAway'];
        $dineIn = $mods['dineIn'];
        $mess = $mods['mess'];
    }

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

    $licenseId = db_stmt_insert_id(
        $con,
        'INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `mpin`, `fastBilling`, `takeAway`, `dineIn`, `mess`)
         VALUES (?, ?, ?, ?, \'active\', ?, ?, ?, \'franchise\', ?, ?, ?, ?, ?, ?)',
        'issssssssiiii',
        $customerId,
        $licenseKey,
        $licenseValidity,
        $licenseType,
        $expiryDate,
        $paymentStatus,
        $amount,
        $branchLabel,
        $defaultMpin,
        (int) $fastBilling,
        (int) $takeAway,
        (int) $dineIn,
        (int) $mess
    );

    if ($licenseId !== false && (int) $licenseId > 0) {
        $tpl = business_template_upsert(
            $con,
            (string) $licenseId,
            (int) $customerId,
            (int) $licenseId,
            $businessType,
            $businessTemplateId,
            '',
            'dealer_register'
        );
        $response['status'] = 'true';
        $response['message'] = 'Franchise branch registered. Same customer account — new licence key issued.';
        $response['licenseKey'] = $licenseKey;
        $response['licensesId'] = (string) $licenseId;
        $response['mpin'] = $defaultMpin;
        $response['branchLabel'] = licence_branch_label('franchise', $branchLabel);
        $response['businessType'] = $businessType;
        $response['businessTemplateId'] = $businessTemplateId;
        $response['templateStatus'] = isset($tpl['status']) ? $tpl['status'] : '0';
    } else {
        $response['status'] = 'false';
        $response['message'] = 'licence registration failed...';
    }
}
header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
