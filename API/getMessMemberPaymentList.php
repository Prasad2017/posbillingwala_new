<?php	
include_once('config.php');
$i=0;
   
    $response["memberResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `mess_member_payment` WHERE `userId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["paymentId"]=$row['payment_id'];
        $getdata["memberId"]=$row['memberId'];
        $getdata["memberName"]=$row['memberName'];
        $getdata["paymentMessAmount"]=$row['paymentMessAmount'];
        $getdata["paymentPaidAmount"]=$row['paymentPaidAmount'];
        $getdata["messTotalDays"]=$row['messTotalDays'];
        $getdata["paymentDate"]=$row['paymentDate'];
        $getdata["paymentNetworkStatus"]=$row['paymentNetworkStatus'];
        $getdata["paymentStatus"]=$row['paymentStatus'];
       
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["memberResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>