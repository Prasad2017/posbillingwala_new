<?php
include_once('config.php');

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    mysqli_query($con, 'set names utf8');

    $userId = $_POST['userId'];
    $categoryId = $_POST['categoryId'];
    $categoryNetworkStatus = isset($_POST['categoryNetworkStatus']) ? $_POST['categoryNetworkStatus'] : '';
    $subcategoryName = $_POST['subcategoryName'];
    $subcategoryDeletedStatus = $_POST['subcategoryDeletedStatus'];
    $subcategoryNetworkStatus = $_POST['subcategoryNetworkStatus'];

    if ($subcategoryDeletedStatus == "1") {
        $subcategoryDeletedStatus = 'deactive';
    } else {
        $subcategoryDeletedStatus = 'active';
    }

    if ($categoryNetworkStatus != '') {
        $sqlCat = "SELECT * FROM `categories` WHERE `userId`='$userId' AND `categoryNetworkStatus`='$categoryNetworkStatus' LIMIT 1";
        $resCat = mysqli_query($con, $sqlCat);
        $checkCat = mysqli_fetch_array($resCat);
        if (isset($checkCat)) {
            $categoryId = $checkCat['categoryId'];
        }
    }

    $sql = "SELECT * FROM `product_subcategories` WHERE `userId`='$userId' AND `subcategoryNetworkStatus`='$subcategoryNetworkStatus'";
    $res = mysqli_query($con, $sql);
    $check = mysqli_fetch_array($res);
    if (isset($check)) {

        $subcategoryId = $check['subcategoryId'];
        $sql = "UPDATE `product_subcategories` SET `categoryId`='$categoryId', `subcategoryName`='$subcategoryName', `subcategoryStatus`='$subcategoryDeletedStatus' WHERE `subcategoryId`='$subcategoryId'";

        if (mysqli_query($con, $sql)) {
            $response["status"] = '1';
            $response["message"] = "update successful!";
        } else {
            $response["status"] = '0';
            $response["message"] = "update failed!";
        }

    } else {

        $sql = "INSERT INTO `product_subcategories`(`userId`, `categoryId`, `subcategoryName`, `subcategoryNetworkStatus`, `subcategoryStatus`)
                VALUES ('$userId', '$categoryId', '$subcategoryName', '$subcategoryNetworkStatus', '$subcategoryDeletedStatus')";

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
