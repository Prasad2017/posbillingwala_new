<?php	
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);

$i=0;
   
    $response["categoryResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
$userId = owner_resolve_user_id($con, $userId);
if ($userId === null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array('status'=>'0','message'=>'Unauthorized'));
    mysqli_close($con);
    exit;
}

        
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