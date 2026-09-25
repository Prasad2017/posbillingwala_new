<?php
/**
 * Upsert company printer settings for a licence.
 * Never fatals on missing POST keys or failed SELECT (PHP 8 safe).
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/pos_schema.php';
require_once __DIR__ . '/dine_in_helpers.php';

header('Content-Type: application/json; charset=utf-8');
$response = array('status' => '0', 'message' => 'Failed');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    exit;
}

try {
    mysqli_query($con, 'set names utf8');
    dine_in_ensure_printer_kot_columns($con);
    pos_schema_ensure($con);

    $userId = isset($_POST['userId']) ? trim((string) $_POST['userId']) : '';
    pos_require_auth($con, $userId, array('status' => '0', 'message' => 'Unauthorized'));
    require_once __DIR__ . '/pos_staff.php';
    pos_require_permission($con, $userId, 'printer.manage');

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
    $kotEnable = $post('kotEnable', 'on');
    if ($kotEnable === '') {
        $kotEnable = 'on';
    }
    $kotPrefix = $post('kotPrefix', 'KOT-');
    if ($kotPrefix === '') {
        $kotPrefix = 'KOT-';
    }
    $kotCopies = $post('kotCopies', '1');
    if ($kotCopies === '') {
        $kotCopies = '1';
    }
    $kotAutoPrint = $post('kotAutoPrint', 'off');
    if ($kotAutoPrint === '') {
        $kotAutoPrint = 'off';
    }
    $kotPreview = $post('kotPreview', 'on');
    if ($kotPreview === '') {
        $kotPreview = 'on';
    }
    $printFastBill = $post('printFastBill', 'off');
    if ($printFastBill === '') {
        $printFastBill = 'off';
    }

    $sql = "SELECT * FROM `company_printer_setting` WHERE `licenseId`='" . mysqli_real_escape_string($con, $userId) . "' LIMIT 1";
    $res = mysqli_query($con, $sql);
    $check = ($res instanceof mysqli_result) ? mysqli_fetch_assoc($res) : null;
    if ($res instanceof mysqli_result) {
        mysqli_free_result($res);
    }

    $existing = is_array($check) ? $check : array();
    $paperSize = pos_normalize_paper_size($post('paperSize', isset($existing['paperSize']) ? $existing['paperSize'] : '2-Inch'));
    $kotPaperFallback = isset($existing['kotPaperSize']) && $existing['kotPaperSize'] !== ''
        ? $existing['kotPaperSize']
        : (isset($existing['paperSize']) ? $existing['paperSize'] : '2-Inch');
    $kotPaperSize = pos_normalize_paper_size($post('kotPaperSize', $kotPaperFallback));
    $billConnectionType = pos_normalize_connection_type(
        $post('billConnectionType', isset($existing['billConnectionType']) ? $existing['billConnectionType'] : 'BLUETOOTH'),
        isset($existing['billConnectionType']) ? $existing['billConnectionType'] : 'BLUETOOTH'
    );
    $kotConnectionType = pos_normalize_connection_type(
        $post('kotConnectionType', isset($existing['kotConnectionType']) ? $existing['kotConnectionType'] : 'BLUETOOTH'),
        isset($existing['kotConnectionType']) ? $existing['kotConnectionType'] : 'BLUETOOTH'
    );
    $billUsbIdentifier = $post('billUsbIdentifier', isset($existing['billUsbIdentifier']) ? $existing['billUsbIdentifier'] : '');
    $billUsbName = $post('billUsbName', isset($existing['billUsbName']) ? $existing['billUsbName'] : '');
    $kotUsbIdentifier = $post('kotUsbIdentifier', isset($existing['kotUsbIdentifier']) ? $existing['kotUsbIdentifier'] : '');
    $kotUsbName = $post('kotUsbName', isset($existing['kotUsbName']) ? $existing['kotUsbName'] : '');

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
            `KotPrinterFeedLines`='" . mysqli_real_escape_string($con, $KotPrinterFeedLines) . "',
            `kotEnable`='" . mysqli_real_escape_string($con, $kotEnable) . "',
            `kotPrefix`='" . mysqli_real_escape_string($con, $kotPrefix) . "',
            `kotCopies`='" . mysqli_real_escape_string($con, $kotCopies) . "',
            `kotAutoPrint`='" . mysqli_real_escape_string($con, $kotAutoPrint) . "',
            `kotPreview`='" . mysqli_real_escape_string($con, $kotPreview) . "',
            `printFastBill`='" . mysqli_real_escape_string($con, $printFastBill) . "',
            `paperSize`='" . mysqli_real_escape_string($con, $paperSize) . "',
            `kotPaperSize`='" . mysqli_real_escape_string($con, $kotPaperSize) . "',
            `billConnectionType`='" . mysqli_real_escape_string($con, $billConnectionType) . "',
            `kotConnectionType`='" . mysqli_real_escape_string($con, $kotConnectionType) . "',
            `billUsbIdentifier`='" . mysqli_real_escape_string($con, $billUsbIdentifier) . "',
            `billUsbName`='" . mysqli_real_escape_string($con, $billUsbName) . "',
            `kotUsbIdentifier`='" . mysqli_real_escape_string($con, $kotUsbIdentifier) . "',
            `kotUsbName`='" . mysqli_real_escape_string($con, $kotUsbName) . "'
         WHERE `settingId`='" . mysqli_real_escape_string($con, (string) $settingId) . "'";

        if (mysqli_query($con, $sql)) {
            $response['status'] = '1';
            $response['message'] = 'update successful!';
        } else {
            $response['message'] = 'update failed!';
        }
    } else {
        $sql = "INSERT INTO `company_printer_setting`(
            `licenseId`, `KOTPrinterName`, `printerName`, `invoicePrefix`, `invoiceTitle`,
            `invoiceTermsCondition`, `logoUse`, `paymentUse`, `customerUse`, `productQuantityUpdate`,
            `duplicateBillUse`, `bluetoothAddress`, `bluetoothKOTAddress`, `printerFeedLines`,
            `KotPrinterFeedLines`, `kotEnable`, `kotPrefix`, `kotCopies`, `kotAutoPrint`, `kotPreview`,
            `printFastBill`,
            `paperSize`, `kotPaperSize`, `billConnectionType`, `kotConnectionType`,
            `billUsbIdentifier`, `billUsbName`, `kotUsbIdentifier`, `kotUsbName`,
            `settingStatus`
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
            '" . mysqli_real_escape_string($con, $kotEnable) . "',
            '" . mysqli_real_escape_string($con, $kotPrefix) . "',
            '" . mysqli_real_escape_string($con, $kotCopies) . "',
            '" . mysqli_real_escape_string($con, $kotAutoPrint) . "',
            '" . mysqli_real_escape_string($con, $kotPreview) . "',
            '" . mysqli_real_escape_string($con, $printFastBill) . "',
            '" . mysqli_real_escape_string($con, $paperSize) . "',
            '" . mysqli_real_escape_string($con, $kotPaperSize) . "',
            '" . mysqli_real_escape_string($con, $billConnectionType) . "',
            '" . mysqli_real_escape_string($con, $kotConnectionType) . "',
            '" . mysqli_real_escape_string($con, $billUsbIdentifier) . "',
            '" . mysqli_real_escape_string($con, $billUsbName) . "',
            '" . mysqli_real_escape_string($con, $kotUsbIdentifier) . "',
            '" . mysqli_real_escape_string($con, $kotUsbName) . "',
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
