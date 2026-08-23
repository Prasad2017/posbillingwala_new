<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
 
  $categoryName = $_POST['categoryName'];
  $categoryId = $_POST['categoryId'];
  $productName = $_POST['productName'];
  $productCode = $_POST['productCode'];
  $productUnit = $_POST['productUnit'];
  $productPrice = $_POST['productPrice'];
  $productCGST = $_POST['productCGST'];
  $productSGST = $_POST['productSGST'];
  $productId = $_POST['productId'];
 
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sql="UPDATE `products` SET `categoryId`='$categoryId', `productCode`='$productCode, `productName`='$productName', `productPrice`='$productPrice', `productUnit`='$productUnit', 
			  `productCGST`='$productCGST', `productSGST`='$productSGST' WHERE `productId`='$productId'";

            if(mysqli_query($con, $sql)){
	
                $response["status"] = '1';
                $response["message"] = "product update successfully";
  
            } else{
    
                $response["status"] = '0';
                $response["message"] = "product failed to update";
 
            }

}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
