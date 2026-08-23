<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
  $memberName = $_POST['memberName'];
  $messType = $_POST['messType'];
  $messInvoiceDate = $_POST['messInvoiceDate'];
  $messInvoiceNetworkStatus = $_POST['messInvoiceNetworkStatus'];
  $messInvoiceStatus = $_POST['messInvoiceStatus'];
  $userId = $_POST['userId'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `mess_invoice` WHERE `userId`='$userId' AND `messInvoiceNetworkStatus`='$messInvoiceNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
                 
                   $invoiceId = $check['invoiceId '];
                  $sql="UPDATE `mess_invoice` SET `memberName`='$memberName',`messType`='$messType',`messInvoiceDate`='$messInvoiceDate', 
                        `messInvoiceNetworkStatus`='$messInvoiceNetworkStatus' WHERE `invoiceId` = '$invoiceId'";

                 if(mysqli_query($con,$sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                   }
                
                   
			} else {
			     $sql="INSERT INTO `mess_invoice`(`userId`, `memberName`, `messType`, `messInvoiceDate`, `messInvoiceNetworkStatus`, `messInvoiceStatus`)
                       VALUES ('$userId', '$memberName', '$messType', '$messInvoiceDate', '$messInvoiceNetworkStatus', 'active')";

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
