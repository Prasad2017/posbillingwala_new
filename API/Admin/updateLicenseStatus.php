<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../db_prepared.php';
require_once __DIR__ . '/auth_guard.php';

/**
 * Suspend or reactivate a licence without rotating the key or clearing device bind.
 * POST: licensesId, action = suspend | reactivate
 */
$response = array('status' => '0', 'message' => 'update failed...');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    admin_require_auth($con);

    mysqli_query($con, 'set names utf8');

    $licensesId = isset($_POST['licensesId']) ? trim($_POST['licensesId']) : '';
    $action = isset($_POST['action']) ? strtolower(trim($_POST['action'])) : '';

    if ($licensesId === '' || ($action !== 'suspend' && $action !== 'reactivate')) {
        $response['message'] = 'licensesId and action (suspend|reactivate) are required.';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    $row = db_stmt_fetch_one(
        $con,
        'SELECT id, licenseKey, licenseStatus, licenseType, expiryDate FROM `licenses` WHERE `id`=? LIMIT 1',
        'i',
        (int) $licensesId
    );

    if ($row === null) {
        $response['message'] = 'Licence not found';
        header('Content-type: application/json; charset=utf-8');
        echo json_encode($response);
        exit;
    }

    if ($action === 'suspend') {
        $ok = db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `licenseStatus`=\'suspended\' WHERE `id`=?',
            'i',
            (int) $licensesId
        );
        if ($ok) {
            $response['status'] = '1';
            $response['message'] = 'Licence suspended.';
            $response['licenseKey'] = $row['licenseKey'];
            $response['licenseStatus'] = 'suspended';
        }
    } else {
        // Reactivate: restore active/expire based on expiry date (never rotate key)
        $expiryDate = isset($row['expiryDate']) ? $row['expiryDate'] : '';
        $newStatus = 'active';
        if (function_exists('licence_is_date_valid') && $expiryDate !== '' && !licence_is_date_valid($expiryDate)) {
            $newStatus = 'expire';
        }
        $ok = db_stmt_execute(
            $con,
            'UPDATE `licenses` SET `licenseStatus`=? WHERE `id`=?',
            'si',
            $newStatus,
            (int) $licensesId
        );
        if ($ok) {
            $response['status'] = '1';
            $response['message'] = 'Licence reactivated.';
            $response['licenseKey'] = $row['licenseKey'];
            $response['licenseStatus'] = $newStatus;
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
