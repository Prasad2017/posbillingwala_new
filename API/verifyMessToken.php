<?php
include_once('config.php');
include_once('db_prepared.php');

$response = array('status' => '0', 'message' => 'Invalid request');
mysqli_query($con, 'set names utf8mb4');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $userId = isset($_POST['userId']) ? trim($_POST['userId']) : '';
    $tokenCode = isset($_POST['tokenCode']) ? trim($_POST['tokenCode']) : '';
    $verifiedDate = isset($_POST['verifiedDate']) ? trim($_POST['verifiedDate']) : '';
    $verifyNetworkStatus = isset($_POST['verifyNetworkStatus']) ? trim($_POST['verifyNetworkStatus']) : '';

    if ($userId === '' || $tokenCode === '' || $verifiedDate === '' || $verifyNetworkStatus === '') {
        $response['message'] = 'Missing required fields';
    } else {
        date_default_timezone_set('Asia/Kolkata');

        $token = db_stmt_fetch_one(
            $con,
            'SELECT tokenId, tokenStatus, tokenDate, memberName, messType, memberType
             FROM mess_token
             WHERE userId = ? AND tokenCode = ?
             LIMIT 1',
            'ss',
            $userId,
            $tokenCode
        );

        if ($token === null) {
            $response['message'] = 'Token not found';
        } elseif (isset($token['memberType']) && $token['memberType'] !== 'member') {
            $response['status'] = '0';
            $response['message'] = 'Only registered member tokens are allowed';
        } elseif ($token['tokenStatus'] === 'verified') {
            $response['status'] = '0';
            $response['message'] = 'Token already verified';
            $response['memberName'] = $token['memberName'];
            $response['messType'] = $token['messType'];
            $response['memberType'] = $token['memberType'];
        } else {
            $tokenDay = substr($token['tokenDate'], 0, 10);
            $today = date('Y-m-d');
            if ($tokenDay !== $today) {
                $response['message'] = 'Token expired (not valid for today)';
            } else {
                $stmt = mysqli_prepare(
                    $con,
                    'UPDATE mess_token
                     SET tokenStatus = ?, verifiedDate = ?, verifyNetworkStatus = ?
                     WHERE tokenId = ? AND tokenStatus = ?'
                );

                if ($stmt) {
                    $verifiedStatus = 'verified';
                    $activeStatus = 'active';
                    db_stmt_bind_params(
                        $stmt,
                        'sssis',
                        array(
                            $verifiedStatus,
                            $verifiedDate,
                            $verifyNetworkStatus,
                            $token['tokenId'],
                            $activeStatus
                        )
                    );

                    if (mysqli_stmt_execute($stmt) && mysqli_stmt_affected_rows($stmt) > 0) {
                        $response['status'] = '1';
                        $response['message'] = 'Token verified';
                        $response['memberName'] = $token['memberName'];
                        $response['messType'] = $token['messType'];
                        $response['memberType'] = $token['memberType'];
                    } else {
                        $response['message'] = 'Verify update failed';
                    }
                    mysqli_stmt_close($stmt);
                } else {
                    $response['message'] = 'prepare failed';
                }
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
