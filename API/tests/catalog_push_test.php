<?php
/**
 * Catalog push helpers — run: php API/tests/catalog_push_test.php
 */
declare(strict_types=1);

$root = dirname(__DIR__);
require_once $root . '/catalog/catalog_push.php';

$passed = 0;
$failed = 0;

function assert_true($cond, $label)
{
    global $passed, $failed;
    if ($cond) {
        echo "PASS: $label\n";
        $passed++;
    } else {
        echo "FAIL: $label\n";
        $failed++;
    }
}

echo "=== Catalog push helper tests ===\n\n";

assert_true(catalog_push_norm_name('  Veg ') === 'veg', 'Normalize category name');
assert_true(catalog_push_parse_ids('1, 2,2, 3') === array(1, 2, 3), 'Parse unique positive ids');
assert_true(catalog_push_parse_ids('') === array(), 'Empty csv is empty list');
assert_true(
    catalog_push_product_key(array('productCode' => 'P01', 'productName' => 'Tea')) === 'c:p01',
    'Match products by code when present'
);
assert_true(
    catalog_push_product_key(array('productCode' => '', 'productName' => 'Tea')) === 'n:tea',
    'Match products by name when code empty'
);

$id = catalog_push_network_id('prd', 12);
assert_true(strpos($id, 'prd12_') === 0 && strlen($id) > 10, 'Network id is unique per target');

echo "\nPassed: $passed  Failed: $failed\n";
exit($failed > 0 ? 1 : 0);
