<?php
if(isset($_GET['number'])){
	$j=json_decode(file_get_contents("php://input"),true);
	$arr = array();

	$mobileNumber = $_GET['number'];
	$message = $_GET['message'];
	
	$template_msg = "Your SONICD CRM verification OTP code is ".$message.". Please DO NOT share this OTP with anyone";

                           
                                $username = "2000232595";
                                $password= "Sonic$123";
                                $campaign= 8056;
                                $routeid = 37;
                                $type = "text";
                                $contacts = $mobileNumber;
                                $senderid = "SONICD";
                                $msg = $template_msg;
                                $template_id = "1707169926599992375";
                          

                            $ch = curl_init();
                            curl_setopt($ch,CURLOPT_URL, "https://enterprise.smsgupshup.com/GatewayAPI/rest");
                            curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
                            curl_setopt($ch, CURLOPT_POST, 1);
                            curl_setopt($ch, CURLOPT_POSTFIELDS, 
										"auth_scheme=PLAIN&method=sendMessage&userid=".$username."&password=".$password."&msg_type=".$type."&send_to=".$contacts."&senderid=".$senderid."&msg=".$msg."&template_id=".$template_id);


                            $response = curl_exec($ch);
                           curl_close($ch);
	                       echo $response;

                           if (substr($response,0,13) == 'SMS-SHOOT-ID/') {
                                echo json_encode(array("success"=>"1"));
                            } else {
                                echo json_encode(array("success"=>"0"));
                            }
		
	 
		
	}else{
		echo json_encode(array("success"=>"0"));
	}
?>