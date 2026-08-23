<?php
	include_once('config.php');
	require_once __DIR__ . '/auth_guard.php';
	
	if($_SERVER['REQUEST_METHOD']=='GET')
	{
		
		admin_require_auth($con, array('status' => 'false', 'totalCustomer' => '0', 'totalDealer' => '0'));
		
	
		date_default_timezone_set('Asia/Kolkata');
        $date=date('Y-m-d');
		    
			$sql_customer="SELECT COUNT(*) as totalCustomer FROM `users` WHERE `role_id`='3'";
		    $res_customer = mysqli_query($con, $sql_customer);
			$check_customer = mysqli_fetch_array($res_customer);
			
			$sql_dealer="SELECT COUNT(*) as totalDealer FROM `users` WHERE `role_id`='2'";
		    $res_dealer = mysqli_query($con, $sql_dealer);
			$check_dealer = mysqli_fetch_array($res_dealer);
		
				if(isset($check_customer) && isset($check_dealer))
				{
				        
				    $response["status"] = 'true';
					$response["totalCustomer"] = $check_customer['totalCustomer'];
					$response["totalDealer"] = $check_dealer['totalDealer'];
				
				} else {
				    
				    $response["status"] = 'false';
					$response["totalCustomer"] = "0";
					$response["totalDealer"] = "0";
					
				}
				
	mysqli_close($con);
	
	}
	
	echo json_encode($response);
?>