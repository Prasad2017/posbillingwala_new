<?php
include_once('config.php');

$response = array('subcategoryResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $categoryId = isset($_GET['categoryId']) ? $_GET['categoryId'] : '';

    if ($userId !== '') {
        $userIdEsc = mysqli_real_escape_string($con, $userId);
        $sth = "SELECT * FROM `product_subcategories` WHERE `userId`='$userIdEsc'";
        if ($categoryId !== '') {
            $categoryIdEsc = mysqli_real_escape_string($con, $categoryId);
            $sth .= " AND `categoryId`='$categoryIdEsc'";
        }
        $sth .= " ORDER BY `subcategoryId` ASC";

        if ($result = mysqli_query($con, $sth)) {
            while ($row = mysqli_fetch_assoc($result)) {
                $getdata = array();
                $getdata['subcategoryId'] = $row['subcategoryId'];
                $getdata['categoryId'] = $row['categoryId'];
                $getdata['subcategoryName'] = $row['subcategoryName'];
                $getdata['subcategoryNetworkStatus'] = $row['subcategoryNetworkStatus'];
                $getdata['subcategoryDeletedStatus'] = ($row['subcategoryStatus'] == 'active') ? '0' : '1';
                array_push($response['subcategoryResponse'], $getdata);
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
mysqli_close($con);
?>
