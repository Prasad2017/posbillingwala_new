<?php
	include_once('config.php');
	require_once __DIR__ . '/../auth_tokens.php';
	
	if($_SERVER['REQUEST_METHOD']=='GET')
	{
		
		$userId = isset($_GET['userId']) ? $_GET['userId'] : '';
		$userId = auth_user_id_from_request($con, $userId, 'owner');
		if ($userId === null) {
		    header('Content-type: application/json; charset=utf-8');
		    echo json_encode(array('status' => '0', 'message' => 'Invalid or expired auth token'));
		    exit;
		}
	
		date_default_timezone_set('Asia/Kolkata');
        $date=date('Y-m-d');
		    
			$sql_category="SELECT COUNT(*) as `totalCategory` FROM `categories`
                          LEFT JOIN `users` on `users`.`id` = `categories`.`userId` 
                          WHERE `users`.`id`='$userId';";
		    $res_category = mysqli_query($con, $sql_category);
			$check_category = mysqli_fetch_array($res_category);
			
			$sql_product="SELECT COUNT(*) as `totalProduct` FROM `products`
                          LEFT JOIN `users` on `users`.`id` = `products`.`userId` 
                          WHERE `users`.`id`='$userId';";
		    $res_product = mysqli_query($con, $sql_product);
			$check_product = mysqli_fetch_array($res_product);
			
			$sql_total_sale="SELECT SUM(`totalAmount`) as `totalSale` FROM `invoice` 
			             LEFT JOIN `licenses` ON `licenses`.`id` = `invoice`.`licenseId` 
			             LEFT JOIN `users` ON `users`.`id` = `licenses`.`userId` 
			             WHERE `users`.`id` = '$userId'";
		    $res_total_sale = mysqli_query($con, $sql_total_sale);
			$check_total_sale = mysqli_fetch_array($res_total_sale);
			
			$sql_today_sale="SELECT SUM(`totalAmount`) as `todaySale` FROM `invoice` 
			             LEFT JOIN `licenses` ON `licenses`.`id` = `invoice`.`licenseId` 
			             LEFT JOIN `users` ON `users`.`id` = `licenses`.`userId` 
			             WHERE `users`.`id` = '$userId' AND `invoice`.`invoiceDate` LIKE '%$date%'";
		    $res_today_sale = mysqli_query($con, $sql_today_sale);
			$check_today_sale = mysqli_fetch_array($res_today_sale);

			$sql_branch_count="SELECT COUNT(*) AS `branchCount` FROM `licenses` WHERE `userId`='$userId'";
		    $res_branch_count = mysqli_query($con, $sql_branch_count);
			$check_branch_count = mysqli_fetch_array($res_branch_count);
		
				if(isset($check_category) && isset($check_product) && isset($check_total_sale) && isset($check_today_sale))
				{
				        
				        $response["status"] = 'true';
				        
				    if($check_category['totalCategory']!=null) {
				    	$response["totalCategory"] = $check_category['totalCategory'];
				    } else {
				        $response["totalCategory"] = "0";
				    }
				    
				    if($check_product['totalProduct']!=null) {
				    	$response["totalProduct"] = $check_product['totalProduct'];
				    } else {
				        $response["totalProduct"] = "0";
				    }
				    
				    if($check_total_sale['totalSale']!=null) {
					    $response["totalSale"] = $check_total_sale['totalSale'];
				    } else {
				        $response["totalSale"] = "0";
				    }
				    
				    if($check_today_sale['todaySale']!=null) {
					    $response["todaySale"] = $check_today_sale['todaySale'];
				    } else {
				        $response["todaySale"] = "0";
				    }

				    if($check_branch_count['branchCount']!=null) {
					    $response["branchCount"] = $check_branch_count['branchCount'];
				    } else {
				        $response["branchCount"] = "0";
				    }
				
				} else {
				    
				        $response["status"] = 'false';
					    $response["totalCategory"] = "0";
						$response["totalProduct"] = "0";
					    $response["totalSale"] = "0";
					    $response["todaySale"] = "0";
					    $response["branchCount"] = "0";
					
				}
				
	mysqli_close($con);
	
	}
	
	echo json_encode($response);
?>