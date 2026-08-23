<?php	
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
$i=0;
   
    $response["customerResponse"] = array();
    mysqli_query($con, 'set names utf8');
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {

        admin_require_auth($con, array('customerResponse' => array()));
        
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `users` WHERE `role_id`='3'";
    $checkuser= mysqli_query($con, $sth);
    $rowcount=mysqli_num_rows($checkuser);

     if($rowcount>0){
      
     while($user=mysqli_fetch_array($checkuser))
	{
       
        $id=$user['id'];
        $name=$user['name'];
        $email=$user['email'];
        $contact_number=$user['contact_number'];
        $aadhar_number=$user['aadhar_number'];
        $address=$user['address'];
        $shopName=$user['shopName'];
        
        $json[] = array("id"=>$id, "name"=>$name, "email"=>$email, "contact_number"=>$contact_number, "aadhar_number"=>$aadhar_number, "address"=>$address, "shopName"=>$shopName);
       
         $json1 = array("customerResponse"=>$json);
            
        }
        
        print_r (json_encode($json1));
	    unset($json);
	    unset($json1);
	    mysqli_close($con);
        
     }
        else { 
           $minfo = array("customerResponse"=>[]);
           $jsondata = json_encode($minfo); 
           print_r($jsondata);   
	       mysqli_close($con); exit();
	  }
    }
    
?>