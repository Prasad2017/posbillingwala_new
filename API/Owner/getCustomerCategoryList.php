<?php	
include_once('config.php');
$i=0;
   
    $response["categoryResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `categories` WHERE `userId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["categoryId"]=$row['categoryId'];
        $getdata["categoryName"]=$row['categoryName'];
        $getdata["categoryStatus"]=$row['categoryStatus'];
        $getdata["categoryNetworkStatus"]=$row['categoryNetworkStatus'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["categoryResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>