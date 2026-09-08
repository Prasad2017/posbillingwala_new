<?php

namespace App\Support;

use mysqli;

class MessMemberBridge
{
    public static function bootstrap(): void
    {
        static $bootstrapped = false;
        if ($bootstrapped) {
            return;
        }

        $candidates = array_filter([
            env('MESS_MEMBER_REMOTE_PATH'),
            base_path('../API/mess/mess_member_remote.php'),
            base_path('API/mess/mess_member_remote.php'),
            dirname(base_path()) . '/API/mess/mess_member_remote.php',
        ]);

        $apiPath = null;
        foreach ($candidates as $candidate) {
            if ($candidate !== null && $candidate !== '' && is_file($candidate)) {
                $apiPath = $candidate;
                break;
            }
        }

        if ($apiPath === null) {
            throw new \RuntimeException(
                'Shared mess member module not found. Deploy API/mess alongside the admin app or set MESS_MEMBER_REMOTE_PATH in .env.'
            );
        }

        require_once $apiPath;
        $bootstrapped = true;
    }

    public static function connection(): mysqli
    {
        return CatalogBridge::connection();
    }

    public static function authorizeCustomer(int $customerId): bool
    {
        return CatalogBridge::authorizeCustomer($customerId);
    }
}
