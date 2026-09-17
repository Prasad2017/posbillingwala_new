<?php
/**
 * P29 permission / PIN tests — php API/tests/pos_permissions_test.php
 */
declare(strict_types=1);

$root = dirname(__DIR__);
require_once $root . '/pos_permissions.php';
require_once $root . '/pos_staff.php';

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

echo "=== P29 permissions / PIN ===\n\n";

$roles = pos_fixed_roles();
assert_true(count($roles) === 8, 'Exactly 8 fixed roles');
assert_true(isset($roles['BAR_ATTENDER']), 'Bar Attender role exists');
assert_true(!isset($roles['CASHIER']), 'No custom cashier role');

$waiter = pos_role_default_map()['WAITER'];
assert_true(!empty($waiter['kot.print']), 'Waiter default KOT print allowed');
assert_true(empty($waiter['bill.create']), 'Waiter default bill create denied');
assert_true(empty($waiter['printer.manage']), 'Waiter cannot manage printers');

$owner = pos_role_default_map()['OWNER'];
assert_true(!empty($owner['user.create']) && !empty($owner['settings.manage']), 'Owner has full access');

$acc = pos_role_default_map()['ACCOUNTANT'];
assert_true(!empty($acc['report.view']) && empty($acc['product.create']), 'Accountant reports yes, products no');

$overrides = pos_normalize_overrides(array('kot.print' => 'DENY', 'bogus' => 'ALLOW'));
assert_true(isset($overrides['kot.print']) && $overrides['kot.print'] === 'DENY', 'DENY override kept');
assert_true(!isset($overrides['bogus']), 'Unknown permission dropped');

$withParent = pos_normalize_overrides(array('bill.print' => 'ALLOW'));
assert_true(isset($withParent['bill.view']) && $withParent['bill.view'] === 'ALLOW', 'bill.print implies bill.view');

assert_true(pos_pin_valid_format('1234') && pos_pin_valid_format('123456'), 'PIN 4 or 6 digits ok');
assert_true(!pos_pin_valid_format('12') && !pos_pin_valid_format('abcd'), 'Invalid PIN rejected');
assert_true(pos_mobile_valid('9876543210') && !pos_mobile_valid('12345'), 'India 10-digit mobile');

$hash = pos_pin_hash('9082');
assert_true(strpos($hash, '$2') === 0, 'PIN hashed with bcrypt');
assert_true(pos_pin_verify('9082', $hash) && !pos_pin_verify('0000', $hash), 'PIN verify works');

$effectiveWaiter = $waiter;
$effectiveWaiter['kot.print'] = 0; // DENY override
assert_true($effectiveWaiter['kot.print'] === 0 && $waiter['kot.print'] === 1, 'User override DENY wins over role default');

$kitchen = pos_role_default_map()['KITCHEN'];
assert_true(!empty($kitchen['kot.print']) && empty($kitchen['bill.create']), 'Kitchen KOT yes, bill no');

$helper = pos_role_default_map()['HELPER'];
assert_true(empty($helper['kot.print']) && empty($helper['bill.create']), 'Helper cannot print KOT or create bill');

$bar = pos_role_default_map()['BAR_ATTENDER'];
assert_true(!empty($bar['kot.create']) && empty($bar['user.create']), 'Bar attender can KOT, cannot manage users');

$security = pos_role_default_map()['SECURITY'];
assert_true(!empty($security['table.view']) && empty($security['order.create']), 'Security table view only');

$public = pos_staff_public(array(
    'id' => 1,
    'licenseId' => 9,
    'name' => 'Test',
    'mobileNumber' => '9876543210',
    'address' => '',
    'profileImage' => '',
    'role' => 'WAITER',
    'pinHash' => '$2y$10$secret',
    'status' => 'ACTIVE',
    'lastLoginAt' => '',
));
assert_true(!isset($public['pinHash']) && !isset($public['appLoginPin']), 'Staff payload never includes PIN hash');

require_once $root . '/log_sanitizer.php';
$redacted = log_sanitize_text('appLoginPin=1234&pinHash=$2y$10$abc&name=Ram');
assert_true(strpos($redacted, '1234') === false && strpos($redacted, '$2y') === false, 'Logs redact PIN fields');

$sql = file_get_contents($root . '/migrations/p29_multi_user_printers.sql');
assert_true(strpos($sql, 'userManagementEnabled') !== false
    && strpos($sql, 'NOT NULL DEFAULT 0') !== false, 'UM defaults off in migration');
assert_true(strpos($sql, 'uk_print_job_idem') !== false, 'Print jobs idempotent unique key');
assert_true(strpos($sql, 'CREATE TABLE IF NOT EXISTS `pos_staff`') !== false, 'pos_staff table in migration');
assert_true(strpos($sql, 'CREATE TABLE IF NOT EXISTS `store_printers`') !== false, 'store_printers separate from devices');
assert_true(strpos($sql, '`paperSize`') !== false && strpos($sql, '`usbIdentifier`') !== false, 'store_printers paperSize and USB');
$p30 = file_get_contents($root . '/migrations/p30_printer_connection.sql');
assert_true($p30 !== false && strpos($p30, 'billConnectionType') !== false && strpos($p30, 'paperSize') !== false && strpos($p30, 'kotPaperSize') !== false, 'P30 Bluetooth/USB paper size columns');
$schemaPhp = file_get_contents($root . '/pos_schema.php');
assert_true(strpos($schemaPhp, "pos_schema_add_column(\$con, 'company_printer_setting', 'paperSize'") !== false, 'Runtime schema adds company paperSize');
assert_true(strpos($schemaPhp, "pos_schema_add_column(\$con, 'company_printer_setting', 'kotPaperSize'") !== false, 'Runtime schema adds company kotPaperSize');
assert_true(function_exists('pos_normalize_paper_size') && pos_normalize_paper_size('3 inch') === '3-Inch' && pos_normalize_paper_size('') === '2-Inch', 'paperSize normalized to 2-Inch/3-Inch');
assert_true(pos_normalize_connection_type('usb') === 'USB' && pos_normalize_connection_type('bt') === 'BLUETOOTH', 'connection type Bluetooth/USB');
assert_true(strpos($sql, 'CREATE TABLE IF NOT EXISTS `pos_devices`') !== false, 'pos_devices table exists');

require_once $root . '/pos_ops_panel.php';
assert_true(function_exists('pos_ops_panel_payload'), 'pos_ops_panel_payload exists');
assert_true(function_exists('pos_device_public'), 'Device public DTO exists');
$dev = pos_device_public(array(
    'id' => 1,
    'deviceId' => 'abc',
    'deviceName' => 'Counter',
    'platform' => 'ANDROID',
    'appVersion' => '2',
    'status' => 'ACTIVE',
    'isPrintHost' => '1',
    'lastSeenAt' => '',
));
assert_true($dev['deviceName'] === 'Counter' && !isset($dev['pinHash']), 'Device DTO has no secrets');

echo "\nPassed: $passed  Failed: $failed\n";
exit($failed > 0 ? 1 : 0);
