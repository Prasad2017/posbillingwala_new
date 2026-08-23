<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    dealer_require_auth($con);
    
    mysqli_query($con, 'set names utf8');
    
 
  $userId = $_POST['userId'];
  $dealerName = $_POST['dealerName'];
  $dealerMobileNumber = $_POST['dealerMobileNumber'];
  $dealerAddress = $_POST['dealerAddress'];
  $dealerEmail = $_POST['dealerEmail'];
  $dealerAadhaarNumber = $_POST['dealerAadhaarNumber'];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sth="UPDATE `users` SET `name`='$dealerName', `contact_number`='$dealerMobileNumber', `address`='$dealerAddress', `email`='$dealerEmail', 
		      `aadhar_number`='$dealerAadhaarNumber' WHERE `id`='$userId'";

            if(mysqli_query($con, $sth)){
	
                $response["status"] = '1';
                $response["message"] = "update successful!";
  
            } else{
    
                $response["status"] = '0';
                $response["message"] = "update failed...";
 
            }

}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
