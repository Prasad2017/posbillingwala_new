<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);


$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
 
  $customerId = $_POST['userId'];
$customerId = owner_resolve_user_id($con, $customerId);
if ($customerId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status'=>'0','message'=>'Unauthorized'));
    mysqli_close($con);
    exit;
}

  $customerName = $_POST['customerName'];
  $customerMobileNumber = $_POST['customerMobileNumber'];
  $customerAddress = $_POST['customerAddress'];
  $customerShopName = $_POST['customerShopName'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sth="UPDATE `users` SET `name`='$customerName', `contact_number`='$customerMobileNumber', `address`='$customerAddress', `shopName`='$customerShopName' WHERE `id`='$customerId'";
	
            if(mysqli_query($con, $sth)){
	
                $response["status"] = '1';
                $response["message"] = "update successful!";
  
            } else{
    
                $response["status"] = '0';
                $response["message"] = "update failed...";
 
            }

}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
