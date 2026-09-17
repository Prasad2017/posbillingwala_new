<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';

$response = array();
$response['productResponse'] = array();
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $__postedUserId = $userId;
    pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    date_default_timezone_set('Asia/Calcutta');
    $userIdEsc = mysqli_real_escape_string($con, (string)$userId);

    /*
     * Do NOT use SELECT * with JOIN — categories.categoryId / userId would
     * overwrite products.categoryId in mysqli_fetch_assoc and drop the category.
     */
    $sth = "SELECT
                p.`productId`,
                p.`userId`,
                p.`categoryId`,
                c.`categoryName`,
                p.`subcategoryId`,
                p.`productCode`,
                p.`productName`,
                p.`productImage`,
                p.`productPrice`,
                p.`productMrp`,
                p.`productUnit`,
                p.`productCGST`,
                p.`productSGST`,
                p.`openPrice`,
                p.`priceIncludesGst`,
                p.`productStatus`,
                p.`productNetworkStatus`
            FROM `products` p
            LEFT JOIN `categories` c
              ON c.`categoryId` = p.`categoryId`
             AND c.`userId` = p.`userId`
            WHERE p.`userId` = '$userIdEsc'
              AND p.`productStatus` = 'active'
            ORDER BY p.`productId` DESC";

    if ($result = mysqli_query($con, $sth)) {
        while ($row = mysqli_fetch_assoc($result)) {
            $getdata = array();
            $getdata['productId'] = $row['productId'];
            $getdata['userId'] = $row['userId'];
            $getdata['categoryId'] = $row['categoryId'];
            $getdata['categoryName'] = isset($row['categoryName']) && $row['categoryName'] !== null
                ? $row['categoryName']
                : '';
            $getdata['productCode'] = $row['productCode'];
            $getdata['productName'] = $row['productName'];
            $getdata['productImage'] = isset($row['productImage']) ? $row['productImage'] : '';
            $getdata['productPrice'] = $row['productPrice'];
            $getdata['productMrp'] = isset($row['productMrp']) && $row['productMrp'] !== null && $row['productMrp'] !== ''
                ? $row['productMrp']
                : $row['productPrice'];
            $getdata['productUnit'] = $row['productUnit'];
            $getdata['productCGST'] = $row['productCGST'];
            $getdata['productSGST'] = $row['productSGST'];
            $getdata['openPrice'] = isset($row['openPrice']) && $row['openPrice'] !== ''
                ? $row['openPrice']
                : 'off';
            $getdata['priceIncludesGst'] = isset($row['priceIncludesGst']) && $row['priceIncludesGst'] !== ''
                ? $row['priceIncludesGst']
                : '0';
            $getdata['productDeletedStatus'] = ($row['productStatus'] == 'active') ? '0' : '1';
            $getdata['productNetworkStatus'] = $row['productNetworkStatus'];
            if (!empty($row['subcategoryId']) && (string)$row['subcategoryId'] !== '0') {
                $getdata['subcategoryId'] = $row['subcategoryId'];
            }

            array_push($response['productResponse'], $getdata);
        }
        mysqli_free_result($result);
    }

    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
}
?>
