<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $userId = $_POST['userId'];
    $categoryName = $_POST['categoryName'];
    $categoryId = $_POST['categoryId'];
    $productName = $_POST['productName'];
    $productUnit = $_POST['productUnit'];
    $productPrice = $_POST['productPrice'];
    $productCGST = $_POST['productCGST'];
    $productSGST = $_POST['productSGST'];
    $productNetworkStatus = $_POST['productNetworkStatus'];
    $subcategoryId = isset($_POST['subcategoryId']) ? $_POST['subcategoryId'] : '';

    date_default_timezone_set('Asia/Kolkata');
    $date = date('Y-m-d');

    $userIdEsc = mysqli_real_escape_string($con, $userId);
    $productNetworkStatusEsc = mysqli_real_escape_string($con, $productNetworkStatus);
    $categoryNameEsc = mysqli_real_escape_string($con, $categoryName);
    $productNameEsc = mysqli_real_escape_string($con, $productName);
    $productPriceEsc = mysqli_real_escape_string($con, $productPrice);
    $productUnitEsc = mysqli_real_escape_string($con, $productUnit);
    $productCGSTEsc = mysqli_real_escape_string($con, $productCGST);
    $productSGSTEsc = mysqli_real_escape_string($con, $productSGST);
    $subSql = ($subcategoryId != '') ? ", `subcategoryId`='" . mysqli_real_escape_string($con, $subcategoryId) . "'" : "";
    $subCol = ($subcategoryId != '') ? ", `subcategoryId`" : "";
    $subVal = ($subcategoryId != '') ? ", '" . mysqli_real_escape_string($con, $subcategoryId) . "'" : "";

    $sql = "SELECT * FROM `products` WHERE `userId`='$userIdEsc' AND `productNetworkStatus`='$productNetworkStatusEsc'";
    $res = mysqli_query($con, $sql);
    $check = mysqli_fetch_array($res);
    if (isset($check)) {
        $productId = $check['productId'];
        $sql1 = "SELECT * FROM `categories` WHERE `userId`='$userIdEsc' AND `categoryName`='$categoryNameEsc'";
        $res1 = mysqli_query($con, $sql1);
        $check1 = mysqli_fetch_array($res1);
        if (isset($check1)) {
            $categoryId = $check1['categoryId'];
            $sql = "UPDATE `products` SET `categoryId`='$categoryId', `productName`='$productNameEsc', `productPrice`='$productPriceEsc',
                    `productUnit`='$productUnitEsc', `productCGST`='$productCGSTEsc', `productSGST`='$productSGSTEsc'$subSql WHERE `productId`='$productId'";
            if (mysqli_query($con, $sql)) {
                $response['status'] = '1';
                $response['message'] = 'update successful!';
                $response['productId'] = (string) $productId;
            } else {
                $response['status'] = '0';
                $response['message'] = 'update failed!';
            }
        }
    } else {
        $sql1 = "SELECT * FROM `categories` WHERE `userId`='$userIdEsc' AND `categoryName`='$categoryNameEsc'";
        $res1 = mysqli_query($con, $sql1);
        $check1 = mysqli_fetch_array($res1);
        if (isset($check1)) {
            $categoryId = $check1['categoryId'];
            $sql = "INSERT INTO `products`(`userId`, `categoryId`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`$subCol)
                    VALUES ('$userIdEsc', '$categoryId', '$productNameEsc', '$productPriceEsc', '$productUnitEsc', '$productCGSTEsc', '$productSGSTEsc', '$productNetworkStatusEsc', 'active'$subVal)";
            if (mysqli_query($con, $sql)) {
                $response['status'] = '1';
                $response['message'] = 'insert successful!';
                $response['productId'] = (string) mysqli_insert_id($con);
            } else {
                $response['status'] = '0';
                $response['message'] = 'insert failed!';
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
