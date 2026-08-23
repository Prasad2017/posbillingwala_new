<?php
	include_once('config.php');
	require_once __DIR__ . '/../auth_tokens.php';
	mysqli_query($con, 'set names utf8');
	header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: X-Requested-With');
    header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
    header('Content-Type: application/json');

    if ($_SERVER['REQUEST_METHOD'] == 'POST') {

       $data = json_decode(file_get_contents("php://input"));
       
		$contactNumber=$_POST['contactNumber'];
	
		date_default_timezone_set('Asia/Kolkata');
        $date=date('Y-m-d');

		if(!empty($contactNumber)){
		    
			$sql="SELECT * FROM `users` WHERE `contact_number`='$contactNumber' AND `is_active`='1'";
			      
		    $res = mysqli_query($con, $sql);
			$check = mysqli_fetch_array($res);
		
				if(isset($check))
				{
				 
				        $response["status"] = '1';
				        $response["message"] = "Login successfully.";
				        
						$response["userId"] = $check['id'];
						$response["reportPin"] = $check['reportPin'];
						$response["contact_number"] = $check['contact_number'];
						auth_token_append_response($con, $response, 'owner', $check['id']);
						
				} else {
				    $response["status"] = '0';
					$response["message"] = "licence key expired or user disable. Please contact our customer care or dealer";
				}
				
	mysqli_close($con);
	
	}
	else
	{
		$response["status"]='0';
		$response["message"]="Enter valid mobile number";
	}
	}
	else
	{
		$response["status"]='0';
		$response["message"]="Use Post Method";
	}
	
	echo json_encode($response);
?>