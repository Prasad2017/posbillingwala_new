<?php
/**
 * Session tokens for POS / Owner / Dealer / Admin.
 * POS / Owner token lifetime follows licence expiryDate (licence validity).
 */

require_once __DIR__ . '/db_prepared.php';

if (!defined('AUTH_TOKEN_TTL_DAYS')) {
    // Fallback only for Admin / Dealer (no licence row).
    define('AUTH_TOKEN_TTL_DAYS', 90);
}

if (!function_exists('auth_token_expires_at_from_licence_date')) {
    /**
     * Licence expiry is a date (Y-m-d). Token stays valid through end of that day.
     *
     * @param string|null $expiryDate
     * @return string|null Y-m-d 23:59:59
     */
    function auth_token_expires_at_from_licence_date($expiryDate)
    {
        if ($expiryDate === null) {
            return null;
        }
        $expiryDate = trim((string) $expiryDate);
        if ($expiryDate === '' || $expiryDate === '0000-00-00') {
            return null;
        }
        // Accept datetime — use date part only
        if (preg_match('/^(\d{4}-\d{2}-\d{2})/', $expiryDate, $m)) {
            $day = $m[1];
            $ts = strtotime($day . ' 23:59:59');
            if ($ts === false || $ts < time()) {
                return null;
            }
            return date('Y-m-d H:i:s', $ts);
        }
        return null;
    }
}

if (!function_exists('auth_token_lookup_licence_expiry')) {
    /**
     * @param mysqli $con
     * @param string $actorType
     * @param int|string $actorId
     * @return string|null Y-m-d
     */
    function auth_token_lookup_licence_expiry($con, $actorType, $actorId)
    {
        if ($actorType === 'pos_licence') {
            return db_stmt_scalar_string(
                $con,
                'SELECT `expiryDate` FROM `licenses` WHERE `id`=? LIMIT 1',
                'i',
                (int) $actorId
            );
        }
        if ($actorType === 'owner') {
            // Owner session lasts until their latest branch licence ends
            return db_stmt_scalar_string(
                $con,
                'SELECT MAX(`expiryDate`) FROM `licenses`
                 WHERE `userId`=? AND `expiryDate` IS NOT NULL AND `expiryDate` > \'0000-00-00\'',
                'i',
                (int) $actorId
            );
        }
        return null;
    }
}

