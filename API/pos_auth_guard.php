<?php
require_once __DIR__ . '/auth_tokens.php';

/**
 * Require valid POS Bearer token. Returns authenticated licence id.
 * Posted userId (licence or owner org) is validated against the token.
 *
 * @param mysqli $con
 * @param string|null $postedUserId
 * @param array|null $unauthorizedPayload
 * @return string licence id
 */
function pos_require_auth($con, $postedUserId = null, $unauthorizedPayload = null)
{
    if ($postedUserId === null) {
        if (isset($_POST['userId'])) {
            $postedUserId = $_POST['userId'];
        } elseif (isset($_GET['userId'])) {
            $postedUserId = $_GET['userId'];
        } else {
            $postedUserId = '';
        }
    }

    $licenceId = auth_pos_licence_id_from_request($con, (string) $postedUserId);
    if ($licenceId === null || $licenceId === '') {
        header('Content-Type: application/json; charset=utf-8');
        if ($unauthorizedPayload !== null) {
            echo json_encode($unauthorizedPayload);
        } else {
            echo json_encode(array('status' => '0', 'message' => 'Unauthorized — login required'));
        }
        if (isset($con) && $con instanceof mysqli) {
            mysqli_close($con);
        }
        exit;
    }

    return $licenceId;
}

?>
