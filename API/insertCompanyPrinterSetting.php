<?php
/**
 * Upsert company printer settings for a licence.
 * Never fatals on missing POST keys or failed SELECT (PHP 8 safe).
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

try {
    mysqli_query($con, 'set names utf8');

    // Ensure optional column without blocking the request if it already exists
    $col = @mysqli_query($con, "SHOW COLUMNS FROM `company_printer_setting` LIKE 'duplicateBillUse'");
    if ($col && mysqli_num_rows($col) === 0) {
        @mysqli_query($con, "ALTER TABLE `company_printer_setting` ADD COLUMN `duplicateBillUse` VARCHAR(10) NULL DEFAULT 'off'");
    }
    if ($col) {
        mysqli_free_result($col);
    }

    $userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
    pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));

    $post = function ($key, $default = '') {
        return isset($_POST[$key]) ? (string) $_POST[$key] : $default;
    };

    $printerName = $post('printerName');
    $KOTPrinterName = $post('KOTPrinterName');
    $invoicePrefix = $post('invoicePrefix');
    $invoiceTitle = $post('invoiceTitle');
    $invoiceTermsCondition = $post('invoiceTermsCondition');
    $logoUse = $post('logoUse', 'off');
    $paymentUse = $post('paymentUse', 'off');
    $customerUse = $post('customerUse', 'off');
    $productQuantityUpdate = $post('productQuantityUpdate', 'off');
    $duplicateBillUse = $post('duplicateBillUse', 'off');
    if ($duplicateBillUse === '') {
        $duplicateBillUse = 'off';
    }
    $bluetoothAddress = $post('bluetoothAddress');
    $bluetoothKOTAddress = $post('bluetoothKOTAddress');
    $printerFeedLines = $post('printerFeedLines', '1');
    $KotPrinterFeedLines = $post('KotPrinterFeedLines', '1');

    $sql = "SELECT `settingId` FROM `company_printer_setting` WHERE `licenseId`='" . mysqli_real_escape_string($con, $userId) . "' LIMIT 1";
    $res = mysqli_query($con, $sql);
    $check = ($res instanceof mysqli_result) ? mysqli_fetch_assoc($res) : null;
    if ($res instanceof mysqli_result) {
        mysqli_free_result($res);
    }

    if ($check !== null && isset($check['settingId'])) {
        $settingId = $check['settingId'];
        $sql = "UPDATE `company_printer_setting` SET
            `printerName`='" . mysqli_real_escape_string($con, $printerName) . "',
            `KOTPrinterName`='" . mysqli_real_escape_string($con, $KOTPrinterName) . "',
            `invoicePrefix`='" . mysqli_real_escape_string($con, $invoicePrefix) . "',
            `invoiceTitle`='" . mysqli_real_escape_string($con, $invoiceTitle) . "',
            `invoiceTermsCondition`='" . mysqli_real_escape_string($con, $invoiceTermsCondition) . "',
            `logoUse`='" . mysqli_real_escape_string($con, $logoUse) . "',
            `paymentUse`='" . mysqli_real_escape_string($con, $paymentUse) . "',
            `customerUse`='" . mysqli_real_escape_string($con, $customerUse) . "',
            `productQuantityUpdate`='" . mysqli_real_escape_string($con, $productQuantityUpdate) . "',
            `duplicateBillUse`='" . mysqli_real_escape_string($con, $duplicateBillUse) . "',
            `bluetoothAddress`='" . mysqli_real_escape_string($con, $bluetoothAddress) . "',
            `bluetoothKOTAddress`='" . mysqli_real_escape_string($con, $bluetoothKOTAddress) . "',
            `printerFeedLines`='" . mysqli_real_escape_string($con, $printerFeedLines) . "',
            `KotPrinterFeedLines`='" . mysqli_real_escape_string($con, $KotPrinterFeedLines) . "'
         WHERE `settingId`='" . mysqli_real_escape_string($con, (string) $settingId) . "'";

        if (mysqli_query($con, $sql)) {
            $response['status'] = '1';
            $response['message'] = 'update successful!';
        } else {
            $response['message'] = 'update failed!';
        }
    } else {
        // Column order matches VALUES (licenseId, KOTPrinterName, printerName, ...)
        $sql = "INSERT INTO `company_printer_setting`(
            `licenseId`, `KOTPrinterName`, `printerName`, `invoicePrefix`, `invoiceTitle`,
            `invoiceTermsCondition`, `logoUse`, `paymentUse`, `customerUse`, `productQuantityUpdate`,
            `duplicateBillUse`, `bluetoothAddress`, `bluetoothKOTAddress`, `printerFeedLines`,
            `KotPrinterFeedLines`, `settingStatus`
         ) VALUES (
            '" . mysqli_real_escape_string($con, $userId) . "',
            '" . mysqli_real_escape_string($con, $KOTPrinterName) . "',
            '" . mysqli_real_escape_string($con, $printerName) . "',
            '" . mysqli_real_escape_string($con, $invoicePrefix) . "',
            '" . mysqli_real_escape_string($con, $invoiceTitle) . "',
            '" . mysqli_real_escape_string($con, $invoiceTermsCondition) . "',
            '" . mysqli_real_escape_string($con, $logoUse) . "',
            '" . mysqli_real_escape_string($con, $paymentUse) . "',
            '" . mysqli_real_escape_string($con, $customerUse) . "',
            '" . mysqli_real_escape_string($con, $productQuantityUpdate) . "',
            '" . mysqli_real_escape_string($con, $duplicateBillUse) . "',
            '" . mysqli_real_escape_string($con, $bluetoothAddress) . "',
            '" . mysqli_real_escape_string($con, $bluetoothKOTAddress) . "',
            '" . mysqli_real_escape_string($con, $printerFeedLines) . "',
            '" . mysqli_real_escape_string($con, $KotPrinterFeedLines) . "',
            'active'
         )";

        if (mysqli_query($con, $sql)) {
            $response['status'] = '1';
            $response['message'] = 'insert successful!';
        } else {
            $response['message'] = 'insert failed!';
        }
    }
} catch (Throwable $e) {
    $response['status'] = '0';
    $response['message'] = 'server error';
}

echo json_encode($response);
?>
