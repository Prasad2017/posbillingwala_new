<?php	
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);

$i=0;
   
    $response["productResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $productId = $_GET['productId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT p.*, c.categoryName, ps.subcategoryName
              FROM `products` p
              LEFT JOIN `categories` c ON c.categoryId = p.categoryId
              LEFT JOIN `product_subcategories` ps ON ps.subcategoryId = p.subcategoryId
              WHERE p.`productId`='$productId'";

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
        $getdata["subcategoryId"]=isset($row['subcategoryId']) ? $row['subcategoryId'] : '';
        $getdata["subcategoryName"]=isset($row['subcategoryName']) ? $row['subcategoryName'] : '';
        $getdata["productName"]=$row['productName'];
        $getdata["productCode"]=$row['productCode'];
        $getdata["productPrice"]=$row['productPrice'];
        $getdata["productUnit"]=$row['productUnit'];
        $getdata["productCGST"]=$row['productCGST'];
        $getdata["productSGST"]=$row['productSGST'];
        $getdata["productStatus"]=$row['productStatus'];
        $getdata["productNetworkStatus"]=$row['productNetworkStatus'];
        
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["productResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>
