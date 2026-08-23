<?php
/**
 * Grant explicit cross-branch read access (Dealer/Admin only).
 * POST: organizationId, sourceBranchId, targetBranchId
 */
include_once('config.php');
include_once('branch_scope.php');
require_once __DIR__ . '/auth_tokens.php';

$response = array('status' => '0', 'message' => 'Invalid request');
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$dealerOk = auth_actor_token_valid_or_legacy($con, 'dealer');
$adminOk = auth_actor_token_valid_or_legacy($con, 'admin');
if (!$dealerOk && !$adminOk) {
    $response['message'] = 'Unauthorized';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$organizationId = isset($_POST['organizationId']) ? (int) $_POST['organizationId'] : 0;
$sourceBranchId = isset($_POST['sourceBranchId']) ? (int) $_POST['sourceBranchId'] : 0;
$targetBranchId = isset($_POST['targetBranchId']) ? (int) $_POST['targetBranchId'] : 0;

if ($organizationId <= 0 || $sourceBranchId <= 0 || $targetBranchId <= 0) {
    $response['message'] = 'Missing organizationId, sourceBranchId, or targetBranchId';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

if ($sourceBranchId === $targetBranchId) {
    $response['status'] = '1';
    $response['message'] = 'Same branch — access implicit';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$scopeSource = branch_scope_from_license($con, $sourceBranchId);
$scopeTarget = branch_scope_from_license($con, $targetBranchId);

if ($scopeSource === null || $scopeTarget === null) {
    $response['message'] = 'Invalid branch id';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

if ((int) $scopeSource['organizationId'] !== $organizationId || (int) $scopeTarget['organizationId'] !== $organizationId) {
    $response['message'] = 'Branches must belong to the same organization';
    header('Content-type: application/json; charset=utf-8');
    echo json_encode($response);
    exit;
}

$insertId = db_stmt_insert_id(
    $con,
    'INSERT INTO `branch_access_grants` (`organization_id`, `source_branch_id`, `target_branch_id`, `granted_by`)
     VALUES (?,?,?,?)
     ON DUPLICATE KEY UPDATE `granted_at`=CURRENT_TIMESTAMP',
    'iiis',
    $organizationId,
    $sourceBranchId,
    $targetBranchId,
    $dealerOk ? 'dealer' : 'admin'
);

if ($insertId !== false) {
    $response['status'] = '1';
    $response['message'] = 'Cross-branch access granted';
    $response['sourceBranchId'] = (string) $sourceBranchId;
    $response['targetBranchId'] = (string) $targetBranchId;
} else {
    $response['message'] = 'Grant failed';
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
