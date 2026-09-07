<?php
/**
 * Dealer: read business template for a licence belonging to this dealer's customer.
 */
include_once('config.php');
require_once __DIR__ . '/auth_guard.php';
require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../business_template_ops.php';

dealer_require_auth($con);

$response = array('status' => '1', 'message' => 'ok', 'businessTemplateResponse' => array());

if ($_SERVER['REQUEST_METHOD'] == 'GET') {
    mysqli_query($con, 'set names utf8mb4');

    $actor = auth_resolve_actor_from_request($con);
    $dealerId = ($actor !== null && $actor['actor_type'] === 'dealer')
        ? (int) $actor['actor_id'] : 0;
    if ($dealerId <= 0 && isset($_GET['userId'])) {
        $dealerId = (int) $_GET['userId'];
    }

    $licenceId = isset($_GET['licenceId']) ? trim($_GET['licenceId']) : '';
    if ($licenceId === '') {
        $response['status'] = '0';
        $response['message'] = 'Missing licenceId';
    } elseif ($dealerId <= 0) {
        $response['status'] = '0';
        $response['message'] = 'Unauthorized';
    } else {
        $lic = business_template_dealer_owns_licence($con, $licenceId, $dealerId);
        if ($lic === null) {
            $response['status'] = '0';
            $response['message'] = 'Licence not found or not yours';
        } else {
            $response['businessTemplateResponse'][] = business_template_fetch($con, $licenceId);
        }
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
