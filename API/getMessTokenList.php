<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';

$response = array(
    'status' => '1',
    'message' => 'ok',
    'messTokenResponse' => array(),
);
mysqli_query($con, 'set names utf8mb4');

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $__postedUserId = isset($_GET['userId']) ? trim($_GET['userId']) : '';
    $userId = pos_require_auth(
        $con,
        $__postedUserId,
        isset($response) ? $response : array('status' => '0', 'message' => 'Unauthorized')
    );

    if ($userId !== '') {
        $uid = (int) $userId;
        $stmt = mysqli_prepare(
            $con,
            'SELECT tokenId, tokenCode, memberId, memberName, memberMobile, memberType, messType,
                    tokenAmount, tokenDate, verifiedDate, tokenNetworkStatus, tokenStatus, verifyNetworkStatus
             FROM mess_token
             WHERE userId = ?
             ORDER BY tokenId DESC'
        );

        if ($stmt) {
            mysqli_stmt_bind_param($stmt, 'i', $uid);
            if (mysqli_stmt_execute($stmt)) {
                $result = mysqli_stmt_get_result($stmt);
                while ($row = mysqli_fetch_assoc($result)) {
                    $lifecycle = isset($row['tokenStatus']) ? (string) $row['tokenStatus'] : 'active';
                    /* Flutter: tokenState=lifecycle, tokenStatus=sync (1=synced). */
                    $response['messTokenResponse'][] = array(
                        'tokenId' => $row['tokenId'],
                        'tokenCode' => $row['tokenCode'],
                        'memberId' => $row['memberId'],
                        'memberName' => $row['memberName'],
                        'memberMobile' => $row['memberMobile'],
                        'memberType' => $row['memberType'],
                        'messType' => $row['messType'],
                        'tokenAmount' => $row['tokenAmount'],
                        'tokenDate' => $row['tokenDate'],
                        'verifiedDate' => $row['verifiedDate'],
                        'tokenNetworkStatus' => $row['tokenNetworkStatus'],
                        'tokenState' => $lifecycle,
                        'tokenStatus' => '1',
                        'verifyNetworkStatus' => $row['verifyNetworkStatus'],
                    );
                }
            }
            mysqli_stmt_close($stmt);
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
