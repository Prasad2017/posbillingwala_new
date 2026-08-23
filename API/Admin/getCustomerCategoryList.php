<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array('categoryResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    admin_require_auth($con, array('categoryResponse' => array()));

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';

    if ($userId !== '') {
        $userIdEsc = mysqli_real_escape_string($con, $userId);
        $sth = "SELECT c.*, ft.foodTypeName, ft.foodTypeCode
                FROM `categories` c
                LEFT JOIN `food_types` ft ON ft.foodTypeId = c.foodTypeId
                WHERE c.`userId`='$userIdEsc'";

        if ($result = mysqli_query($con, $sth)) {
            while ($row = mysqli_fetch_assoc($result)) {
                $getdata = array();
                $getdata['categoryId'] = $row['categoryId'];
                $getdata['categoryName'] = $row['categoryName'];
                $getdata['categoryStatus'] = $row['categoryStatus'];
                $getdata['categoryNetworkStatus'] = $row['categoryNetworkStatus'];
                $getdata['foodTypeId'] = isset($row['foodTypeId']) ? $row['foodTypeId'] : '';
                $getdata['foodTypeName'] = isset($row['foodTypeName']) ? $row['foodTypeName'] : '';
                $getdata['foodTypeCode'] = isset($row['foodTypeCode']) ? $row['foodTypeCode'] : '';
                array_push($response['categoryResponse'], $getdata);
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
