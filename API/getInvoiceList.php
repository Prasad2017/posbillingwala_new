<?php	
include_once('config.php');
require_once __DIR__ . '/branch_scope.php';

$response = array("invoiceResponse" => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == "GET") {

    $postedUserId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $invoiceDate = isset($_GET['invoiceDate']) ? $_GET['invoiceDate'] : '';
    $startDate = isset($_GET['startDate']) ? trim($_GET['startDate']) : '';
    $endDate = isset($_GET['endDate']) ? trim($_GET['endDate']) : '';

    $readCtx = branch_pos_prepare_read($con, $postedUserId, $postedUserId, $response);
    if ($readCtx === null) {
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }
    $userId = $readCtx['targetBranchId'];
    require_once __DIR__ . '/pos_staff.php';
    require_once __DIR__ . '/invoice_sales_filter.php';
    pos_require_permission($con, $userId, 'bill.view');
    $userIdEsc = mysqli_real_escape_string($con, (string) $userId);

    date_default_timezone_set("Asia/Calcutta");

    $scopeWhere = "(`branch_id`='$userIdEsc' OR (`branch_id` IS NULL AND `licenseId`='$userIdEsc'))";
    $staffScope = invoice_and_staff_scope($con, '');

    /* Date range for Web POS reports (startDate/endDate inclusive, Y-m-d). */
    if ($startDate !== '' && preg_match('/^\d{4}-\d{2}-\d{2}$/', $startDate)
        && $endDate !== '' && preg_match('/^\d{4}-\d{2}-\d{2}$/', $endDate)) {
        $startEsc = mysqli_real_escape_string($con, $startDate);
        $endEsc = mysqli_real_escape_string($con, $endDate);
        $sth = "SELECT * FROM `invoice` WHERE $scopeWhere"
             . " AND DATE(`invoiceDate`) >= '$startEsc'"
             . " AND DATE(`invoiceDate`) <= '$endEsc'"
             . $staffScope
             . " ORDER BY `invoiceDate` DESC";
    } elseif ($invoiceDate !== '') {
        $invoiceDateEsc = mysqli_real_escape_string($con, $invoiceDate);
        $sth = "SELECT * FROM `invoice` WHERE $scopeWhere AND `invoiceDate` LIKE '%$invoiceDateEsc%'"
             . $staffScope;
    } else {
        $sth = "SELECT * FROM `invoice` WHERE $scopeWhere" . $staffScope;
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
                $getdata["cashAmount"] = isset($row['cashAmount']) ? $row['cashAmount'] : '0';
                $getdata["upiAmount"] = isset($row['upiAmount']) ? $row['upiAmount'] : '0';
                $getdata["diningSessionId"] = isset($row['diningSessionId']) ? $row['diningSessionId'] : '';
                $getdata["billPrintStatus"] = isset($row['billPrintStatus']) ? $row['billPrintStatus'] : '';
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
