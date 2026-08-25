<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);


$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
 
  $userId = $_POST['userId'];
$userId = owner_resolve_user_id($con, $userId);
if ($userId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status'=>'0','message'=>'Unauthorized'));
    mysqli_close($con);
    exit;
}

  $reportPin = $_POST['reportPin'];
 
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sth="UPDATE `users` SET `reportPin`='$reportPin' WHERE `id`='$userId'";

            if(mysqli_query($con, $sth)){
	
                $response["status"] = '1';
                $response["message"] = "pin update successfully";
  
            } else{
    
                $response["status"] = '0';
                $response["message"] = "pin failed to update";
 
            }

}
header('Content-type: application/json; charset=utf-8');
	echo json_encode($response);
?>
