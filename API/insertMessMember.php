<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
  $memberName = $_POST['memberName'];
  $memberMobileNumber = $_POST['memberMobileNumber'];
  $memberAltenetMobileNumber = $_POST['memberAltenetMobileNumber'];
  $memberAddress = $_POST['memberAddress'];
  $memberNetworkStatus = $_POST['memberNetworkStatus'];
  $memberStatus = $_POST['memberStatus'];
  $userId = $_POST['userId'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $sql="SELECT * FROM `mess_member` WHERE `userId`='$userId' AND `member_network_status`='$memberNetworkStatus'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
				if(isset($check))
				{
				    
				    $memberId = $check['id'];
				    $sql="UPDATE `mess_member` SET `memberName`='$memberName', `member_mobile_number`='$memberMobileNumber',
				          `member_altenet_mobile_number`='$memberAltenetMobileNumber', `member_address`='$memberAddress' WHERE `id`='$memberId'";

                 if(mysqli_query($con, $sql)){
	
                       $response["status"] = '1';
                       $response["message"] = "update successful!";
  
                   }
                   else{
    
                        $response["status"] = '0';
                        $response["message"] = "update failed!";
 
                     }
				    
				    
				} else {

                 
                 $sql="INSERT INTO `mess_member`(`userId`, `member_name`, `member_mobile_number`, `member_altenet_mobile_number`, `member_address`, `member_status`, `member_network_status`) 
                       VALUES('$userId', '$memberName', '$memberMobileNumber', '$memberAltenetMobileNumber', '$memberAddress', 'active', '$memberNetworkStatus')";

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
