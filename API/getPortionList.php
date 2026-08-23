<?php
include_once('config.php');

$response["portionResponse"] = array();
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == "GET") {

    $userId = $_GET['userId'];

    $sth = "SELECT pp.*, p.productNetworkStatus FROM `product_portions` pp
            INNER JOIN `products` p ON p.productId = pp.productId
            WHERE p.userId='$userId' ORDER BY pp.portionId ASC";

    if ($result = mysqli_query($con, $sth)) {
        while ($row = mysqli_fetch_assoc($result)) {
            $getdata = array();
            $getdata["portionId"] = $row['portionId'];
            $getdata["productId"] = $row['productId'];
            $getdata["productNetworkStatus"] = $row['productNetworkStatus'];
            $getdata["portionName"] = $row['portionName'];
            $getdata["portionPrice"] = $row['portionPrice'];
            $getdata["portionSortOrder"] = $row['portionSortOrder'];
            $getdata["portionNetworkStatus"] = $row['portionNetworkStatus'];
            if ($row['portionStatus'] == 'active') {
                $getdata["portionDeletedStatus"] = '0';
            } else {
                $getdata["portionDeletedStatus"] = '1';
            }
            array_push($response["portionResponse"], $getdata);
        }
    }

    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
}
?>
