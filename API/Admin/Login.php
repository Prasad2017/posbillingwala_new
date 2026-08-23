<?php
	include_once('config.php');
	require_once __DIR__ . '/../auth_tokens.php';
	require_once __DIR__ . '/../db_prepared.php';
	mysqli_query($con, 'set names utf8');
	header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: X-Requested-With');
    header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
    header('Content-Type: application/json');

    if ($_SERVER['REQUEST_METHOD'] == 'POST') {

       $data = json_decode(file_get_contents("php://input"));
       
		$userEmail = isset($_POST['userEmail']) ? $_POST['userEmail'] : '';
		if ($userEmail === '' && isset($_POST['userName'])) {
		    $userEmail = $_POST['userName'];
		}
		$password = isset($_POST['password']) ? $_POST['password'] : '';
	
		date_default_timezone_set('Asia/Kolkata');
        $date=date('Y-m-d');

		if(!empty($userEmail) && $password !== ''){
		    
			$check = db_stmt_fetch_one(
			    $con,
			    "SELECT * FROM `users` WHERE `email`=? AND `role_id`='1' AND `is_active`='1' LIMIT 1",
			    's',
			    $userEmail
			);
		
				if($check !== null && isset($check['password']) && password_verify($password, $check['password']))
				{
				    $response["status"] = 'true';
				    $response["userId"] = $check['id'];
					$response["message"] = "Login Successfully.";
					auth_token_append_response($con, $response, 'admin', $check['id']);
						
				} else {
				    $response["status"] = 'false';
					$response["message"] = "Login Failed.";
				}
				
	mysqli_close($con);
	
	}
	else
	{
		$response["status"]='false';
		$response["message"]="Enter valid email & password";
	}
	}
	else
	{
		$response["status"]='false';
		$response["message"]="Use Post Method";
	}
	
	echo json_encode($response);
?>
