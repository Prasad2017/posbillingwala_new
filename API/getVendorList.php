<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'inventory.view');

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `clientId`,`name`,`gstin`,`contactName`,`mobile`,`email`,`address`,`paymentTerms`,
            `creditLimit`,`openingBalance`,`bankDetails`,`status`,`createdAt`,`updatedAt`
     FROM `pos_vendors` WHERE `licenseId`=? ORDER BY `name` ASC',
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $list[] = array(
        'id' => $row['clientId'],
        'name' => $row['name'],
        'gstin' => $row['gstin'],
        'contactName' => $row['contactName'],
        'mobile' => $row['mobile'],
        'email' => $row['email'],
        'address' => $row['address'],
        'paymentTerms' => $row['paymentTerms'],
        'creditLimit' => (float) $row['creditLimit'],
        'openingBalance' => (float) $row['openingBalance'],
        'bankDetails' => $row['bankDetails'],
        'status' => $row['status'],
        'createdAt' => $row['createdAt'],
        'updatedAt' => $row['updatedAt'],
    );
}

pos_api_json(array('status' => '1', 'vendorResponse' => $list));
