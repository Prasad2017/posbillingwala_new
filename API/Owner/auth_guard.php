<?php
require_once __DIR__ . '/../auth_tokens.php';

/**
 * Require valid owner Bearer token.
 */
function owner_require_auth($con, $unauthorizedPayload = null)
{
    if (!auth_actor_token_valid($con, 'owner')) {
        header('Content-Type: application/json; charset=utf-8');
        if ($unauthorizedPayload !== null) {
            echo json_encode($unauthorizedPayload);
        } else {
            echo json_encode(array('status' => '0', 'message' => 'Unauthorized'));
        }
        mysqli_close($con);
        exit;
    }
}

/**
 * Resolve and enforce owner userId from token (ignore spoofed body when mismatched).
 *
 * @return string|null
 */
function owner_resolve_user_id($con, $postedUserId)
{
    return auth_user_id_from_request($con, $postedUserId, 'owner');
}

?>
