<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    dealer_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $userId = $_POST['userId'];
    $categoryId = $_POST['categoryId'];
    $subcategoryName = $_POST['subcategoryName'];
    $subcategoryNetworkStatus = $_POST['subcategoryNetworkStatus'];

    $userIdEsc = mysqli_real_escape_string($con, $userId);
    $categoryIdEsc = mysqli_real_escape_string($con, $categoryId);
    $subcategoryNameEsc = mysqli_real_escape_string($con, $subcategoryName);
    $subcategoryNetworkStatusEsc = mysqli_real_escape_string($con, $subcategoryNetworkStatus);

    $sql = "SELECT * FROM `product_subcategories` WHERE `userId`='$userIdEsc' AND `subcategoryNetworkStatus`='$subcategoryNetworkStatusEsc'";
    $res = mysqli_query($con, $sql);
    $check = mysqli_fetch_array($res);
    if (isset($check)) {
        $subcategoryId = $check['subcategoryId'];
        $sql = "UPDATE `product_subcategories` SET `categoryId`='$categoryIdEsc', `subcategoryName`='$subcategoryNameEsc', `subcategoryStatus`='active' WHERE `subcategoryId`='$subcategoryId'";
    } else {
        $sql = "INSERT INTO `product_subcategories`(`userId`, `categoryId`, `subcategoryName`, `subcategoryNetworkStatus`, `subcategoryStatus`)
                VALUES ('$userIdEsc', '$categoryIdEsc', '$subcategoryNameEsc', '$subcategoryNetworkStatusEsc', 'active')";
    }

    if (mysqli_query($con, $sql)) {
        $response['status'] = '1';
        $response['message'] = isset($check) ? 'update successful!' : 'insert successful!';
    } else {
        $response['status'] = '0';
        $response['message'] = 'save failed!';
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
