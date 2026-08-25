<?php	
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';

$i=0;
   
    $response["inventoryResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        $__postedUserId = isset($_GET['userId']) ? $_GET['userId'] : (isset($userId) ? $userId : '');
        pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status'=>'0','message'=>'Unauthorized'));

        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `inventory` WHERE `userId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["inventoryId"]=$row['inventoryId'];
        $getdata["productId"]=$row['productId'];
        $getdata["productInventoryQuantity"]=$row['productInventoryQuantity'];
        $getdata["afterSaleInventoryQuantity"]=$row['afterSaleInventoryQuantity'];
        $getdata["saleInventoryQuantity"]=$row['saleInventoryQuantity'];
        $getdta["inventoryDate"]=$row['inventoryDate'];
        $getdta["inventoryNetworkStatus"]=$row['inventoryNetworkStatus'];
        $getdta["inventoryStatus"]=$row['inventoryStatus'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["inventoryResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>