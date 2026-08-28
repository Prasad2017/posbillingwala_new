<?php
include_once('config.php');
require_once __DIR__ . '/pos_auth_guard.php';
require_once __DIR__ . '/db_prepared.php';

$response = array();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8');
    $licenceId = pos_require_auth($con);
    $invoiceNumber = isset($_POST['invoiceNumber']) ? trim((string) $_POST['invoiceNumber']) : '';
    $networkStatus = isset($_POST['invoiceProductNetworkStatus']) ? trim((string) $_POST['invoiceProductNetworkStatus']) : '';

    if ($invoiceNumber === '' || $networkStatus === '') {
        $response['status'] = '0';
        $response['message'] = 'invoiceNumber and invoiceProductNetworkStatus required';
    } else {
        db_stmt_execute(
            $con,
            'DELETE FROM `invoice_combo_items` WHERE `invoiceProductNetworkStatus`=? AND `invoiceNumber`=?',
            'ss',
            $networkStatus,
            $invoiceNumber
        );
        $existing = db_stmt_fetch_one(
            $con,
            'SELECT p.`invoiceProductId` FROM `invoice_final_product` p
             INNER JOIN `invoice` i ON i.`invoiceNumber` = p.`invoiceNumber` AND i.`licenseId` = ?
             WHERE p.`invoiceProductNetworkStatus` = ? AND p.`invoiceNumber` = ? LIMIT 1',
            'sss',
            (string) $licenceId,
            $networkStatus,
            $invoiceNumber
        );
        if ($existing === null) {
            $response['status'] = '1';
            $response['message'] = 'already deleted';
        } else {
            $deleted = db_stmt_execute(
                $con,
                'DELETE p FROM `invoice_final_product` p
                 INNER JOIN `invoice` i ON i.`invoiceNumber` = p.`invoiceNumber` AND i.`licenseId` = ?
                 WHERE p.`invoiceProductNetworkStatus` = ? AND p.`invoiceNumber` = ?',
                'sss',
                (string) $licenceId,
                $networkStatus,
                $invoiceNumber
            );
            if ($deleted) {
                $response['status'] = '1';
                $response['message'] = 'delete successful!';
            } else {
                $response['status'] = '0';
                $response['message'] = 'delete failed!';
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
