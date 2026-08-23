<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/auth_guard.php';

/**
 * P4-4: Same-key renew/upgrade.
 * Extends an existing licence without rotating licenseKey or clearing device bind.
 */
$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    dealer_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $licensesId = isset($_POST['licensesId']) ? $_POST['licensesId'] : '';
    $licenseValidity = isset($_POST['licenseValidity']) ? $_POST['licenseValidity'] : '';
    $licenseType = isset($_POST['licenseType']) ? $_POST['licenseType'] : '';
    $amount = isset($_POST['amount']) ? $_POST['amount'] : '';
    // registrationDate accepted for backward compatibility but ignored for expiry math

    $response = licence_same_key_upgrade($con, $licensesId, $licenseValidity, $licenseType, $amount);
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
