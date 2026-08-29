<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);


$response = array('subcategoryResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';
$userId = owner_resolve_user_id($con, $userId);
if ($userId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status'=>'0','message'=>'Unauthorized'));
    mysqli_close($con);
    exit;
}

    $categoryId = isset($_GET['categoryId']) ? $_GET['categoryId'] : '';

    if ($userId !== '') {
        $userIdEsc = mysqli_real_escape_string($con, $userId);
        $sth = "SELECT * FROM `product_subcategories` WHERE `userId`='$userIdEsc'
                AND (`subcategoryStatus`='active' OR `subcategoryStatus` IS NULL OR `subcategoryStatus`='')";
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
