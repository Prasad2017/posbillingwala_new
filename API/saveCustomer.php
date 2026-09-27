<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
$actor = pos_require_permission($con, $licenceId, 'product.view');
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
$name = pos_api_post('name');
if ($clientId === '' || $name === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId and name are required'));
}

$mobile = pos_api_post('mobile');
$email = pos_api_post('email');
$address = pos_api_post('address');
$gstin = pos_api_post('gstin');
$creditLimit = (float) pos_api_post('creditLimit', '0');
$walletBalance = (float) pos_api_post('walletBalance', '0');
$loyaltyPoints = (float) pos_api_post('loyaltyPoints', '0');
$membership = pos_api_post('membership');
$birthday = pos_api_post('birthday');
$anniversary = pos_api_post('anniversary');
$totalPurchase = (float) pos_api_post('totalPurchase', '0');
$lastPurchaseAt = pos_api_post('lastPurchaseAt');
$status = strtoupper(pos_api_post('status', 'ACTIVE'));
$notes = pos_api_post('notes');

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_customers` WHERE `licenseId`=? AND `clientId`=? LIMIT 1',
    'is',
    (int) $licenceId,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_customers` SET `name`=?, `mobile`=?, `email`=?, `address`=?, `gstin`=?,
         `creditLimit`=?, `walletBalance`=?, `loyaltyPoints`=?, `membership`=?, `birthday`=?,
         `anniversary`=?, `totalPurchase`=?, `lastPurchaseAt`=?, `status`=?, `notes`=?
         WHERE `licenseId`=? AND `clientId`=?',
        'sssssdddsssdsssiss',
        $name, $mobile, $email, $address, $gstin,
        $creditLimit, $walletBalance, $loyaltyPoints, $membership,
        $birthday === '' ? null : $birthday,
        $anniversary === '' ? null : $anniversary,
        $totalPurchase,
        $lastPurchaseAt === '' ? null : $lastPurchaseAt,
        $status, $notes,
        (int) $licenceId, $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_customers`
         (`organization_id`,`licenseId`,`clientId`,`name`,`mobile`,`email`,`address`,`gstin`,
          `creditLimit`,`walletBalance`,`loyaltyPoints`,`membership`,`birthday`,`anniversary`,
          `totalPurchase`,`lastPurchaseAt`,`status`,`notes`)
         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
        'iissssssdddsssdsdss',
        $orgId, (int) $licenceId, $clientId, $name, $mobile, $email, $address, $gstin,
        $creditLimit, $walletBalance, $loyaltyPoints, $membership,
        $birthday === '' ? null : $birthday,
        $anniversary === '' ? null : $anniversary,
        $totalPurchase,
        $lastPurchaseAt === '' ? null : $lastPurchaseAt,
        $status, $notes
    );
}

pos_audit($con, $licenceId, 'Customer Saved', 'customer', $clientId, isset($actor['id']) ? $actor['id'] : 0, array('name' => $name));
pos_api_json(array('status' => '1', 'message' => 'Customer saved', 'clientId' => $clientId));
