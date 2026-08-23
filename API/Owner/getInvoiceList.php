<?php	
include_once('config.php');
$i=0;
   
    $response["invoiceResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `invoice`
          LEFT JOIN `licenses` ON `licenses`.`id` = `invoice`.`licenseId`
          LEFT JOIN `users` ON `users`.`id` = `licenses`.`userId`
          WHERE `users`.`id` = '$userId' ORDER BY `invoiceId` DESC";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["invoiceId"]=$row['invoiceId'];
        $getdata["userId"]=$row['licenseId'];
        $getdata["noOfTable"]=$row['noOfTable'];
        $getdata["invoiceType"]=$row['invoiceType'];
        $getdata["invoiceNumber"]=$row['invoiceNumber'];
        $getdata["customerName"]=$row['customerName'];
        $getdata["customerMobile"]=$row['customerMobile'];
        $getdata["customerEmail"]=$row['customerEmail'];
        $getdata["customerAddress"]=$row['customerAddress'];
        $getdata["subTotal"]=$row['subTotal'];
        $getdata["totalGSTAmount"]=$row['totalGSTAmount'];
        $getdata["discount"]=$row['discount'];
        $getdata["discountType"]=$row['discountType'];
        $getdata["totalAmount"]=$row['totalAmount'];
        $getdata["paymentMode"]=$row['paymentMode'];
        $getdata["invoiceDate"]=$row['invoiceDate'];
        $getdata["invoiceOrderStatus"]=$row['invoiceOrderStatus'];
        $getdata["invoiceNetworkStatus"]=$row['invoiceNetworkStatus'];
        
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["invoiceResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>