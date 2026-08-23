<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $categoryName = $_POST['categoryName'];
    $categoryId = $_POST['categoryId'];
    $productName = $_POST['productName'];
    $productUnit = $_POST['productUnit'];
    $productPrice = $_POST['productPrice'];
    $productCGST = $_POST['productCGST'];
    $productSGST = $_POST['productSGST'];
    $productId = $_POST['productId'];
    $subcategoryId = isset($_POST['subcategoryId']) ? $_POST['subcategoryId'] : '';

    date_default_timezone_set('Asia/Kolkata');
    $date = date('Y-m-d');

    $categoryIdEsc = mysqli_real_escape_string($con, $categoryId);
    $productNameEsc = mysqli_real_escape_string($con, $productName);
    $productPriceEsc = mysqli_real_escape_string($con, $productPrice);
    $productUnitEsc = mysqli_real_escape_string($con, $productUnit);
    $productCGSTEsc = mysqli_real_escape_string($con, $productCGST);
    $productSGSTEsc = mysqli_real_escape_string($con, $productSGST);
    $productIdEsc = mysqli_real_escape_string($con, $productId);
    $subSql = ($subcategoryId != '') ? ", `subcategoryId`='" . mysqli_real_escape_string($con, $subcategoryId) . "'" : "";

    $sql = "UPDATE `products` SET `categoryId`='$categoryIdEsc', `productName`='$productNameEsc', `productPrice`='$productPriceEsc',
            `productUnit`='$productUnitEsc', `productCGST`='$productCGSTEsc', `productSGST`='$productSGSTEsc'$subSql WHERE `productId`='$productIdEsc'";

    if (mysqli_query($con, $sql)) {
        $response['status'] = '1';
        $response['message'] = 'product update successfully';
    } else {
        $response['status'] = '0';
        $response['message'] = 'product failed to update';
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
