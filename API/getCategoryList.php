<?php	
include_once('config.php');
$i=0;
   
    $response["categoryResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT c.*, ft.foodTypeCode, ft.foodTypeId AS linkedFoodTypeId FROM `categories` c
	      LEFT JOIN `food_types` ft ON ft.foodTypeId = c.foodTypeId
	      WHERE c.`userId`='$userId' GROUP BY c.categoryName";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["categoryId"]=$row['categoryId'];
        $getdata["categoryName"]=$row['categoryName'];
        if($row['categoryStatus'] == 'active') {
           $getdata["categoryDeletedStatus"]='0';
         } else {
           $getdata["categoryDeletedStatus"]='1';
         }
        $getdata["categoryNetworkStatus"]=$row['categoryNetworkStatus'];
        if (!empty($row['linkedFoodTypeId'])) {
            $getdata["foodTypeId"] = $row['linkedFoodTypeId'];
        }
        if (!empty($row['foodTypeCode'])) {
            $getdata["foodTypeCode"] = $row['foodTypeCode'];
        }
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["categoryResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>