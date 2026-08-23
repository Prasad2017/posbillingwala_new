<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array('productResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    admin_require_auth($con, array('productResponse' => array()));

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';

    if ($userId !== '') {
        $userIdEsc = mysqli_real_escape_string($con, $userId);
        $sth = "SELECT p.*, c.categoryName, ps.subcategoryName
                FROM `products` p
                LEFT JOIN `categories` c ON c.categoryId = p.categoryId
                LEFT JOIN `product_subcategories` ps ON ps.subcategoryId = p.subcategoryId
                WHERE p.`userId`='$userIdEsc'";

        if ($result = mysqli_query($con, $sth)) {
            while ($row = mysqli_fetch_assoc($result)) {
                $getdata = array();
                $getdata['productId'] = $row['productId'];
                $getdata['userId'] = $row['userId'];
                $getdata['categoryId'] = $row['categoryId'];
                $getdata['categoryName'] = $row['categoryName'];
                $getdata['subcategoryId'] = isset($row['subcategoryId']) ? $row['subcategoryId'] : '';
                $getdata['subcategoryName'] = isset($row['subcategoryName']) ? $row['subcategoryName'] : '';
                $getdata['productName'] = $row['productName'];
                $getdata['productPrice'] = $row['productPrice'];
                $getdata['productUnit'] = $row['productUnit'];
                $getdata['productCGST'] = $row['productCGST'];
                $getdata['productSGST'] = $row['productSGST'];
                $getdata['productStatus'] = $row['productStatus'];
                $getdata['productNetworkStatus'] = $row['productNetworkStatus'];
                array_push($response['productResponse'], $getdata);
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
