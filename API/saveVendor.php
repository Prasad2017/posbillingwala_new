<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
$actor = pos_require_permission($con, $licenceId, 'inventory.manage');
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
$name = pos_api_post('name');
if ($clientId === '' || $name === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId and name are required'));
}

$gstin = pos_api_post('gstin');
$contactName = pos_api_post('contactName');
$mobile = pos_api_post('mobile');
$email = pos_api_post('email');
$address = pos_api_post('address');
$paymentTerms = pos_api_post('paymentTerms', 'Net 30');
$creditLimit = (float) pos_api_post('creditLimit', '0');
$openingBalance = (float) pos_api_post('openingBalance', '0');
$bankDetails = pos_api_post('bankDetails');
$status = strtoupper(pos_api_post('status', 'ACTIVE'));
if ($status !== 'ACTIVE' && $status !== 'INACTIVE') {
    $status = 'ACTIVE';
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_vendors` WHERE `licenseId`=? AND `clientId`=? LIMIT 1',
    'is',
    (int) $licenceId,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_vendors` SET `name`=?, `gstin`=?, `contactName`=?, `mobile`=?, `email`=?,
         `address`=?, `paymentTerms`=?, `creditLimit`=?, `openingBalance`=?, `bankDetails`=?, `status`=?
         WHERE `licenseId`=? AND `clientId`=?',
        'sssssssddssis',
        $name,
        $gstin,
        $contactName,
        $mobile,
        $email,
        $address,
        $paymentTerms,
        $creditLimit,
        $openingBalance,
        $bankDetails,
        $status,
        (int) $licenceId,
        $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_vendors`
         (`organization_id`,`licenseId`,`clientId`,`name`,`gstin`,`contactName`,`mobile`,`email`,`address`,
          `paymentTerms`,`creditLimit`,`openingBalance`,`bankDetails`,`status`)
         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
        'iissssssssddss',
        $orgId,
        (int) $licenceId,
        $clientId,
        $name,
        $gstin,
        $contactName,
        $mobile,
        $email,
        $address,
        $paymentTerms,
        $creditLimit,
        $openingBalance,
        $bankDetails,
        $status
    );
}

pos_audit(
    $con,
    $licenceId,
    $existing ? 'Vendor Updated' : 'Vendor Created',
    'vendor',
    $clientId,
    isset($actor['id']) ? $actor['id'] : 0,
    array('name' => $name)
);

pos_api_json(array('status' => '1', 'message' => 'Vendor saved', 'clientId' => $clientId));
