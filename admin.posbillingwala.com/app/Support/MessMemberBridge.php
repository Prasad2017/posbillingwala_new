<?php

namespace App\Support;

use mysqli;

class MessMemberBridge
{
    /**
     * Resolve shared mess_member_remote.php (local API/ or production androidApp/).
     */
    public static function resolveRemotePath(): ?string
    {
        $androidApp = env('ANDROID_APP_PATH');
        $candidates = array_filter([
            env('MESS_MEMBER_REMOTE_PATH'),
            is_string($androidApp) && $androidApp !== ''
                ? rtrim(str_replace('\\', '/', $androidApp), '/') . '/mess/mess_member_remote.php'
                : null,
            // Production: API is deployed as posbillingwala.com/androidApp
            '/home/rgusomuk/posbillingwala.com/androidApp/mess/mess_member_remote.php',
            dirname(base_path()) . '/posbillingwala.com/androidApp/mess/mess_member_remote.php',
            base_path('../posbillingwala.com/androidApp/mess/mess_member_remote.php'),
            // Local monorepo: ../API/mess
            base_path('../API/mess/mess_member_remote.php'),
            base_path('API/mess/mess_member_remote.php'),
            dirname(base_path()) . '/API/mess/mess_member_remote.php',
        ]);

        foreach ($candidates as $candidate) {
            if (!is_string($candidate) || $candidate === '') {
                continue;
            }
            $candidate = str_replace('\\', '/', $candidate);
            if (is_file($candidate)) {
                return $candidate;
            }
        }

        return null;
    }

    public static function bootstrap(): void
    {
        static $bootstrapped = false;
        if ($bootstrapped) {
            return;
        }

        $apiPath = self::resolveRemotePath();
        if ($apiPath === null) {
            throw new \RuntimeException(
                'Shared mess member module not found. On the server, mess helpers live at '
                . 'posbillingwala.com/androidApp/mess/. Set MESS_MEMBER_REMOTE_PATH or ANDROID_APP_PATH in .env.'
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
