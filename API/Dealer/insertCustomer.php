<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../user_identity.php';
require_once __DIR__ . '/auth_guard.php';

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    dealer_require_auth($con);
    
    mysqli_query($con, 'set names utf8');
    
  $name = $_POST['name'];
  $userType = $_POST['userType'];
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
  $mess = $_POST['mess'];
  $dealerId = $_POST['userId'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
    $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
    if($licenseType == 'Demo') {
        $paymentStatus = "";
    } else {
        $paymentStatus = "cash";
    }
    
    if($userType == 'Dealer') {
        $roleId = '2';
    } else {
        $roleId = '3';
    }

    $contact_number = licence_normalize_contact($contact_number);
    if (strlen($contact_number) < 10) {
        $response["status"] = 'false';
        $response["message"] = "Please enter a valid 10-digit mobile number.";
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if ($roleId === '3' && user_customer_mobile_taken($con, $contact_number)) {
        $response["status"] = 'false';
        $response["message"] = "This mobile number is already registered for another customer.";
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if ($roleId === '2' && user_dealer_mobile_taken($con, $contact_number)) {
        $response["status"] = 'false';
        $response["message"] = "This mobile number is already registered for another dealer.";
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }
    
    $sql="INSERT INTO `users`(`name`, `contact_number`, `address`, `is_active`, `shopName`, `dealerId`, `role_id`) 
          VALUES ('$name', '$contact_number', '$address', '1', '$shopName', '$dealerId', '$roleId')";
				if(mysqli_query($con, $sql))
				{
				    
				    $customerId=mysqli_insert_id($con);
				    $licenseValidity = licence_apply_trial_validity($licenseType, $licenseValidity);
				    
				    $expiryDate = date('Y-m-d', strtotime($date .' +'.$licenseValidity.' day'));
				    
				    $defaultMpin = licence_default_mpin();
				    $defaultReportPin = licence_default_report_pin();
				    
				    $sth="INSERT INTO `licenses`(`userId`, `licenseKey`, `licenseValidity`, `licenseType`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType`, `userName`, `mpin`, `fastBilling`, `takeAway`, `dineIn`, `mess`)
				          VALUES ('$customerId', '$licenseKey', '$licenseValidity', '$licenseType', 'active', '$expiryDate', '$paymentStatus', '$amount', 'owner', '$name', '$defaultMpin', '$fastBilling', '$takeAway', '$dineIn', '$mess')";

                 if(mysqli_query($con, $sth)){
	
                       $response["status"] = 'true';
                       $response["message"] = "registration successful!";
                       $response["licenseKey"] = $licenseKey;
                       $response["mpin"] = $defaultMpin;
                       $response["reportPin"] = $defaultReportPin;
  
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
