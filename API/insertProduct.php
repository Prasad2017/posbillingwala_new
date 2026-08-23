<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
     mysqli_query($con, 'set names utf8');
    
  $userId = $_POST['userId'];
  $categoryName = $_POST['categoryName'];
  $categoryId = $_POST['categoryId'];
  $productName = $_POST['productName'];
  $productCode = $_POST['productCode'];
  $productUnit = $_POST['productUnit'];
  $productPrice = $_POST['productPrice'];
  $productCGST = $_POST['productCGST'];
  $productSGST = $_POST['productSGST'];
  $productNetworkStatus = $_POST['productNetworkStatus'];
  $productDeletedStatus = $_POST['productDeletedStatus'];
  $subcategoryId = isset($_POST['subcategoryId']) ? $_POST['subcategoryId'] : '';
  
  if($productDeletedStatus == '1') {
        $productDeletedStatus = 'deactive';
    } else {
        $productDeletedStatus = 'active';
    }
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `products` WHERE `userId`='$userId' AND `productNetworkStatus`='$productNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
				    
				    $productId = $check['productId'];
				    
				    $sql1="SELECT * FROM `categories` WHERE `userId`='$userId' AND `categoryName`='$categoryName'";
		            $res1 = mysqli_query($con, $sql1);
			        $check1 = mysqli_fetch_array($res1);
				    if(isset($check1))
				    {
				    
				       $categoryId = $check1['categoryId'];
				    
				    $subSql = ($subcategoryId != '') ? ", `subcategoryId`='$subcategoryId'" : "";
				    $sql="UPDATE `products` SET `categoryId`='$categoryId', `productCode`='$productCode', `productName`='$productName', `productPrice`='$productPrice', `productUnit`='$productUnit', 
				          `productCGST`='$productCGST', `productSGST`='$productSGST', `productStatus`='$productDeletedStatus'$subSql WHERE `productId`='$productId'";
				    
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
				    
				    
				    $sql1="SELECT * FROM `categories` WHERE `userId`='$userId' AND `categoryName`='$categoryName'";
		            $res1 = mysqli_query($con, $sql1);
			        $check1 = mysqli_fetch_array($res1);
				    if(isset($check1))
				    {
				    
				       $categoryId = $check1['categoryId'];

                 $subCol = ($subcategoryId != '') ? ", `subcategoryId`" : "";
                 $subVal = ($subcategoryId != '') ? ", '$subcategoryId'" : "";
                 $sql="INSERT INTO `products`(`userId`, `categoryId`, `productCode`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productNetworkStatus`, `productStatus`$subCol) 
                       VALUES ('$userId', '$categoryId', '$productCode', '$productName', '$productPrice', '$productUnit', '$productCGST', '$productSGST', '$productNetworkStatus', '$productDeletedStatus'$subVal)";

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
}

header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
