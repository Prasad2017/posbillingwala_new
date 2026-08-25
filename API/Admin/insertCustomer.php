<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $name = isset($_POST['name']) ? trim($_POST['name']) : '';
    $contact_number = isset($_POST['contact_number']) ? trim($_POST['contact_number']) : '';
    $address = isset($_POST['address']) ? trim($_POST['address']) : '';
    $shopName = isset($_POST['shopName']) ? trim($_POST['shopName']) : '';
    // Client-supplied key ignored — server generates securely
    $licenseValidity = isset($_POST['licenseValidity']) ? trim($_POST['licenseValidity']) : '';
    $licenseType = isset($_POST['licenseType']) ? trim($_POST['licenseType']) : '';
    $amount = isset($_POST['amount']) ? trim($_POST['amount']) : '0';
    $fastBilling = isset($_POST['fastBilling']) ? trim($_POST['fastBilling']) : '0';
    $takeAway = isset($_POST['takeAway']) ? trim($_POST['takeAway']) : '0';
    $dineIn = isset($_POST['dineIn']) ? trim($_POST['dineIn']) : '0';
    $mess = isset($_POST['mess']) ? trim($_POST['mess']) : '0';

    date_default_timezone_set('Asia/Kolkata');
    $date = date('Y-m-d');

    if ($name === '' || $contact_number === '' || $shopName === '') {
        $response['status'] = 'false';
        $response['message'] = 'Owner name, mobile and restaurant name are required.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
    $licenseKey = licence_generate_unique_key($con);
    if ($licenseKey === null) {
        $response['status'] = 'false';
        $response['message'] = 'Unable to generate license key. Please try again.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $expiryDate = date('Y-m-d', strtotime($date . ' +' . $licenseValidity . ' day'));
    $paymentStatus = ($licenseType === 'Demo' || $licenseType === 'Trial') ? '' : 'cash';
    // Paid direct sale: active but device not bound yet
    $licenseStatus = 'active';

    $okUser = db_stmt_execute(
        $con,
        'INSERT INTO `users`(`role_id`, `name`, `contact_number`, `address`, `is_active`, `shopName`) VALUES (\'3\', ?, ?, ?, \'1\', ?)',
        'ssss',
        $name,
        $contact_number,
        $address,
        $shopName
    );

    if ($okUser) {
        $customerId = mysqli_insert_id($con);
        $okLic = db_stmt_execute(
            $con,
            'INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `fastBilling`, `takeAway`, `dineIn`, `mess`)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, \'owner\', ?, ?, ?, ?, ?)',
            'issssssssiiii',
            $customerId,
            $licenseKey,
            $licenseValidity,
            $licenseType,
            $licenseStatus,
            $expiryDate,
            $paymentStatus,
            $amount,
            $name,
            (int) $fastBilling,
            (int) $takeAway,
            (int) $dineIn,
            (int) $mess
        );

        if ($okLic) {
            $response['status'] = 'true';
            $response['message'] = 'registration successful!';
            $response['licenseKey'] = $licenseKey;
            $response['customerId'] = (string) $customerId;
            $response['expiryDate'] = $expiryDate;
            $response['licenseStatus'] = $licenseStatus;
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
