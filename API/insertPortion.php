<?php
include_once('config.php');

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    mysqli_query($con, 'set names utf8');

    $userId = $_POST['userId'];
    $productId = isset($_POST['productId']) ? $_POST['productId'] : '';
    $productNetworkStatus = isset($_POST['productNetworkStatus']) ? $_POST['productNetworkStatus'] : '';
    $portionName = $_POST['portionName'];
    $portionPrice = $_POST['portionPrice'];
    $portionSortOrder = isset($_POST['portionSortOrder']) ? $_POST['portionSortOrder'] : '0';
    $portionDeletedStatus = $_POST['portionDeletedStatus'];
    $portionNetworkStatus = $_POST['portionNetworkStatus'];

    if ($portionDeletedStatus == "1") {
        $portionDeletedStatus = 'deactive';
    } else {
        $portionDeletedStatus = 'active';
    }

    if ($productNetworkStatus != '') {
        $sqlProd = "SELECT * FROM `products` WHERE `userId`='$userId' AND `productNetworkStatus`='$productNetworkStatus' LIMIT 1";
        $resProd = mysqli_query($con, $sqlProd);
        $checkProd = mysqli_fetch_array($resProd);
        if (isset($checkProd)) {
            $productId = $checkProd['productId'];
        }
    }

    if ($productId == '') {
        $response["status"] = '0';
        $response["message"] = "product not found!";
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $sql = "SELECT * FROM `product_portions` WHERE `portionNetworkStatus`='$portionNetworkStatus'";
    $res = mysqli_query($con, $sql);
    $check = mysqli_fetch_array($res);
    if (isset($check)) {

        $portionId = $check['portionId'];
        $sql = "UPDATE `product_portions` SET `productId`='$productId', `portionName`='$portionName', `portionPrice`='$portionPrice',
                `portionSortOrder`='$portionSortOrder', `portionStatus`='$portionDeletedStatus' WHERE `portionId`='$portionId'";

        if (mysqli_query($con, $sql)) {
            $response["status"] = '1';
            $response["message"] = "update successful!";
        } else {
            $response["status"] = '0';
            $response["message"] = "update failed!";
        }

    } else {

        $sql = "INSERT INTO `product_portions`(`userId`, `productId`, `portionName`, `portionPrice`, `portionSortOrder`, `portionNetworkStatus`, `portionStatus`)
                VALUES ('$userId', '$productId', '$portionName', '$portionPrice', '$portionSortOrder', '$portionNetworkStatus', '$portionDeletedStatus')";

        if (mysqli_query($con, $sql)) {
            $response["status"] = '1';
            $response["message"] = "insert successful!";
        } else {
            $response["status"] = '0';
            $response["message"] = "insert failed!";
        }
    }
}
header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
