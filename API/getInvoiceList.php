<?php	
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';

$response = array("invoiceResponse" => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == "GET") {

    $postedUserId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $invoiceDate = isset($_GET['invoiceDate']) ? $_GET['invoiceDate'] : '';

    $readCtx = branch_pos_prepare_read($con, $postedUserId, $postedUserId, $response);
    if ($readCtx === null) {
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }
    $userId = $readCtx['targetBranchId'];
    $userIdEsc = mysqli_real_escape_string($con, (string) $userId);

    date_default_timezone_set("Asia/Calcutta");

    if ($invoiceDate !== '') {
        $invoiceDateEsc = mysqli_real_escape_string($con, $invoiceDate);
        $sth = "SELECT * FROM `invoice` WHERE (`branch_id`='$userIdEsc' OR (`branch_id` IS NULL AND `licenseId`='$userIdEsc')) AND `invoiceDate` LIKE '%$invoiceDateEsc%'";
    } else {
        $sth = "SELECT * FROM `invoice` WHERE `branch_id`='$userIdEsc' OR (`branch_id` IS NULL AND `licenseId`='$userIdEsc')";
    }

    if ($result = mysqli_query($con, $sth)) {
        if (mysqli_num_rows($result) > 0) {
            while ($row = mysqli_fetch_assoc($result)) {
                $getdata = array();
                $getdata["invoiceId"] = $row['invoiceId'];
                $getdata["userId"] = $row['licenseId'];
                $getdata["noOfTable"] = $row['noOfTable'];
                $getdata["invoiceType"] = $row['invoiceType'];
                $getdata["invoiceNumber"] = $row['invoiceNumber'];
                $getdata["customerName"] = $row['customerName'];
                $getdata["customerMobile"] = $row['customerMobile'];
                $getdata["customerEmail"] = isset($row['customerEmail']) ? $row['customerEmail'] : '';
                $getdata["customerAddress"] = $row['customerAddress'];
                $getdata["subTotal"] = $row['subTotal'];
                $getdata["totalGSTAmount"] = $row['totalGSTAmount'];
                $getdata["discount"] = $row['discount'];
                $getdata["discountType"] = $row['discountType'];
                $getdata["packingCharge"] = isset($row['packingCharge']) ? $row['packingCharge'] : '0';
                $getdata["packingChargeType"] = isset($row['packingChargeType']) ? $row['packingChargeType'] : 'Percentage';
                $getdata["totalAmount"] = $row['totalAmount'];
                $getdata["paymentMode"] = $row['paymentMode'];
                $getdata["invoiceDate"] = $row['invoiceDate'];
                $getdata["invoiceOrderStatus"] = $row['invoiceOrderStatus'];
                $getdata["invoiceNetworkStatus"] = $row['invoiceNetworkStatus'];
                branch_append_scope_to_invoice_row($getdata, $row);
                array_push($response["invoiceResponse"], $getdata);
            }
        }
    }

    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
}
?>
