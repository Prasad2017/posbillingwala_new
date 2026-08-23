<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
  $memberName = $_POST['memberName'];
  $memberId = $_POST['memberId'];
  $paymentMessAmount = $_POST['paymentMessAmount'];
  $paymentPaidAmount = $_POST['paymentPaidAmount'];
  $messTotalDays = $_POST['messTotalDays'];
  $paymentDate = $_POST['paymentDate'];
  $paymentNetworkStatus = $_POST['paymentNetworkStatus'];
  $paymentStatus = $_POST['paymentStatus'];
  $userId = $_POST['userId'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `mess_member_payment` WHERE `userId`='$userId' AND `paymentNetworkStatus`='$paymentNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
                 
                   $paymentId = $check['payment_id'];
                  $sql="UPDATE `mess_member_payment` SET `memberName`='$memberName',`paymentMessAmount`='$paymentMessAmount',`paymentPaidAmount`='$paymentPaidAmount',`messTotalDays`='$messTotalDays',
                        `paymentDate`='$paymentDate',`paymentNetworkStatus`='$paymentNetworkStatus' WHERE `payment_id` = '$paymentId'";

                 if(mysqli_query($con,$sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                   }
                
                   
			} else {
			     $sql="INSERT INTO `mess_member_payment`(`userId`, `memberId`, `memberName`, `paymentMessAmount`, `paymentPaidAmount`, `messTotalDays`, `paymentDate`, `paymentNetworkStatus`, `paymentStatus`)
                       VALUES ('$userId', '$memberId', '$memberName', '$paymentMessAmount', '$paymentPaidAmount', '$messTotalDays', '$paymentDate', '$paymentNetworkStatus', 'active')";

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
