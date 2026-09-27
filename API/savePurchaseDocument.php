<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
$actor = pos_require_permission($con, $licenceId, 'inventory.manage');
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
$docNo = pos_api_post('docNo');
$docType = strtolower(pos_api_post('docType', 'order'));
$docStatus = strtolower(pos_api_post('docStatus', 'draft'));
$vendorClientId = pos_api_post('vendorId');
$vendorName = pos_api_post('vendorName');
$notes = pos_api_post('notes');
$referenceNo = pos_api_post('referenceNo');
$parentClientId = pos_api_post('parentDocId');
$createdBy = pos_api_post('createdBy');
$linesJson = pos_api_post('linesJson', '[]');
$subTotal = (float) pos_api_post('subTotal', '0');
$receivedAt = pos_api_post('receivedAt');
if ($receivedAt === '') {
    $receivedAt = null;
}

if ($clientId === '' || $docNo === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId and docNo are required'));
}
if (json_decode($linesJson) === null) {
    pos_api_json(array('status' => '0', 'message' => 'linesJson must be valid JSON'));
}

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_purchase_documents` WHERE `licenseId`=? AND `clientId`=? LIMIT 1',
    'is',
    (int) $licenceId,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_purchase_documents` SET `docNo`=?, `docType`=?, `docStatus`=?, `vendorClientId`=?,
         `vendorName`=?, `notes`=?, `referenceNo`=?, `parentClientId`=?, `createdBy`=?, `linesJson`=?,
         `subTotal`=?, `receivedAt`=? WHERE `licenseId`=? AND `clientId`=?',
        'ssssssssssdsis',
        $docNo,
        $docType,
        $docStatus,
        $vendorClientId,
        $vendorName,
        $notes,
        $referenceNo,
        $parentClientId,
        $createdBy,
        $linesJson,
        $subTotal,
        $receivedAt === '' ? null : $receivedAt,
        (int) $licenceId,
        $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_purchase_documents`
         (`organization_id`,`licenseId`,`clientId`,`docNo`,`docType`,`docStatus`,`vendorClientId`,`vendorName`,
          `notes`,`referenceNo`,`parentClientId`,`createdBy`,`linesJson`,`subTotal`,`receivedAt`)
         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
        'iisssssssssssds',
        $orgId,
        (int) $licenceId,
        $clientId,
        $docNo,
        $docType,
        $docStatus,
        $vendorClientId,
        $vendorName,
        $notes,
        $referenceNo,
        $parentClientId,
        $createdBy,
        $linesJson,
        $subTotal,
        $receivedAt === '' ? null : $receivedAt
    );
}

pos_audit(
    $con,
    $licenceId,
    'PurchaseDocument Saved',
    'purchase',
    $clientId,
    isset($actor['id']) ? $actor['id'] : 0,
    array('docNo' => $docNo, 'type' => $docType, 'status' => $docStatus)
);

pos_api_json(array('status' => '1', 'message' => 'Purchase document saved', 'clientId' => $clientId));
