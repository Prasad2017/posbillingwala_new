<?php
include_once('config.php');

$response = array();
if($_SERVER['REQUEST_METHOD']=='POST'){
    
    mysqli_query($con, 'set names utf8');
    
 
  $licenseId = $_POST['licenseId'];
  $totalSaleData = $_POST["totalSaleData"];
  $todaySaleData = $_POST["todaySaleData"];
  
	date_default_timezone_set('Asia/Kolkata');
    $date=date('Y-m-d');
    
				    
		$sth="UPDATE `licenses` SET `total_sale_data`='$totalSaleData', `today_sale_data`='$todaySaleData' WHERE `id`='$licenseId'";
	
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
