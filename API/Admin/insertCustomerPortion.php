<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $userId = $_POST['userId'];
    $productId = isset($_POST['productId']) ? $_POST['productId'] : '';
    $portionName = $_POST['portionName'];
    $portionPrice = $_POST['portionPrice'];
    $portionSortOrder = isset($_POST['portionSortOrder']) ? $_POST['portionSortOrder'] : '0';
    $portionNetworkStatus = $_POST['portionNetworkStatus'];

    if ($productId == '') {
        $response['status'] = '0';
        $response['message'] = 'product not found!';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        mysqli_close($con);
        exit;
    }

    $userIdEsc = mysqli_real_escape_string($con, $userId);
    $productIdEsc = mysqli_real_escape_string($con, $productId);
    $portionNameEsc = mysqli_real_escape_string($con, $portionName);
    $portionPriceEsc = mysqli_real_escape_string($con, $portionPrice);
    $portionSortOrderEsc = mysqli_real_escape_string($con, $portionSortOrder);
    $portionNetworkStatusEsc = mysqli_real_escape_string($con, $portionNetworkStatus);

    $sql = "SELECT * FROM `product_portions` WHERE `portionNetworkStatus`='$portionNetworkStatusEsc'";
    $res = mysqli_query($con, $sql);
    $check = mysqli_fetch_array($res);
    if (isset($check)) {
        $portionId = $check['portionId'];
        $sql = "UPDATE `product_portions` SET `productId`='$productIdEsc', `portionName`='$portionNameEsc', `portionPrice`='$portionPriceEsc',
                `portionSortOrder`='$portionSortOrderEsc', `portionStatus`='active' WHERE `portionId`='$portionId'";
    } else {
        $sql = "INSERT INTO `product_portions`(`userId`, `productId`, `portionName`, `portionPrice`, `portionSortOrder`, `portionNetworkStatus`, `portionStatus`)
                VALUES ('$userIdEsc', '$productIdEsc', '$portionNameEsc', '$portionPriceEsc', '$portionSortOrderEsc', '$portionNetworkStatusEsc', 'active')";
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
