<?php
include_once('config.php');
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../branch_scope.php';

$response = array('invoiceResponse' => array(), 'status' => '0');
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    $licenseId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $saleDate = isset($_GET['saleDate']) ? $_GET['saleDate'] : '';

    $ownerUserId = auth_user_id_from_request($con, '', 'owner');
    if ($ownerUserId === null || $ownerUserId === '') {
        $branchScope = branch_scope_from_license($con, $licenseId);
        $ownerUserId = $branchScope !== null ? $branchScope['organizationId'] : '';
    }

    if ($licenseId === '' || !branch_owner_require_branch_access($con, $ownerUserId, $licenseId, $response)) {
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $licenseIdEsc = mysqli_real_escape_string($con, (string) $licenseId);

    date_default_timezone_set('Asia/Kolkata');
    $date = date('Y-m-d');
    $dateEsc = mysqli_real_escape_string($con, $date);

    if ($saleDate === 'totalSale') {
        $sql = "SELECT * FROM `invoice`
                WHERE (`branch_id`='$licenseIdEsc' OR (`branch_id` IS NULL AND `licenseId`='$licenseIdEsc'))
                ORDER BY `invoiceId` DESC";
    } else {
        $sql = "SELECT * FROM `invoice`
                WHERE `invoiceDate` LIKE '%$dateEsc%'
                  AND (`branch_id`='$licenseIdEsc' OR (`branch_id` IS NULL AND `licenseId`='$licenseIdEsc'))
                ORDER BY `invoiceId` DESC";
    }

    $result = mysqli_query($con, $sql);
    if ($result && mysqli_num_rows($result) > 0) {
        while ($row = mysqli_fetch_assoc($result)) {
            $item = array(
                'invoiceId' => $row['invoiceId'],
                'userId' => $row['licenseId'],
                'noOfTable' => $row['noOfTable'],
                'invoiceType' => $row['invoiceType'],
                'invoiceNumber' => $row['invoiceNumber'],
                'customerName' => $row['customerName'],
                'customerMobile' => $row['customerMobile'],
                'customerEmail' => isset($row['customerEmail']) ? $row['customerEmail'] : '',
                'customerAddress' => $row['customerAddress'],
                'subTotal' => $row['subTotal'],
                'totalGSTAmount' => $row['totalGSTAmount'],
                'discount' => $row['discount'],
                'discountType' => $row['discountType'],
                'totalAmount' => $row['totalAmount'],
                'paymentMode' => $row['paymentMode'],
                'invoiceDate' => $row['invoiceDate'],
                'invoiceOrderStatus' => $row['invoiceOrderStatus'],
                'invoiceNetworkStatus' => $row['invoiceNetworkStatus'],
            );
            branch_append_scope_to_invoice_row($item, $row);
            $response['invoiceResponse'][] = $item;
        }
    }
    $response['status'] = '1';
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
