<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
     mysqli_query($con, 'set names utf8');
    
  $customerId = $_POST['customerId'];
  $categoryName = $_POST['categoryName'];
  $productCode = $_POST['productCode'];
  $productName = $_POST['productName'];
  $productUnit = $_POST['productUnit'];
  $productPrice = $_POST['productPrice'];
  $productCGST = $_POST['productCGST'];
  $productSGST = $_POST['productSGST'];
  
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    // String of all alphanumeric character
    $str_result = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    $categoryNetworkStatus = substr(str_shuffle($str_result), 0, 10);
    $productNetworkStatus = substr(str_shuffle($str_result), 0, 10);
    
    
    
    $sql="SELECT * FROM `products` WHERE `userId`='$customerId' AND `productName`='$productName'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
				    
				    $productId = $check['productId'];
				    
				    $sql1="SELECT * FROM `categories` WHERE `userId`='$customerId' AND `categoryName`='$categoryName'";
		            $res1 = mysqli_query($con, $sql1);
			        $check1 = mysqli_fetch_array($res1);
				    if(isset($check1))
				    {
				    
				       $categoryId = $check1['categoryId'];
				    
				       $sql="UPDATE `products` SET `categoryId`='$categoryId', `productCode`='$productCode', `productName`='$productName', `productPrice`='$productPrice', `productUnit`='$productUnit', 
				          `productCGST`='$productCGST', `productSGST`='$productSGST' WHERE `productId`='$productId'";
				    
				    }
				    
				    

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {
				    
				    
				    $sql1="SELECT * FROM `categories` WHERE `userId`='$customerId' AND `categoryName`='$categoryName'";
		            $res1 = mysqli_query($con, $sql1);
			        $check1 = mysqli_fetch_array($res1);
				    if(isset($check1))
				    {
				    
				       $categoryId = $check1['categoryId'];
				       $productNetworkStatus = substr(str_shuffle($str_result), 0, 10);

                 $sql="INSERT INTO `products`(`userId`, `categoryId`, `productCode`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`) 
                       VALUES ('$customerId', '$categoryId', '$productCode', '$productName', '$productPrice', '$productUnit', '$productCGST', '$productSGST', '$productNetworkStatus', 'active')";

                 if(mysqli_query($con,$sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "insert successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "insert failed!";
 
                     }
                     
				    } else {
				        
				        
				        $sql="INSERT INTO `categories`(`userId`, `categoryName`, `categoryNetworkStatus`, `categoryStatus`) VALUES ('$customerId', '$categoryName', '$categoryNetworkStatus', 'active')";

                 if(mysqli_query($con,$sql)){
                     
                     $categoryId = mysqli_insert_id();
	
                       $sql="INSERT INTO `products`(`userId`, `categoryId`, `productCode`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`) 
                       VALUES ('$customerId', '$categoryId', '$productCode', '$productName', '$productPrice', '$productUnit', '$productCGST', '$productSGST', '$productNetworkStatus', 'active')";

                 if(mysqli_query($con,$sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "insert product successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "insert product failed!";
 
                     }
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "insert category failed!";
 
                     }
				        
				    }

                }
    
    
}

header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
