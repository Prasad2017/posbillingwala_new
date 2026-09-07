<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../user_identity.php';
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/../business_template_ops.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    dealer_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $name = isset($_POST['name']) ? $_POST['name'] : '';
    $userType = isset($_POST['userType']) ? $_POST['userType'] : '';
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
    $dealerId = isset($_POST['userId']) ? $_POST['userId'] : '';
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

    if ($userType == 'Dealer') {
        $roleId = '2';
    } else {
        $roleId = '3';
    }

    $contact_number = licence_normalize_contact($contact_number);
    if (strlen($contact_number) < 10) {
        $response['status'] = 'false';
        $response['message'] = 'Please enter a valid 10-digit mobile number.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if ($roleId === '3' && user_customer_mobile_taken($con, $contact_number)) {
        $response['status'] = 'false';
        $response['message'] = 'This mobile number is already registered for another customer.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if ($roleId === '2' && user_dealer_mobile_taken($con, $contact_number)) {
        $response['status'] = 'false';
        $response['message'] = 'This mobile number is already registered for another dealer.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $okUser = db_stmt_execute(
        $con,
        'INSERT INTO `users`(`name`, `contact_number`, `address`, `is_active`, `shopName`, `dealerId`, `role_id`)
         VALUES (?, ?, ?, \'1\', ?, ?, ?)',
        'ssssis',
        $name,
        $contact_number,
        $address,
        $shopName,
        (int) $dealerId,
        $roleId
    );

    if ($okUser) {
        $customerId = mysqli_insert_id($con);
        $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
        $expiryDate = date('Y-m-d', strtotime($date . ' +' . $licenseValidity . ' day'));
        $defaultMpin = licence_default_mpin();
        $defaultReportPin = licence_default_report_pin();

        $licenseId = db_stmt_insert_id(
            $con,
            'INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `mpin`, `fastBilling`, `takeAway`, `dineIn`, `mess`)
             VALUES (?, ?, ?, ?, \'active\', ?, ?, ?, \'owner\', ?, ?, ?, ?, ?, ?)',
            'issssssssiiii',
            $customerId,
            $licenseKey,
            $licenseValidity,
            $licenseType,
            $expiryDate,
            $paymentStatus,
            $amount,
            $name,
            $defaultMpin,
            (int) $fastBilling,
            (int) $takeAway,
            (int) $dineIn,
            (int) $mess
        );

        if ($licenseId !== false && (int) $licenseId > 0) {
            // Template only for customer (role 3) owner licences — not dealer-user rows
            if ($roleId === '3') {
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
                $response['templateStatus'] = isset($tpl['status']) ? $tpl['status'] : '0';
                $response['businessType'] = $businessType;
                $response['businessTemplateId'] = $businessTemplateId;
            }
            $response['status'] = 'true';
            $response['message'] = 'registration successful!';
            $response['licenseKey'] = $licenseKey;
            $response['licensesId'] = (string) $licenseId;
            $response['mpin'] = $defaultMpin;
            $response['reportPin'] = $defaultReportPin;
        } else {
            $response['status'] = 'false';
            $response['message'] = 'registration failed...';
        }
    } else {
        $response['status'] = 'false';
        $response['message'] = 'registration failed!';
    }
}
header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
