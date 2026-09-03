<?php
/**
 * Owner: copy HQ or a source-branch catalog onto selected franchise POS licences.
 * POST: userId, sourceMode (hq|branch), sourceBranchId, targetBranchIds (csv)
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
owner_require_auth($con);

require_once __DIR__ . '/../branch_scope.php';
require_once __DIR__ . '/../licence_expiry.php';
require_once __DIR__ . '/../catalog/catalog_push.php';

header('Content-Type: application/json; charset=utf-8');
mysqli_query($con, 'set names utf8');

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$ownerUserId = owner_resolve_user_id($con, isset($_POST['userId']) ? $_POST['userId'] : '');
if ($ownerUserId === null) {
    $response['message'] = 'Unauthorized';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$sourceMode = isset($_POST['sourceMode']) ? trim((string) $_POST['sourceMode']) : 'hq';
$sourceBranchId = isset($_POST['sourceBranchId']) ? trim((string) $_POST['sourceBranchId']) : '';
$targetCsv = isset($_POST['targetBranchIds']) ? trim((string) $_POST['targetBranchIds']) : '';

$source = catalog_push_resolve_source($con, $ownerUserId, $sourceMode, $sourceBranchId);
if ($source === null) {
    $response['message'] = 'Invalid source branch';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$sourceProductCount = catalog_push_count_products($con, $source['userId']);
if ($sourceProductCount < 1) {
    $response['message'] = 'Source catalog has no products to copy';
    $response['sourceLabel'] = $source['label'];
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$targetIds = catalog_push_parse_ids($targetCsv);
if (empty($targetIds)) {
    $response['message'] = 'Select at least one outlet';
    echo json_encode($response);
    mysqli_close($con);
    exit;
}

$results = array();
$okCount = 0;
foreach ($targetIds as $branchId) {
    $branchResult = array(
        'branchId' => (string) $branchId,
        'branchLabel' => '',
        'status' => '0',
        'message' => '',
    );
    if (!branch_owner_owns_branch($con, $ownerUserId, $branchId)) {
        $branchResult['message'] = 'Branch not in your organization';
        $results[] = $branchResult;
        continue;
    }
    $lic = db_stmt_fetch_one($con, 'SELECT `id`, `userId`, `userType`, `userName` FROM `licenses` WHERE `id`=? LIMIT 1', 'i', (int) $branchId);
    $branchResult['branchLabel'] = $lic !== null ? licence_branch_fields($lic)['branchLabel'] : ('Outlet ' . $branchId);

    if ((int) $source['userId'] === (int) $branchId) {
        $branchResult['status'] = '1';
        $branchResult['message'] = 'Skipped (same as source)';
        $results[] = $branchResult;
        continue;
    }

    mysqli_begin_transaction($con);
    try {
        $stats = catalog_push_copy_to_user($con, $source['userId'], (int) $branchId);
        mysqli_commit($con);
        $branchResult['status'] = '1';
        $branchResult['message'] = 'Menu copied';
        $branchResult = array_merge($branchResult, $stats);
        $okCount++;
    } catch (Exception $e) {
        mysqli_rollback($con);
        $branchResult['message'] = 'Copy failed';
    }
    $results[] = $branchResult;
}

$response['status'] = $okCount > 0 ? '1' : '0';
$response['message'] = $okCount > 0
    ? ('Pushed menu to ' . $okCount . ' outlet(s). Sync each POS to download the catalog.')
    : 'No outlets updated';
$response['sourceLabel'] = $source['label'];
$response['sourceProductCount'] = (string) $sourceProductCount;
$response['outletsUpdated'] = (string) $okCount;
$response['branchResults'] = $results;

echo json_encode($response);
mysqli_close($con);
