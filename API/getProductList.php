<?php	
include_once('config.php');
$i=0;
   
    $response["productResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
//	$sth="SELECT * FROM `products` LEFT JOIN `categories` ON `categories`.`categoryId`=`products`.`categoryId` WHERE `products`.`userId`='$userId' GROUP BY `products`.`productName`";
	$sth="SELECT * FROM `products` LEFT JOIN `categories` ON `categories`.`categoryId`=`products`.`categoryId` WHERE `products`.`userId`='$userId' AND `products`.`productStatus`='active' ORDER BY `productId` DESC";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["productId"]=$row['productId'];
        $getdata["userId"]=$row['userId'];
        $getdata["categoryId"]=$row['categoryId'];
        $getdata["categoryName"]=$row['categoryName'];
        $getdata["productCode"]=$row['productCode'];
        $getdata["productName"]=$row['productName'];
        $getdata["productPrice"]=$row['productPrice'];
        $getdata["productUnit"]=$row['productUnit'];
        $getdata["productCGST"]=$row['productCGST'];
        $getdata["productSGST"]=$row['productSGST'];
        if($row['productStatus'] == 'active') {
           $getdata["productDeletedStatus"]='0';
        } else {
           $getdata["productDeletedStatus"]='1';
        }
       
        $getdata["productNetworkStatus"]=$row['productNetworkStatus'];
        if (!empty($row['subcategoryId'])) {
            $getdata["subcategoryId"] = $row['subcategoryId'];
        }
        
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["productResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>