<?php
include_once('config.php');

$response["subcategoryResponse"] = array();
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == "GET") {

    $userId = $_GET['userId'];

    $sth = "SELECT * FROM `product_subcategories` WHERE `userId`='$userId' ORDER BY `subcategoryId` ASC";

    if ($result = mysqli_query($con, $sth)) {
        while ($row = mysqli_fetch_assoc($result)) {
            $getdata = array();
            $getdata["subcategoryId"] = $row['subcategoryId'];
            $getdata["categoryId"] = $row['categoryId'];
            $getdata["subcategoryName"] = $row['subcategoryName'];
            $getdata["subcategoryNetworkStatus"] = $row['subcategoryNetworkStatus'];
            if ($row['subcategoryStatus'] == 'active') {
                $getdata["subcategoryDeletedStatus"] = '0';
            } else {
                $getdata["subcategoryDeletedStatus"] = '1';
            }
            array_push($response["subcategoryResponse"], $getdata);
        }
    }

    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
}
?>