if (!function_exists('auth_token_compute_expires_at')) {
    /**
     * Prefer explicit licence expiry; else look up; else fixed TTL (admin/dealer).
     *
     * @param mysqli $con
     * @param string $actorType
     * @param int|string $actorId
     * @param string|null $licenceExpiryDate
     * @param int|null $ttlDays
     * @return string|null
     */
    function auth_token_compute_expires_at($con, $actorType, $actorId, $licenceExpiryDate = null, $ttlDays = null)
    {
        $fromLicence = auth_token_expires_at_from_licence_date($licenceExpiryDate);
        if ($fromLicence !== null) {
            return $fromLicence;
        }

        if ($actorType === 'pos_licence' || $actorType === 'owner') {
            $lookedUp = auth_token_lookup_licence_expiry($con, $actorType, $actorId);
            $fromLicence = auth_token_expires_at_from_licence_date($lookedUp);
            if ($fromLicence !== null) {
                return $fromLicence;
            }
            // Licence missing/expired — do not issue a long-lived token
            return null;
        }

        if ($ttlDays === null) {
            $ttlDays = (int) AUTH_TOKEN_TTL_DAYS;
        }
        return date('Y-m-d H:i:s', strtotime('+' . (int) $ttlDays . ' days'));
    }
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

        if (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION']) && $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] !== '') {
            if (preg_match('/Bearer\s+(\S+)/i', $_SERVER['REDIRECT_HTTP_AUTHORIZATION'], $matches)) {
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
     * @param int|null $ttlDays fallback for admin/dealer only
     * @param string|null $licenceExpiryDate Y-m-d from licenses.expiryDate
     * @return array|null authToken + tokenExpiresAt, or null if table unavailable / licence expired
     */
    function auth_token_issue($con, $actorType, $actorId, $deviceId = null, $ttlDays = null, $licenceExpiryDate = null)
    {
        $expiresAt = auth_token_compute_expires_at($con, $actorType, $actorId, $licenceExpiryDate, $ttlDays);
        if ($expiresAt === null) {
            return null;
        }

        try {
            $plainToken = bin2hex(random_bytes(32));
        } catch (Exception $e) {
            return null;
        }

        $tokenHash = auth_token_hash($plainToken);
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

        // POS: also reject if the licence itself has expired
        if ($row['actor_type'] === 'pos_licence') {
            $licenceExpiry = auth_token_lookup_licence_expiry($con, 'pos_licence', $row['actor_id']);
            if (auth_token_expires_at_from_licence_date($licenceExpiry) === null) {
                return null;
            }
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

if (!function_exists('auth_token_revoke')) {
    function auth_token_revoke($con, $plainToken)
    {
        if ($plainToken === null || $plainToken === '') {
            return false;
        }
        return db_stmt_execute(
            $con,
            'UPDATE `api_tokens` SET `expires_at`=NOW() WHERE `token_hash`=?',
            's',
            auth_token_hash($plainToken)
        );
    }
}

if (!function_exists('auth_token_append_response')) {
    /**
     * @param string|null $licenceExpiryDate licenses.expiryDate (Y-m-d) — token ends with licence validity
     */
    function auth_token_append_response($con, array &$response, $actorType, $actorId, $deviceId = null, $licenceExpiryDate = null)
    {
        $issued = auth_token_issue($con, $actorType, $actorId, $deviceId, null, $licenceExpiryDate);
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
     * Require valid pos_licence Bearer token.
     * Posted userId (if any) must match the licence id OR that licence's owner (organization) id.
     *
     * @param mysqli $con
     * @param string $postedUserId
     * @return string|null licence id, or null when unauthorized
     */
    function auth_pos_licence_id_from_request($con, $postedUserId)
    {
        $plainToken = auth_token_from_request();
        if ($plainToken === null || $plainToken === '') {
            return null;
        }

        $actor = auth_token_resolve($con, $plainToken);
        if ($actor === null || $actor['actor_type'] !== 'pos_licence') {
            return null;
        }

        $licenceId = (string) $actor['actor_id'];
        $posted = trim((string) $postedUserId);
        if ($posted === '') {
            return $licenceId;
        }

        if ($posted === $licenceId) {
            return $licenceId;
        }

        $ownerId = db_stmt_scalar_string(
            $con,
            'SELECT `userId` FROM `licenses` WHERE `id`=? LIMIT 1',
            'i',
            (int) $licenceId
        );
        if ($ownerId !== null && $posted === (string) $ownerId) {
            return $licenceId;
        }

        return null;
    }
}

if (!function_exists('auth_user_id_from_request')) {
    /**
     * Resolve owner/dealer/admin user id from Bearer token (required).
     * Posted userId must match token actor when provided.
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
            return null;
        }

        $actor = auth_token_resolve($con, $plainToken);
        if ($actor === null || $actor['actor_type'] !== $expectedActorType) {
            return null;
        }

        $actorId = (string) $actor['actor_id'];
        $posted = trim((string) $postedUserId);
        if ($posted !== '' && $posted !== $actorId) {
            return null;
        }

        return $actorId;
    }
}

if (!function_exists('auth_actor_token_valid')) {
    /**
     * Require a valid Bearer token for the expected actor type.
     *
     * @param mysqli $con
     * @param string $expectedActorType
     * @return bool
     */
    function auth_actor_token_valid($con, $expectedActorType)
    {
        $plainToken = auth_token_from_request();
        if ($plainToken === null || $plainToken === '') {
            return false;
        }

        $actor = auth_token_resolve($con, $plainToken);
        return $actor !== null && $actor['actor_type'] === $expectedActorType;
    }
}

if (!function_exists('auth_actor_token_valid_or_legacy')) {
    /**
     * @deprecated Legacy name — now requires a valid token (no empty-token bypass).
     */
    function auth_actor_token_valid_or_legacy($con, $expectedActorType)
    {
        return auth_actor_token_valid($con, $expectedActorType);
    }
}

?>
