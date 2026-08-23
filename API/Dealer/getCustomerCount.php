<?php
	include_once('config.php');
	require_once __DIR__ . '/auth_guard.php';
	
	if($_SERVER['REQUEST_METHOD']=='GET')
	{
		
		dealer_require_auth($con, array('status' => 'false', 'totalCustomer' => '0'));
		
		$userId=$_GET['userId'];
	
		date_default_timezone_set('Asia/Kolkata');
        $date=date('Y-m-d');

		if(isset($userId)){
		    
			$sql="SELECT COUNT(*) as totalCustomer FROM `users` WHERE `role_id`= '3' AND `dealerId`='$userId'";
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
		
				if(isset($check))
				{
				        
				   if($check['totalCustomer']!=null){
				            $response["status"] = 'true';
					        $response["message"] = $check['totalCustomer'];
				        } else {
				            $response["status"] = 'false';
					        $response["message"] = "0";
				        }
						
				
				} else {
				    $response["status"] = 'false';
					$response["message"] = "0";
				}
				
	mysqli_close($con);
	
	}
	}
	else
	{
		$response["status"]='false';
		$response["message"]="0";
	}
	
	echo json_encode($response);
?>