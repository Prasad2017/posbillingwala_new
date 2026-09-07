<?php
/**
 * Dealer: set business template for a licence belonging to this dealer's customer.
 * Optional syncModules=1 updates Fast/Dine/Takeaway/Mess on the licence row.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../business_template_ops.php';

dealer_require_auth($con);

$response = array('status' => '0', 'message' => 'Invalid request');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    mysqli_query($con, 'set names utf8mb4');

    $actor = auth_resolve_actor_from_request($con);
    $dealerId = ($actor !== null && $actor['actor_type'] === 'dealer')
        ? (int) $actor['actor_id'] : 0;
    if ($dealerId <= 0 && isset($_POST['userId'])) {
        $dealerId = (int) $_POST['userId'];
    }

    $licenceId = isset($_POST['licenceId']) ? trim($_POST['licenceId']) : '';
    $businessType = isset($_POST['businessType']) ? trim($_POST['businessType']) : 'restaurant';
    $businessTemplateId = isset($_POST['businessTemplateId'])
        ? trim($_POST['businessTemplateId']) : 'restaurant_default';
    $businessTemplateJson = isset($_POST['businessTemplateJson'])
        ? trim($_POST['businessTemplateJson']) : '';
    $syncModules = !isset($_POST['syncModules']) || (string) $_POST['syncModules'] === '1';

    if ($licenceId === '') {
        $response['message'] = 'Missing licenceId';
    } elseif ($dealerId <= 0) {
        $response['message'] = 'Unauthorized';
    } else {
        $lic = business_template_dealer_owns_licence($con, $licenceId, $dealerId);
        if ($lic === null) {
            $response['message'] = 'Licence not found or not yours';
        } else {
            if ($businessType === '') {
                $businessType = 'restaurant';
            }
            if ($businessTemplateId === '') {
                $businessTemplateId = 'restaurant_default';
            }
            $orgId = (int) $lic['userId'];
            $branchId = (int) $lic['id'];
            $response = business_template_upsert(
                $con,
                $licenceId,
                $orgId,
                $branchId,
                $businessType,
                $businessTemplateId,
                $businessTemplateJson,
                'dealer_push'
            );
            if ($response['status'] === '1' && $syncModules) {
                $okMods = business_template_sync_licence_modules(
                    $con,
                    $licenceId,
                    $businessType,
                    $businessTemplateId
                );
                $response['modulesSynced'] = $okMods ? '1' : '0';
            } else {
                $response['modulesSynced'] = '0';
            }
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
