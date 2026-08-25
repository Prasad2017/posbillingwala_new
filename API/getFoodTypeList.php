<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
pos_require_auth($con);

$response["foodTypeResponse"] = array();
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == "GET") {

    $sth = "SELECT * FROM `food_types` WHERE `foodTypeStatus` = 1 ORDER BY `foodTypeSortOrder` ASC, `foodTypeId` ASC";

    if ($result = mysqli_query($con, $sth)) {
        while ($row = mysqli_fetch_assoc($result)) {
            $getdata = array();
            $getdata["foodTypeId"] = $row['foodTypeId'];
            $getdata["foodTypeName"] = $row['foodTypeName'];
            $getdata["foodTypeCode"] = $row['foodTypeCode'];
            $getdata["foodTypeSortOrder"] = $row['foodTypeSortOrder'];
            $getdata["foodTypeStatus"] = $row['foodTypeStatus'];
            array_push($response["foodTypeResponse"], $getdata);
        }
    }

    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
}
?>
