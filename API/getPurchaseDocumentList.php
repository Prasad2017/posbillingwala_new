<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'inventory.view');

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `clientId`,`docNo`,`docType`,`docStatus`,`vendorClientId`,`vendorName`,`notes`,`referenceNo`,
            `parentClientId`,`createdBy`,`linesJson`,`subTotal`,`receivedAt`,`createdAt`,`updatedAt`
     FROM `pos_purchase_documents` WHERE `licenseId`=? ORDER BY `updatedAt` DESC',
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $lines = json_decode($row['linesJson'], true);
    if (!is_array($lines)) {
        $lines = array();
    }
    $list[] = array(
        'id' => $row['clientId'],
        'docNo' => $row['docNo'],
        'type' => $row['docType'],
        'status' => $row['docStatus'],
        'vendorId' => $row['vendorClientId'],
        'vendorName' => $row['vendorName'],
        'notes' => $row['notes'],
        'referenceNo' => $row['referenceNo'],
        'parentDocId' => $row['parentClientId'],
        'createdBy' => $row['createdBy'],
        'lines' => $lines,
        'subTotal' => (float) $row['subTotal'],
        'receivedAt' => $row['receivedAt'],
        'createdAt' => $row['createdAt'],
        'updatedAt' => $row['updatedAt'],
    );
}

pos_api_json(array('status' => '1', 'purchaseResponse' => $list));
