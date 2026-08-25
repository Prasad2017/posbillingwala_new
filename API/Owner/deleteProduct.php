<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);


$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
 
  $productId = $_POST['productId'];
 
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sth="DELETE FROM `products` WHERE `productId`='$productId'";

            if(mysqli_query($con, $sth)){
	
                $response["status"] = '1';
                $response["message"] = "product delete successfully";
  
            } else{
    
                $response["status"] = '0';
                $response["message"] = "product failed to delete";
 
            }

}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
