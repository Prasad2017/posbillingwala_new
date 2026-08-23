<?php
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';

$response = array('customerResponse' => array());
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    dealer_require_auth($con, array('customerResponse' => array()));
        
        $userId = $_GET['userId'];
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `users` WHERE `id`='$userId'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $getdata["id"]=$row['id'];
        $getdata["name"]=$row['name'];
        $getdata["email"]=$row['email'];
        $getdata["contact_number"]=$row['contact_number'];
        $getdata["aadhar_number"]=$row['aadhar_number'];
        $getdata["address"]=$row['address'];
       
        header('Content-type: application/json; charset=utf-8');
    
        array_push($response["customerResponse"], $getdata);
        }
            
        }
    }
    
    echo json_encode($response);
    
    }
?>