<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
  $invoiceNumber = $_POST['invoiceNumber'];
  $productName = $_POST['productName'];
  $productPrice = $_POST['productPrice'];
  $productUnit = $_POST['productUnit'];
  $productCGST = $_POST['productCGST'];
  $productSGST = $_POST['productSGST'];
  $productQuantity = $_POST['productQuantity'];
  $productStatus = $_POST['productStatus'];
  $invoiceProductNetworkStatus = $_POST['invoiceProductNetworkStatus'];
  $portionId = isset($_POST['portionId']) ? $_POST['portionId'] : '';
  $portionName = isset($_POST['portionName']) ? $_POST['portionName'] : '';
  $snapshotProductName = isset($_POST['snapshotProductName']) ? $_POST['snapshotProductName'] : '';
  $snapshotLinePrice = isset($_POST['snapshotLinePrice']) ? $_POST['snapshotLinePrice'] : '';
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `invoice_final_product` WHERE `invoiceNumber`='$invoiceNumber' AND `invoiceProductNetworkStatus`='$invoiceProductNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
				    
				    $invoiceProductId = $check['invoiceProductId'];
				    $snapshotSql = "";
				    if ($portionId != '') { $snapshotSql .= ", `portionId`='$portionId'"; }
				    if ($portionName != '') { $snapshotSql .= ", `portionName`='$portionName'"; }
				    if ($snapshotProductName != '') { $snapshotSql .= ", `snapshotProductName`='$snapshotProductName'"; }
				    if ($snapshotLinePrice != '') { $snapshotSql .= ", `snapshotLinePrice`='$snapshotLinePrice'"; }
				    $sql="UPDATE `invoice_final_product` SET `invoiceNumber`='$invoiceNumber', `productName`='$productName', `productPrice`='$productPrice', `productUnit`='$productUnit', `productCGST`='$productCGST', `productSGST`='$productSGST', 
				          `productQuantity`='$productQuantity', `productStatus`='$productStatus', `invoiceProductNetworkStatus`='$invoiceProductNetworkStatus'$snapshotSql WHERE `invoiceProductId`='$invoiceProductId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 

                 $snapCols = "";
                 $snapVals = "";
                 if ($portionId != '') { $snapCols .= ", `portionId`"; $snapVals .= ", '$portionId'"; }
                 if ($portionName != '') { $snapCols .= ", `portionName`"; $snapVals .= ", '$portionName'"; }
                 if ($snapshotProductName != '') { $snapCols .= ", `snapshotProductName`"; $snapVals .= ", '$snapshotProductName'"; }
                 if ($snapshotLinePrice != '') { $snapCols .= ", `snapshotLinePrice`"; $snapVals .= ", '$snapshotLinePrice'"; }
                 $sql="INSERT INTO `invoice_final_product`(`invoiceNumber`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productQuantity`, `productStatus`, `invoiceProductNetworkStatus`$snapCols)
                       VALUES ('$invoiceNumber', '$productName', '$productPrice', '$productUnit', '$productCGST', '$productSGST', '$productQuantity', '$productStatus', '$invoiceProductNetworkStatus'$snapVals)";


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
