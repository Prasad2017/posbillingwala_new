<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'product.view');

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `clientId`,`name`,`mobile`,`email`,`address`,`gstin`,`creditLimit`,`walletBalance`,
            `loyaltyPoints`,`membership`,`birthday`,`anniversary`,`totalPurchase`,`lastPurchaseAt`,
            `status`,`notes`,`createdAt`,`updatedAt`
     FROM `pos_customers` WHERE `licenseId`=? ORDER BY `name` ASC',
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $list[] = array(
        'id' => $row['clientId'],
        'name' => $row['name'],
        'mobile' => $row['mobile'],
        'email' => $row['email'],
        'address' => $row['address'],
        'gstin' => $row['gstin'],
        'creditLimit' => (float) $row['creditLimit'],
        'walletBalance' => (float) $row['walletBalance'],
        'loyaltyPoints' => (float) $row['loyaltyPoints'],
        'membership' => $row['membership'],
        'birthday' => $row['birthday'],
        'anniversary' => $row['anniversary'],
        'totalPurchase' => (float) $row['totalPurchase'],
        'lastPurchaseAt' => $row['lastPurchaseAt'],
        'status' => $row['status'],
        'notes' => $row['notes'],
        'createdAt' => $row['createdAt'],
        'updatedAt' => $row['updatedAt'],
    );
}

pos_api_json(array('status' => '1', 'customerResponse' => $list));
