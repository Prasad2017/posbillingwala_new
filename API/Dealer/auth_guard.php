<?php
require_once __DIR__ . '/../auth_tokens.php';

/**
 * Require valid dealer Bearer token (mandatory).
 */
function dealer_require_auth($con, $unauthorizedPayload = null)
{
    if (!auth_actor_token_valid($con, 'dealer')) {
        header('Content-Type: application/json; charset=utf-8');
        if ($unauthorizedPayload !== null) {
            echo json_encode($unauthorizedPayload);
        } else {
            echo json_encode(array('status' => 'false', 'message' => 'Unauthorized'));
        }
        mysqli_close($con);
        exit;
    }
}

?>
