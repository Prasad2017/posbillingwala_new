<?php

require_once __DIR__ . '/../auth_tokens.php';
require_once __DIR__ . '/../db_prepared.php';

/**
 * Resolve authenticated actor from Bearer token.
 *
 * @return array|null actor_type, actor_id
 */
function catalog_resolve_actor($con)
{
    return auth_resolve_actor_from_request($con);
}

/**
 * Require actor of expected type; returns actor row or exits with JSON error.
 *
 * @param mysqli $con
 * @param string $expectedType admin|dealer|owner
 * @return array
 */
function catalog_require_actor($con, $expectedType)
{
    $actor = catalog_resolve_actor($con);
    if ($actor === null || $actor['actor_type'] !== $expectedType) {
        catalog_json_response(array(
            'success' => false,
            'status' => 'false',
            'message' => 'Unauthorized',
        ), 401);
        exit;
    }
    return $actor;
}

/**
 * Validate customer exists and actor may manage their catalog.
 *
 * @return array|null customer user row
 */
function catalog_authorize_customer($con, $actorType, $actorId, $customerId)
{
    $customerId = (int) $customerId;
    if ($customerId <= 0) {
        return null;
    }

    $customer = db_stmt_fetch_one(
        $con,
        'SELECT `id`, `name`, `dealerId`, `role_id`, `is_active` FROM `users` WHERE `id`=? LIMIT 1',
        'i',
        $customerId
    );

    if ($customer === null || (int) $customer['role_id'] !== 3) {
        return null;
    }

    if ((int) $customer['is_active'] !== 1) {
        return null;
    }

    if ($actorType === 'dealer') {
        if ((int) $customer['dealerId'] !== (int) $actorId) {
            return null;
        }
    } elseif ($actorType === 'owner') {
        if ((int) $customer['id'] !== (int) $actorId) {
            return null;
        }
    }

    return $customer;
}

function catalog_require_customer($con, $actorType, $actorId, $customerId)
{
    $customer = catalog_authorize_customer($con, $actorType, $actorId, $customerId);
    if ($customer === null) {
        catalog_json_response(array(
            'success' => false,
            'status' => 'false',
            'message' => catalog_error_message('UNAUTHORIZED_CUSTOMER'),
            'code' => 'UNAUTHORIZED_CUSTOMER',
        ), 403);
        exit;
    }
    return $customer;
}
