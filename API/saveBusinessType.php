<?php
include_once __DIR__ . '/config.php';
require_once __DIR__ . '/pos_api_boot.php';
require_once __DIR__ . '/enterprise_schema.php';

$licenceId = pos_api_require_licence($con);
enterprise_schema_ensure($con);
pos_require_permission($con, $licenceId, 'inventory.view');

$businessType = pos_api_post('businessType');
if ($businessType === '') {
    pos_api_json(array('status' => '0', 'message' => 'businessType is required'));
}
$allowed = array(
    'retail_store', 'clothing_store', 'supermarket', 'department_store',
    'grocery_store', 'electronics_hardware', 'hotel', 'restaurant', 'cafe',
    'cake_shop_bakery', 'bar', 'cold_drinks_beverage', 'pharmacy', 'footwear',
    'cosmetics', 'home_kitchen', 'other',
);
if (!in_array($businessType, $allowed, true)) {
    pos_api_json(array('status' => '0', 'message' => 'Invalid business type'));
}

$ok = db_stmt_execute(
    $con,
    'UPDATE `licenses` SET `businessType`=? WHERE `id`=?',
    'si',
    $businessType,
    (int) $licenceId
);
if (!$ok) {
    pos_api_json(array('status' => '0', 'message' => 'Unable to save business type'));
}
pos_audit($con, $licenceId, 'BusinessType Updated', 'license', $licenceId, null, array(
    'businessType' => $businessType,
));
pos_api_json(array(
    'status' => '1',
    'message' => 'Business type saved',
    'businessType' => $businessType,
));
