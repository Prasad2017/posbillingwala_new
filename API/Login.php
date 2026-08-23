<?php
	include_once('config.php');
	include_once('licence_expiry.php');
	mysqli_query($con, 'set names utf8');
	header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: X-Requested-With');
    header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
    header('Content-Type: application/json');

    if ($_SERVER['REQUEST_METHOD'] == 'POST') {

       $data = json_decode(file_get_contents("php://input"));
       
		$android_device_id=$_POST['android_device_id'];
		$app_licence_key=$_POST['app_licence_key'];
	
		$date = licence_today();

		if(!empty($app_licence_key)){
		    
			$check = db_stmt_fetch_one(
				$con,
				"SELECT `licenses`.*, `users`.`shopName`, `users`.`shopImage`, `users`.`reportPin` FROM `licenses`
			      LEFT JOIN `users` ON `users`.`id`= `licenses`.`userId`
			      WHERE `licenseKey`=? AND `licenseStatus`='active'",
				's',
				$app_licence_key
			);
		
				if($check !== null)
				{
				    $check = licence_sync_trial_consumed_state($con, $check);

				    if (!licence_trial_allows_login($con, $check)) {
				        $response["status"] = '0';
					    $response["message"] = "Trial already used on this licence. Please upgrade to continue.";
				    // P4-1: enforce expiryDate even when licenseStatus was left active
				    } else if (!licence_enforce_expiry($con, $check)) {
				        $response["status"] = '0';
					    $response["message"] = "licence key expired or user disable. Please contact our customer care or dealer";
				    } else if($check['android_device_id'] == $android_device_id) {
				        
				        $response["status"] = '1';
				        $response["message"] = "Login successfully.";
				        
						$response["licenceId"] = $check['id'];
						$response["ownerId"] = $check['userId'];
						$response["userName"] = $check['userName'];
						$response["shopName"] = $check['shopName'];
						$response["shopImage"] = $check['shopImage'];
						$response["reportPin"] = $check['reportPin'];
						$response["fastBilling"] = $check['fastBilling'];
						$response["takeAway"] = $check['takeAway'];
						$response["dineIn"] = $check['dineIn'];
						$response["mess"] = $check['mess'];
						$response["licenceKey"] = $check['licenseKey'];
						$response["mpin"] = $check['mpin'];
						$response["licence_key_reg_date"] = $check['created_at'];
						$response["licence_key_expire_date"] = $check['expiryDate'];
						$response["totalSaleData"] = $check['total_sale_data'];
						$response["todaySaleData"] = $check['today_sale_data'];
						
						$response = licence_append_trial_response($con, $response, $check);
						
				    } else if($check['android_device_id'] == null) {
				        $response["status"] = '2';
					    $response["message"] = "Login Failed";
				    } else {
				    
				        $response["status"] = '3';
					    $response["message"] = "Already login in ".$check['android_device_name']." mobile";
				    }
						
				} else {
				    $response["status"] = '0';
					$response["message"] = "licence key expired or user disable. Please contact our customer care or dealer";
				}
				
	mysqli_close($con);
	
	}
	else
	{
		$response["status"]='0';
		$response["message"]="Enter valid licence key";
	}
	}
	else
	{
		$response["status"]='0';
		$response["message"]="Use Post Method";
	}
	
	echo json_encode($response);
?>
