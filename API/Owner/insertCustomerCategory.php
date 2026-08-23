<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
  $categoryName = $_POST['categoryName'];
  $categoryNetworkStatus = $_POST['categoryNetworkStatus'];
  $userId = $_POST['userId'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `categories` WHERE `userId`='$userId' AND `categoryNetworkStatus`='$categoryNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
				    
				    $categoryId = $check['categoryId'];
				    $sql="UPDATE `categories` SET `categoryName`='$categoryName' WHERE `categoryId`='$categoryId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 
                 $sql="INSERT INTO `categories`(`userId`, `categoryName`, `categoryNetworkStatus`, `categoryStatus`) VALUES ('$userId', '$categoryName', '$categoryNetworkStatus', 'active')";

                 if(mysqli_query($con,$sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "insert successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "insert failed!";
 
                     }

                }
}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
