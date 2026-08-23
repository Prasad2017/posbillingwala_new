<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $categoryName = $_POST['categoryName'];
    $categoryNetworkStatus = $_POST['categoryNetworkStatus'];
    $userId = $_POST['userId'];
    $foodTypeId = isset($_POST['foodTypeId']) ? $_POST['foodTypeId'] : '';
    $foodTypeCode = isset($_POST['foodTypeCode']) ? $_POST['foodTypeCode'] : '';

    if ($foodTypeId == '' && $foodTypeCode != '') {
        $foodTypeCodeEsc = mysqli_real_escape_string($con, $foodTypeCode);
        $ftRes = mysqli_query($con, "SELECT foodTypeId FROM `food_types` WHERE `foodTypeCode`='$foodTypeCodeEsc' LIMIT 1");
        $ftRow = mysqli_fetch_array($ftRes);
        if (isset($ftRow)) {
            $foodTypeId = $ftRow['foodTypeId'];
        }
    }

    date_default_timezone_set('Asia/Kolkata');
    $date = date('Y-m-d');

    $userIdEsc = mysqli_real_escape_string($con, $userId);
    $categoryNetworkStatusEsc = mysqli_real_escape_string($con, $categoryNetworkStatus);
    $categoryNameEsc = mysqli_real_escape_string($con, $categoryName);

    $sql = "SELECT * FROM `categories` WHERE `userId`='$userIdEsc' AND `categoryNetworkStatus`='$categoryNetworkStatusEsc'";
    $res = mysqli_query($con, $sql);
    $check = mysqli_fetch_array($res);
    if (isset($check)) {
        $categoryId = $check['categoryId'];
        $foodTypeSql = ($foodTypeId != '') ? ", `foodTypeId`='" . mysqli_real_escape_string($con, $foodTypeId) . "'" : "";
        $sql = "UPDATE `categories` SET `categoryName`='$categoryNameEsc'$foodTypeSql WHERE `categoryId`='$categoryId'";
    } else {
        $foodTypeCol = ($foodTypeId != '') ? ", `foodTypeId`" : "";
        $foodTypeVal = ($foodTypeId != '') ? ", '" . mysqli_real_escape_string($con, $foodTypeId) . "'" : "";
        $sql = "INSERT INTO `categories`(`userId`, `categoryName`, `categoryNetworkStatus`, `categoryStatus`$foodTypeCol)
                VALUES ('$userIdEsc', '$categoryNameEsc', '$categoryNetworkStatusEsc', 'active'$foodTypeVal)";
    }

    if (mysqli_query($con, $sql)) {
        $response['status'] = '1';
        $response['message'] = isset($check) ? 'update successful!' : 'insert successful!';
    } else {
        $response['status'] = '0';
        $response['message'] = isset($check) ? 'update failed!' : 'insert failed!';
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
