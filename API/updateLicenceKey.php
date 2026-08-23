<?php	
include_once('config.php');
$i=0;
   mysqli_query($con, 'set names utf8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: X-Requested-With');
    header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
    header('Content-Type: application/json');
  
    
    if ($_SERVER['REQUEST_METHOD'] == "GET") {
        
        $data = json_decode(file_get_contents("php://input"));
  
        date_default_timezone_set("Asia/Calcutta");
        $date = date("Y-m-d");
        
	$sth="SELECT * FROM `licenses`";

    if ($result = mysqli_query($con, $sth))
    {
        if (mysqli_num_rows($result) > 0)
        {
            
    while($row=mysqli_fetch_assoc($result))
    {
      
        $licence_key_valid_days = $row['licenseValidity'];
        $expiryDate = $row['expiryDate'];
        $licence_id = $row['id'];
		
		$date1=date_create($date);
        $date2=date_create($expiryDate);
        $diff=date_diff($date1, $date2);
   
	                   // P4-1: expire only after expiryDate (valid while days remaining >= 0)
	                   if (($diff->format("%R%a")) < 0) {
				               $sql_update="UPDATE `licenses` SET `licenseValidity`='0', `licenseStatus`='expire' WHERE `id` ='$licence_id'";	
						} else {
						    
						    $datediff  = $diff->format("%a");
						   
						   $sql_update="UPDATE `licenses` SET `licenseValidity`='$datediff', `licenseStatus`='active'  WHERE `id` ='$licence_id'";			
						}
						
					 
						$res_update=mysqli_query($con, $sql_update);
						
        }
        
        	$jsonmain=array("success"=>'true', "message"=>"update key successfully");
        		print_r (json_encode($jsonmain));
       	mysqli_close($con);
            
        } else {
    $jsonmain=array("success"=>'false', "message"=>"update key failed");
	print_r (json_encode($jsonmain));
	mysqli_close($con);
        }
    } else {
    $jsonmain=array("success"=>'false', "message"=>"update key failed");
	print_r (json_encode($jsonmain));
	mysqli_close($con);
        
    }
    }else {
   $jsonmain=array("success">'false', "message"=>"update key failed");
	print_r (json_encode($jsonmain));
	mysqli_close($con);
    }
?>