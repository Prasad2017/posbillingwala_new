<?php	
include_once('config.php');
$i=0;
   
    $response["expensesResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `expenses` WHERE `userId`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["expensesId"]=$row['expensesId'];
        $getdata["expensesName"]=$row['expensesName'];
        $getdata["expensesAmount"]=$row['expensesAmount'];
        $getdata["expensesDate"]=$row['expensesDate'];
        $getdta["expensesNetworkStatus"]=$row['expensesNetworkStatus'];
        $getdta["expensesStatus"]=$row['expensesStatus'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["expensesResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    }
?>