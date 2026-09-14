<?php
/**
 * POS Web / Android sales summary for a date range.
 * Auth: POS Bearer (pos_auth_guard) via userId = licence id.
 */
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/branch_scope.php';
require_once __DIR__ . '/invoice_sales_filter.php';

header('Content-Type: application/json; charset=utf-8');
date_default_timezone_set('Asia/Kolkata');

$response = array(
    'status' => '0',
    'message' => '',
    'startDate' => '',
    'endDate' => '',
    'billCount' => '0',
    'totalSales' => '0',
    'subTotal' => '0',
    'gstTotal' => '0',
    'discountTotal' => '0',
    'cashTotal' => '0',
    'upiTotal' => '0',
    'posCount' => '0',
    'takeawayCount' => '0',
    'tableCount' => '0',
    'avgBill' => '0',
    'invoiceResponse' => array(),
);

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $response['message'] = 'Use GET';
    echo json_encode($response);
    exit;
}

$postedUserId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
$startDate = isset($_GET['startDate']) ? trim($_GET['startDate']) : date('Y-m-d');
$endDate = isset($_GET['endDate']) ? trim($_GET['endDate']) : date('Y-m-d');

if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $startDate)) {
    $startDate = date('Y-m-d');
}
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $endDate)) {
    $endDate = date('Y-m-d');
}

$response['startDate'] = $startDate;
$response['endDate'] = $endDate;

$licenceId = pos_require_auth($con, $postedUserId, $response);
mysqli_query($con, 'set names utf8');

$readCtx = branch_pos_prepare_read($con, $postedUserId, $postedUserId, $response);
if ($readCtx === null) {
    echo json_encode($response);
    exit;
}
$branchId = $readCtx['targetBranchId'];
$branchEsc = mysqli_real_escape_string($con, (string) $branchId);
$startEsc = mysqli_real_escape_string($con, $startDate);
$endEsc = mysqli_real_escape_string($con, $endDate);

$scopeWhere = "(`branch_id`='$branchEsc' OR (`branch_id` IS NULL AND `licenseId`='$branchEsc'))";
$notRefunded = invoice_and_not_refunded('');

$sql = "SELECT * FROM `invoice`"
     . " WHERE $scopeWhere"
     . " AND DATE(`invoiceDate`) >= '$startEsc'"
     . " AND DATE(`invoiceDate`) <= '$endEsc'"
     . $notRefunded
     . " ORDER BY `invoiceDate` DESC";

$billCount = 0;
$totalSales = 0.0;
$subTotal = 0.0;
$gstTotal = 0.0;
$discountTotal = 0.0;
$cashTotal = 0.0;
$upiTotal = 0.0;
$posCount = 0;
$takeawayCount = 0;
$tableCount = 0;

if ($result = mysqli_query($con, $sql)) {
    while ($row = mysqli_fetch_assoc($result)) {
        $getdata = array();
        $getdata['invoiceId'] = $row['invoiceId'];
        $getdata['userId'] = $row['licenseId'];
        $getdata['noOfTable'] = $row['noOfTable'];
        $getdata['invoiceType'] = $row['invoiceType'];
        $getdata['invoiceNumber'] = $row['invoiceNumber'];
        $getdata['customerName'] = $row['customerName'];
        $getdata['customerMobile'] = $row['customerMobile'];
        $getdata['customerEmail'] = isset($row['customerEmail']) ? $row['customerEmail'] : '';
        $getdata['customerAddress'] = $row['customerAddress'];
        $getdata['subTotal'] = $row['subTotal'];
        $getdata['totalGSTAmount'] = $row['totalGSTAmount'];
        $getdata['discount'] = $row['discount'];
        $getdata['discountType'] = $row['discountType'];
        $getdata['packingCharge'] = isset($row['packingCharge']) ? $row['packingCharge'] : '0';
        $getdata['packingChargeType'] = isset($row['packingChargeType']) ? $row['packingChargeType'] : 'Percentage';
        $getdata['totalAmount'] = $row['totalAmount'];
        $getdata['paymentMode'] = $row['paymentMode'];
        $getdata['cashAmount'] = isset($row['cashAmount']) ? $row['cashAmount'] : '0';
        $getdata['upiAmount'] = isset($row['upiAmount']) ? $row['upiAmount'] : '0';
        $getdata['diningSessionId'] = isset($row['diningSessionId']) ? $row['diningSessionId'] : '';
        $getdata['billPrintStatus'] = isset($row['billPrintStatus']) ? $row['billPrintStatus'] : '';
        $getdata['invoiceDate'] = $row['invoiceDate'];
        $getdata['invoiceOrderStatus'] = $row['invoiceOrderStatus'];
        $getdata['invoiceNetworkStatus'] = $row['invoiceNetworkStatus'];
        if (function_exists('branch_append_scope_to_invoice_row')) {
            branch_append_scope_to_invoice_row($getdata, $row);
        }
        array_push($response['invoiceResponse'], $getdata);

        $billCount++;
        $totalSales += floatval($row['totalAmount']);
        $subTotal += floatval($row['subTotal']);
        $gstTotal += floatval($row['totalGSTAmount']);
        $discountTotal += floatval($row['discount']);
        $cashTotal += floatval(isset($row['cashAmount']) ? $row['cashAmount'] : 0);
        $upiTotal += floatval(isset($row['upiAmount']) ? $row['upiAmount'] : 0);

        $type = isset($row['invoiceType']) ? strtolower(trim($row['invoiceType'])) : '';
        if ($type === 'take_away') {
            $takeawayCount++;
        } elseif ($type === 'table_wise') {
            $tableCount++;
        } else {
            $posCount++;
        }
    }
}

$response['status'] = '1';
$response['message'] = 'ok';
$response['billCount'] = (string) $billCount;
$response['totalSales'] = number_format($totalSales, 2, '.', '');
$response['subTotal'] = number_format($subTotal, 2, '.', '');
$response['gstTotal'] = number_format($gstTotal, 2, '.', '');
$response['discountTotal'] = number_format($discountTotal, 2, '.', '');
$response['cashTotal'] = number_format($cashTotal, 2, '.', '');
$response['upiTotal'] = number_format($upiTotal, 2, '.', '');
$response['posCount'] = (string) $posCount;
$response['takeawayCount'] = (string) $takeawayCount;
$response['tableCount'] = (string) $tableCount;
$response['avgBill'] = $billCount > 0
    ? number_format($totalSales / $billCount, 2, '.', '')
    : '0.00';
/* licence id used for auth (unused in body; keeps static analyzers quiet) */
unset($licenceId);

echo json_encode($response);
?>
