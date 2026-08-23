<?php	
include_once('config.php');

   mysqli_query($con, 'set names utf8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: X-Requested-With');
    header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
    header('Content-Type: application/json');
  
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $data = json_decode(file_get_contents("php://input"));
        
        $licenseKey = $_GET['licenceKey'];
  
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `licenses` WHERE `licenseKey`='$licenseKey'";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
        $getdata = array();
       
        $android_device_id = $row['android_device_id'];
        $android_device_name = $row['android_device_name'];
        $licence_id = $row['id'];
        
						    
		$sql_update="UPDATE `licenses` SET `android_device_name`='$android_device_name', `android_device_id`='$android_device_id'  WHERE `id` ='$licence_id'";			
		$res_update=mysqli_query($con, $sql_update);
						
    }
        
        	$jsonmain=array("status"=>'true', "message"=>"Logout statusfully");
        	
        		print_r (json_encode($jsonmain));
       	mysqli_close($con);
            
        } else {
    $jsonmain=array("status"=>'false', "message"=>"Logout failed");
	print_r (json_encode($jsonmain));
	mysqli_close($con);
        }
    } else {
    $jsonmain=array("status"=>'false', "message"=>"Logout failed");
	print_r (json_encode($jsonmain));
	mysqli_close($con);
        
    }
    }else {
   $jsonmain=array("status">'false', "message"=>"Logout failed");
	print_r (json_encode($jsonmain));
	mysqli_close($con);
    }
?>