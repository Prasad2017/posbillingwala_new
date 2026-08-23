<?php
/**
 * P5-3: Incremental session tokens for POS / Owner / Dealer / Admin.
 * Backward compatible — endpoints accept legacy userId when no token is sent.
 */

require_once __DIR__ . '/db_prepared.php';

if (!defined('AUTH_TOKEN_TTL_DAYS')) {
    define('AUTH_TOKEN_TTL_DAYS', 30);
}

if (!function_exists('auth_token_hash')) {
    function auth_token_hash($plainToken)
    {
        return hash('sha256', (string) $plainToken);
    }
}

if (!function_exists('auth_token_from_request')) {
    function auth_token_from_request()
    {
        if (function_exists('getallheaders')) {
            foreach (getallheaders() as $name => $value) {
                if (strcasecmp($name, 'Authorization') === 0) {
                    if (preg_match('/Bearer\s+(\S+)/i', $value, $matches)) {
                        return $matches[1];
                    }
                }
            }
        }

        if (isset($_SERVER['HTTP_AUTHORIZATION']) && $_SERVER['HTTP_AUTHORIZATION'] !== '') {
            if (preg_match('/Bearer\s+(\S+)/i', $_SERVER['HTTP_AUTHORIZATION'], $matches)) {
                return $matches[1];
            }
        }

        if (isset($_POST['authToken']) && $_POST['authToken'] !== '') {
            return $_POST['authToken'];
        }

        if (isset($_GET['authToken']) && $_GET['authToken'] !== '') {
            return $_GET['authToken'];
        }

        return null;
    }
}

if (!function_exists('auth_token_issue')) {
    /**
     * @param mysqli $con
     * @param string $actorType pos_licence|owner|dealer|admin
     * @param int|string $actorId
     * @param string|null $deviceId
     * @param int|null $ttlDays
     * @return array|null authToken + tokenExpiresAt, or null if table unavailable
     */
    function auth_token_issue($con, $actorType, $actorId, $deviceId = null, $ttlDays = null)
    {
        if ($ttlDays === null) {
            $ttlDays = AUTH_TOKEN_TTL_DAYS;
        }

        try {
            $plainToken = bin2hex(random_bytes(32));
        } catch (Exception $e) {
            return null;
        }

        $tokenHash = auth_token_hash($plainToken);
        $expiresAt = date('Y-m-d H:i:s', strtotime('+' . (int) $ttlDays . ' days'));
        $deviceValue = ($deviceId !== null && $deviceId !== '') ? (string) $deviceId : null;

        $insertId = db_stmt_insert_id(
            $con,
            'INSERT INTO `api_tokens` (`token_hash`, `actor_type`, `actor_id`, `device_id`, `expires_at`) VALUES (?,?,?,?,?)',
            'ssiss',
            $tokenHash,
            $actorType,
            (int) $actorId,
            $deviceValue,
            $expiresAt
        );

        if ($insertId === false) {
            return null;
        }

        return array(
            'authToken' => $plainToken,
            'tokenExpiresAt' => $expiresAt,
        );
    }
}

if (!function_exists('auth_token_resolve')) {
    /**
     * @param mysqli $con
     * @param string $plainToken
     * @return array|null
     */
    function auth_token_resolve($con, $plainToken)
    {
        if ($plainToken === null || $plainToken === '') {
            return null;
        }

        $row = db_stmt_fetch_one(
            $con,
            'SELECT `id`, `actor_type`, `actor_id`, `device_id`, `expires_at` FROM `api_tokens` WHERE `token_hash`=? AND `expires_at` > NOW() LIMIT 1',
            's',
            auth_token_hash($plainToken)
        );

        if ($row === null) {
            return null;
        }

        db_stmt_execute(
            $con,
            'UPDATE `api_tokens` SET `last_used_at`=NOW() WHERE `id`=?',
            'i',
            (int) $row['id']
        );

        return $row;
    }
}

if (!function_exists('auth_token_append_response')) {
    function auth_token_append_response($con, array &$response, $actorType, $actorId, $deviceId = null)
    {
        $issued = auth_token_issue($con, $actorType, $actorId, $deviceId);
        if ($issued !== null) {
            $response['authToken'] = $issued['authToken'];
            $response['tokenExpiresAt'] = $issued['tokenExpiresAt'];
        }
        return $response;
    }
}

if (!function_exists('auth_resolve_actor_from_request')) {
    function auth_resolve_actor_from_request($con)
    {
        $plainToken = auth_token_from_request();
        if ($plainToken === null || $plainToken === '') {
            return null;
        }

        return auth_token_resolve($con, $plainToken);
    }
}

if (!function_exists('auth_pos_licence_id_from_request')) {
    /**
     * When Bearer token is present, derive licence id from token (ignore spoofed body).
     * When no token, return posted userId unchanged.
     * Returns null when token was sent but invalid/expired.
     *
     * @param mysqli $con
     * @param string $postedUserId
     * @return string|null
     */
    function auth_pos_licence_id_from_request($con, $postedUserId)
    {
        $plainToken = auth_token_from_request();
        if ($plainToken === null || $plainToken === '') {
            return $postedUserId;
        }

        $actor = auth_token_resolve($con, $plainToken);
        if ($actor === null || $actor['actor_type'] !== 'pos_licence') {
            return null;
        }

        return (string) $actor['actor_id'];
    }
}

if (!function_exists('auth_user_id_from_request')) {
    /**
     * Resolve owner/dealer/admin user id from Bearer token when present.
     *
     * @param mysqli $con
     * @param string $postedUserId
     * @param string $expectedActorType owner|dealer|admin
     * @return string|null
     */
    function auth_user_id_from_request($con, $postedUserId, $expectedActorType)
    {
        $plainToken = auth_token_from_request();
        if ($plainToken === null || $plainToken === '') {
            return $postedUserId;
        }

        $actor = auth_token_resolve($con, $plainToken);
        if ($actor === null || $actor['actor_type'] !== $expectedActorType) {
            return null;
        }

        return (string) $actor['actor_id'];
    }
}

if (!function_exists('auth_actor_token_valid_or_legacy')) {
    /**
     * When no token is sent, allow legacy callers. When token is sent, it must match actor type.
     *
     * @param mysqli $con
     * @param string $expectedActorType
     * @return bool
     */
    function auth_actor_token_valid_or_legacy($con, $expectedActorType)
    {
        $plainToken = auth_token_from_request();
        if ($plainToken === null || $plainToken === '') {
            return true;
        }

        $actor = auth_token_resolve($con, $plainToken);
        return $actor !== null && $actor['actor_type'] === $expectedActorType;
    }
}

?>
