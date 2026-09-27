<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
$actor = pos_require_permission($con, $licenceId, 'product.view');
$orgId = enterprise_org_id($con, $licenceId);

$clientId = pos_api_post('clientId');
if ($clientId === '') {
    pos_api_json(array('status' => '0', 'message' => 'clientId is required'));
}

$productId = (int) pos_api_post('productId', '0');
$productName = pos_api_post('productName');
$brand = pos_api_post('brand');
$sizeLabel = pos_api_post('sizeLabel');
$colorLabel = pos_api_post('colorLabel');
$sku = pos_api_post('sku');
$barcode = pos_api_post('barcode');
$fabric = pos_api_post('fabric');
$stockQty = (float) pos_api_post('stockQty', '0');
$sellingPrice = (float) pos_api_post('sellingPrice', '0');
$mrp = (float) pos_api_post('mrp', '0');
$status = strtoupper(pos_api_post('status', 'ACTIVE'));

$existing = db_stmt_fetch_one(
    $con,
    'SELECT `id` FROM `pos_product_variants` WHERE `licenseId`=? AND `clientId`=? LIMIT 1',
    'is',
    (int) $licenceId,
    $clientId
);

if ($existing !== null) {
    db_stmt_execute(
        $con,
        'UPDATE `pos_product_variants` SET `productId`=?, `productName`=?, `brand`=?, `sizeLabel`=?,
         `colorLabel`=?, `sku`=?, `barcode`=?, `fabric`=?, `stockQty`=?, `sellingPrice`=?, `mrp`=?,
         `status`=? WHERE `licenseId`=? AND `clientId`=?',
        'isssssssdddsis',
        $productId, $productName, $brand, $sizeLabel, $colorLabel, $sku, $barcode, $fabric,
        $stockQty, $sellingPrice, $mrp, $status, (int) $licenceId, $clientId
    );
} else {
    db_stmt_execute(
        $con,
        'INSERT INTO `pos_product_variants`
         (`organization_id`,`licenseId`,`clientId`,`productId`,`productName`,`brand`,`sizeLabel`,
          `colorLabel`,`sku`,`barcode`,`fabric`,`stockQty`,`sellingPrice`,`mrp`,`status`)
         VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
        'iisisssssssddds',
        $orgId, (int) $licenceId, $clientId, $productId, $productName, $brand, $sizeLabel,
        $colorLabel, $sku, $barcode, $fabric, $stockQty, $sellingPrice, $mrp, $status
    );
}

pos_audit($con, $licenceId, 'ProductVariant Saved', 'variant', $clientId, isset($actor['id']) ? $actor['id'] : 0, array(
    'sku' => $sku,
    'size' => $sizeLabel,
    'color' => $colorLabel,
));
pos_api_json(array('status' => '1', 'message' => 'Variant saved', 'clientId' => $clientId));
