<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';


$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    mysqli_query($con, 'set names utf8');

    $userId = $_POST['userId'];
  $__postedUserId = isset($_POST['userId']) ? $_POST['userId'] : (isset($userId) ? $userId : '');
  pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status'=>'0','message'=>'Unauthorized'));

    $categoryId = $_POST['categoryId'];
    $categoryNetworkStatus = isset($_POST['categoryNetworkStatus']) ? $_POST['categoryNetworkStatus'] : '';
    $subcategoryName = $_POST['subcategoryName'];
    $subcategoryDeletedStatus = $_POST['subcategoryDeletedStatus'];
    $subcategoryNetworkStatus = $_POST['subcategoryNetworkStatus'];
    $subcategorySortOrder = isset($_POST['subcategorySortOrder']) ? intval($_POST['subcategorySortOrder']) : -1;

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
        $sortSql = ($subcategorySortOrder >= 0) ? ", `subcategorySortOrder`='$subcategorySortOrder'" : "";
        $sql = "UPDATE `product_subcategories` SET `categoryId`='$categoryId', `subcategoryName`='$subcategoryName', `subcategoryStatus`='$subcategoryDeletedStatus'$sortSql WHERE `subcategoryId`='$subcategoryId'";

        if (mysqli_query($con, $sql)) {
            $response["status"] = '1';
            $response["message"] = "update successful!";
        } else {
            $response["status"] = '0';
            $response["message"] = "update failed!";
        }

    } else {

        $nextSort = $subcategorySortOrder;
        if ($nextSort < 0) {
            $sortRes = mysqli_query($con, "SELECT IFNULL(MAX(`subcategorySortOrder`), 0) + 1 AS nextSort FROM `product_subcategories` WHERE `userId`='$userId' AND `categoryId`='$categoryId'");
            $sortRow = mysqli_fetch_array($sortRes);
            $nextSort = isset($sortRow['nextSort']) ? intval($sortRow['nextSort']) : 1;
        }
        $sql = "INSERT INTO `product_subcategories`(`userId`, `categoryId`, `subcategoryName`, `subcategoryNetworkStatus`, `subcategoryStatus`, `subcategorySortOrder`)
                VALUES ('$userId', '$categoryId', '$subcategoryName', '$subcategoryNetworkStatus', '$subcategoryDeletedStatus', '$nextSort')";

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
