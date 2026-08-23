<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    admin_require_auth($con);
    
    mysqli_query($con, 'set names utf8');
    
  $name = $_POST['name'];
  $contact_number = $_POST['contact_number'];
  $address = $_POST['address'];
  $shopName = $_POST['shopName'];
  $licenseKey = $_POST['licenseKey'];
  $licenseValidity = $_POST['licenseValidity'];
  $licenseType = $_POST['licenseType'];
  $amount = $_POST['amount'];
  $fastBilling = $_POST['fastBilling'];
  $takeAway = $_POST['takeAway'];
  $dineIn = $_POST['dineIn'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');

    $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
    
    $sql="INSERT INTO `users`(`role_id`, `name`, `contact_number`, `address`, `is_active`, `shopName`) VALUES ('3', '$name', '$contact_number', '$address', '1', '$shopName')";
				if(mysqli_query($con, $sql))
				{
				    
				    $customerId=mysqli_insert_id($con);
				    
				    $expiryDate = date('Y-m-d', strtotime($date .' +'.$licenseValidity.' day'));
				    
				    $sth="INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `fastBilling`, `takeAway`, `dineIn`)
				          VALUES ('$customerId', '$licenseKey', '$licenseValidity', '$licenseType', 'active', '$expiryDate', 'cash', '$amount', 'owner', '$name', '$fastBilling', '$takeAway', '$dineIn')";

                 if(mysqli_query($con, $sth)){
	
                       $response["status"] = 'true';
                       $response["message"] = "registration successful!";
  
                   }
                   else{
    
                        $response["status"] = 'false';
                        $response["message"] = "registration failed...";
 
                     }
				    
				} else{
    
                        $response["status"] = 'false';
                        $response["message"] = "registration failed!";
 
                     }
}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
