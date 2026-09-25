<?php	
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

$response = array();
$response['status'] = '1';
$response['message'] = 'ok';
$response['memberResponse'] = array();
mysqli_query($con, 'set names utf8');
mess_common_ensure_schema($con);

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    $__postedUserId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $userId = pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status'=>'0','message'=>'Unauthorized'));
    $userId = mysqli_real_escape_string($con, (string) $userId);

    date_default_timezone_set('Asia/Calcutta');

    /* Normalize DB status (active/inactive) to Android/Flutter codes: 1=active, 2=deleted. */
    $sth = "SELECT * FROM `mess_member` WHERE `userId`='$userId'";
    if ($result = mysqli_query($con, $sth)) {
        while ($row = mysqli_fetch_assoc($result)) {
            $rawStatus = isset($row['member_status'])
                ? strtolower(trim((string) $row['member_status']))
                : '1';
            if ($rawStatus === '2' || $rawStatus === 'inactive' || $rawStatus === 'deleted') {
                $memberStatus = '2';
            } else {
                /* active, 1, empty, or unknown → treat as active */
                $memberStatus = '1';
            }

            $getdata = array();
            $getdata['memberId'] = $row['id'];
            $getdata['memberName'] = $row['member_name'];
            $getdata['memberMobileNumber'] = $row['member_mobile_number'];
            $getdata['memberAltenetMobileNumber'] = $row['member_altenet_mobile_number'];
            $getdata['memberAddress'] = $row['member_address'];
            $getdata['registrationNo'] = isset($row['registration_no']) ? $row['registration_no'] : '';
            $getdata['memberType'] = isset($row['member_type']) ? $row['member_type'] : 'student';
            $getdata['rollNo'] = isset($row['roll_no']) ? $row['roll_no'] : '';
            $getdata['college'] = isset($row['college']) ? $row['college'] : '';
            $getdata['studentYear'] = isset($row['student_year']) ? $row['student_year'] : '';
            $getdata['company'] = isset($row['company']) ? $row['company'] : '';
            $getdata['memberStatus'] = $memberStatus;
            $getdata['memberNetworkStatus'] = $row['member_network_status'];

            $response['memberResponse'][] = $getdata;
        }
    }

    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
}
?>
