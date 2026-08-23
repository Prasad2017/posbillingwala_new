<?php
include_once('config.php');

$response = array('messTokenResponse' => array());
mysqli_query($con, 'set names utf8mb4');

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $userId = isset($_GET['userId']) ? trim($_GET['userId']) : '';

    if ($userId !== '') {
        $stmt = mysqli_prepare(
            $con,
            'SELECT tokenId, tokenCode, memberId, memberName, memberMobile, memberType, messType,
                    tokenAmount, tokenDate, verifiedDate, tokenNetworkStatus, tokenStatus, verifyNetworkStatus
             FROM mess_token
             WHERE userId = ?
             ORDER BY tokenId DESC'
        );

        if ($stmt) {
            mysqli_stmt_bind_param($stmt, 'i', $userId);
            if (mysqli_stmt_execute($stmt)) {
                $result = mysqli_stmt_get_result($stmt);
                while ($row = mysqli_fetch_assoc($result)) {
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
                        'tokenState' => $row['tokenStatus'],
                        'verifyNetworkStatus' => $row['verifyNetworkStatus']
                    );
                }
            }
            mysqli_stmt_close($stmt);
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
