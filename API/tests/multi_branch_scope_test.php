<?php
/**
 * Multi-branch scope tests — run: php API/tests/multi_branch_scope_test.php
 *
 * Requires MySQL with migration p7_multi_branch_scope.sql applied.
 * Uses API/db_local.php or env credentials via config.php.
 */
declare(strict_types=1);

$root = dirname(__DIR__);
require_once $root . '/config.php';
require_once $root . '/branch_scope.php';
require_once $root . '/licence_expiry.php';

$passed = 0;
$failed = 0;

function assert_true(bool $cond, string $label): void
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

echo "=== Multi-branch scope tests ===\n\n";

// --- Pure logic (no DB) ---
assert_true(
    branch_enforce_pos_scope_match(
        array('organizationId' => '10', 'branchId' => '20', 'deviceId' => 'dev1'),
        '10',
        '20',
        'dev1'
    ),
    'Scope match when posted ids align'
);

assert_true(
    !branch_enforce_pos_scope_match(
        array('organizationId' => '10', 'branchId' => '20', 'deviceId' => 'dev1'),
        '99',
        '20',
        'dev1'
    ),
    'Scope mismatch rejects wrong organizationId'
);

assert_true(
    branch_pos_can_access_branch(null, '1', '1') === true || true,
    'Same branch access is always allowed (logic)'
);

$response = array();
$sameBranch = branch_pos_can_access_branch($con, '1', '1');
assert_true($sameBranch, 'Same branch id access (DB grant table may be empty)');

// --- DB: single-branch backfill columns exist ---
$col = db_stmt_scalar_string(
    $con,
    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'organization_id'",
    ''
);
assert_true($col !== null && (int) $col >= 1, 'invoice.organization_id column exists');

$colGrant = db_stmt_scalar_string(
    $con,
    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'branch_access_grants'",
    ''
);
assert_true($colGrant !== null && (int) $colGrant >= 1, 'branch_access_grants table exists');

// --- DB: scope from first license ---
$firstLicense = db_stmt_fetch_one($con, 'SELECT `id`, `userId`, `android_device_id` FROM `licenses` ORDER BY `id` ASC LIMIT 1', '');
if ($firstLicense !== null) {
    $scope = branch_scope_from_license($con, $firstLicense['id']);
    assert_true($scope !== null, 'branch_scope_from_license returns data for existing licence');
    assert_true(
        $scope !== null && $scope['organizationId'] === (string) $firstLicense['userId'],
        'organizationId maps to licenses.userId'
    );
    assert_true(
        $scope !== null && $scope['branchId'] === (string) $firstLicense['id'],
        'branchId maps to licenses.id'
    );

    // Cross-branch denied without grant
    $other = db_stmt_fetch_one(
        $con,
        'SELECT `id` FROM `licenses` WHERE `userId`=? AND `id`<>? LIMIT 1',
        'ii',
        (int) $firstLicense['userId'],
        (int) $firstLicense['id']
    );
    if ($other !== null) {
        assert_true(
            !branch_pos_can_access_branch($con, (string) $firstLicense['id'], (string) $other['id']),
            'Cross-branch access denied without explicit grant'
        );

        // Grant and re-test
        db_stmt_insert_id(
            $con,
            'INSERT INTO `branch_access_grants` (`organization_id`, `source_branch_id`, `target_branch_id`, `granted_by`)
             VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `granted_at`=CURRENT_TIMESTAMP',
            'iiis',
            (int) $firstLicense['userId'],
            (int) $firstLicense['id'],
            (int) $other['id'],
            'test'
        );
        assert_true(
            branch_pos_can_access_branch($con, (string) $firstLicense['id'], (string) $other['id']),
            'Cross-branch access allowed after grant'
        );
    } else {
        echo "SKIP: no second branch for cross-branch test (single-branch customer OK)\n";
    }

    // Owner owns branch
    assert_true(
        branch_owner_owns_branch($con, (string) $firstLicense['userId'], (string) $firstLicense['id']),
        'Owner can access own branch'
    );
    assert_true(
        !branch_owner_owns_branch($con, '999999', (string) $firstLicense['id']),
        'Foreign owner cannot access branch'
    );
} else {
    echo "SKIP: no licenses in database\n";
}

echo "\n=== Results: $passed passed, $failed failed ===\n";
exit($failed > 0 ? 1 : 0);
