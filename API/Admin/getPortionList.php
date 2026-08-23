<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array('portionResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    admin_require_auth($con, array('portionResponse' => array()));

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $productId = isset($_GET['productId']) ? $_GET['productId'] : '';

    if ($userId !== '') {
        $userIdEsc = mysqli_real_escape_string($con, $userId);
        $sth = "SELECT pp.*, p.productNetworkStatus FROM `product_portions` pp
                INNER JOIN `products` p ON p.productId = pp.productId
                WHERE p.userId='$userIdEsc'";
        if ($productId !== '') {
            $productIdEsc = mysqli_real_escape_string($con, $productId);
            $sth .= " AND pp.productId='$productIdEsc'";
        }
        $sth .= " ORDER BY pp.portionSortOrder ASC, pp.portionId ASC";

        if ($result = mysqli_query($con, $sth)) {
            while ($row = mysqli_fetch_assoc($result)) {
                $getdata = array();
                $getdata['portionId'] = $row['portionId'];
                $getdata['productId'] = $row['productId'];
                $getdata['productNetworkStatus'] = $row['productNetworkStatus'];
                $getdata['portionName'] = $row['portionName'];
                $getdata['portionPrice'] = $row['portionPrice'];
                $getdata['portionSortOrder'] = $row['portionSortOrder'];
                $getdata['portionNetworkStatus'] = $row['portionNetworkStatus'];
                $getdata['portionDeletedStatus'] = ($row['portionStatus'] == 'active') ? '0' : '1';
                array_push($response['portionResponse'], $getdata);
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
