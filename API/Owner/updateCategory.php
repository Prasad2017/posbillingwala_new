<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);


$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
 
  $categoryId = $_POST['categoryId'];
  $categoryName = $_POST['categoryName'];
 
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sth="UPDATE `categories` SET `categoryName`='$categoryName' WHERE `categoryId`='$categoryId'";

            if(mysqli_query($con, $sth)){
	
                $response["status"] = '1';
                $response["message"] = "category update successfully";
  
            } else{
    
                $response["status"] = '0';
                $response["message"] = "category failed to update";
 
            }

}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
