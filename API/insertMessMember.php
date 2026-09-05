<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/mess_common_helpers.php';

$response = array();
mess_common_ensure_schema($con);

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8mb4');

    $memberName = isset($_POST['memberName']) ? $_POST['memberName'] : '';
    $memberMobileNumber = isset($_POST['memberMobileNumber']) ? $_POST['memberMobileNumber'] : '';
    $memberAltenetMobileNumber = isset($_POST['memberAltenetMobileNumber']) ? $_POST['memberAltenetMobileNumber'] : '';
    $memberAddress = isset($_POST['memberAddress']) ? $_POST['memberAddress'] : '';
    $memberNetworkStatus = isset($_POST['memberNetworkStatus']) ? $_POST['memberNetworkStatus'] : '';
    $memberStatus = isset($_POST['memberStatus']) ? $_POST['memberStatus'] : 'active';
    $userId = isset($_POST['userId']) ? $_POST['userId'] : '';
    $registrationNo = isset($_POST['registrationNo']) ? mess_normalize_registration($_POST['registrationNo']) : '';
    // Default registration no = customer mobile (used on public Mess QR page).
    if ($registrationNo === '') {
        $registrationNo = mess_normalize_registration($memberMobileNumber);
    }
    if ($registrationNo === '') {
        $registrationNo = mess_normalize_mobile($memberMobileNumber);
    }
    $__postedUserId = isset($_POST['userId']) ? $_POST['userId'] : (isset($userId) ? $userId : '');
    pos_require_auth($con, $__postedUserId, isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized'));

    date_default_timezone_set('Asia/Kolkata');

    $check = db_stmt_fetch_one(
        $con,
        'SELECT * FROM mess_member WHERE userId = ? AND member_network_status = ? LIMIT 1',
        'is',
        (int) $userId,
        $memberNetworkStatus
    );

    if ($check !== null) {
        $memberId = (int) $check['id'];
        if ($registrationNo === '') {
            $registrationNo = !empty($check['registration_no'])
                ? mess_normalize_registration($check['registration_no'])
                : mess_normalize_registration($memberMobileNumber);
        }
        if ($registrationNo === '') {
            $registrationNo = mess_normalize_mobile($memberMobileNumber);
        }
        if ($registrationNo === '') {
            $registrationNo = 'REG-' . str_pad((string) $memberId, 4, '0', STR_PAD_LEFT);
        }
        $ok = db_stmt_execute(
            $con,
            'UPDATE mess_member SET member_name = ?, member_mobile_number = ?, member_altenet_mobile_number = ?, member_address = ?, registration_no = ?, member_status = ? WHERE id = ?',
            'ssssssi',
            $memberName,
            $memberMobileNumber,
            $memberAltenetMobileNumber,
            $memberAddress,
            $registrationNo,
            $memberStatus,
            $memberId
        );
        $response['status'] = $ok ? '1' : '0';
        $response['message'] = $ok ? 'update successful!' : 'update failed!';
        $response['registrationNo'] = $registrationNo;
        $response['memberId'] = (string) $memberId;
    } else {
        $ok = db_stmt_execute(
            $con,
            'INSERT INTO mess_member (userId, member_name, member_mobile_number, member_altenet_mobile_number, member_address, registration_no, member_status, member_network_status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            'isssssss',
            (int) $userId,
            $memberName,
            $memberMobileNumber,
            $memberAltenetMobileNumber,
            $memberAddress,
            $registrationNo !== '' ? $registrationNo : null,
            'active',
            $memberNetworkStatus
        );
        if ($ok) {
            $memberId = (int) mysqli_insert_id($con);
            if ($registrationNo === '') {
                $registrationNo = mess_normalize_mobile($memberMobileNumber);
                if ($registrationNo === '') {
                    $registrationNo = 'REG-' . str_pad((string) $memberId, 4, '0', STR_PAD_LEFT);
                }
                db_stmt_execute(
                    $con,
                    'UPDATE mess_member SET registration_no = ? WHERE id = ?',
                    'si',
                    $registrationNo,
                    $memberId
                );
            }
            $response['status'] = '1';
            $response['message'] = 'insert successful!';
            $response['registrationNo'] = $registrationNo;
            $response['memberId'] = (string) $memberId;
        } else {
            $response['status'] = '0';
            $response['message'] = 'insert failed!';
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
