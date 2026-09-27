<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'product.view');

$rows = db_stmt_fetch_all(
    $con,
    'SELECT `clientId`,`productId`,`productName`,`brand`,`sizeLabel`,`colorLabel`,`sku`,`barcode`,
            `fabric`,`stockQty`,`sellingPrice`,`mrp`,`status`,`createdAt`,`updatedAt`
     FROM `pos_product_variants` WHERE `licenseId`=? ORDER BY `productName`,`sizeLabel`,`colorLabel`',
    'i',
    (int) $licenceId
);

$list = array();
foreach ($rows as $row) {
    $list[] = array(
        'id' => $row['clientId'],
        'productId' => (int) $row['productId'],
        'productName' => $row['productName'],
        'brand' => $row['brand'],
        'sizeLabel' => $row['sizeLabel'],
        'colorLabel' => $row['colorLabel'],
        'sku' => $row['sku'],
        'barcode' => $row['barcode'],
        'fabric' => $row['fabric'],
        'stockQty' => (float) $row['stockQty'],
        'sellingPrice' => (float) $row['sellingPrice'],
        'mrp' => (float) $row['mrp'],
        'status' => $row['status'],
        'createdAt' => $row['createdAt'],
        'updatedAt' => $row['updatedAt'],
    );
}

pos_api_json(array('status' => '1', 'variantResponse' => $list));
