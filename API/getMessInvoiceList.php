<?php	
include_once('config.php');
$i=0;
   
    $response["memberInvoiceResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `mess_invoice` WHERE `userId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["invoiceId"]=$row['invoiceId'];
        $getdata["memberName"]=$row['memberName'];
        $getdata["messType"]=$row['messType'];
        $getdata["messInvoiceDate"]=$row['messInvoiceDate'];
        $getdata["messInvoiceNetworkStatus"]=$row['messInvoiceNetworkStatus'];
        $getdata["messInvoiceStatus"]=$row['messInvoiceStatus'];
        
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["memberInvoiceResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>