<?php
include_once('config.php');
include_once('db_prepared.php');

$response = array('status' => '0', 'message' => 'Invalid request');
mysqli_query($con, 'set names utf8mb4');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $userId = isset($_POST['userId']) ? trim($_POST['userId']) : '';
    $tokenCode = isset($_POST['tokenCode']) ? trim($_POST['tokenCode']) : '';
    $memberId = isset($_POST['memberId']) ? trim($_POST['memberId']) : '';
    $memberName = isset($_POST['memberName']) ? trim($_POST['memberName']) : '';
    $memberMobile = isset($_POST['memberMobile']) ? trim($_POST['memberMobile']) : '';
    $memberType = isset($_POST['memberType']) ? trim($_POST['memberType']) : 'walk_in';
    $messType = isset($_POST['messType']) ? trim($_POST['messType']) : '';
    $tokenAmount = isset($_POST['tokenAmount']) ? trim($_POST['tokenAmount']) : '0';
    $tokenDate = isset($_POST['tokenDate']) ? trim($_POST['tokenDate']) : '';
    $tokenNetworkStatus = isset($_POST['tokenNetworkStatus']) ? trim($_POST['tokenNetworkStatus']) : '';

    if ($userId === '' || $tokenCode === '' || $memberName === '' || $messType === '' || $tokenDate === '' || $tokenNetworkStatus === '') {
        $response['message'] = 'Missing required fields';
    } else {
        date_default_timezone_set('Asia/Kolkata');

        $existing = db_stmt_fetch_one(
            $con,
            'SELECT tokenId FROM mess_token WHERE tokenNetworkStatus = ? LIMIT 1',
            's',
            $tokenNetworkStatus
        );

        if ($existing !== null) {
            $response['status'] = '1';
            $response['message'] = 'already synced';
            $response['tokenId'] = $existing['tokenId'];
        } else {
            $stmt = mysqli_prepare(
                $con,
                'INSERT INTO mess_token
                (userId, tokenCode, memberId, memberName, memberMobile, memberType, messType, tokenAmount, tokenDate, tokenNetworkStatus, tokenStatus, syncStatus)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
            );

            if ($stmt) {
                $tokenStatus = 'active';
                $syncStatus = '1';
                db_stmt_bind_params(
                    $stmt,
                    'ssssssssssss',
                    array(
                        $userId,
                        $tokenCode,
                        $memberId,
                        $memberName,
                        $memberMobile,
                        $memberType,
                        $messType,
                        $tokenAmount,
                        $tokenDate,
                        $tokenNetworkStatus,
                        $tokenStatus,
                        $syncStatus
                    )
                );

                if (mysqli_stmt_execute($stmt)) {
                    $response['status'] = '1';
                    $response['message'] = 'insert successful';
                    $response['tokenId'] = mysqli_insert_id($con);
                } else {
                    $response['message'] = 'insert failed';
                }
                mysqli_stmt_close($stmt);
            } else {
                $response['message'] = 'prepare failed';
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
