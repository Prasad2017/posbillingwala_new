<?php

namespace App\Support;

use Auth;
use mysqli;

class CatalogBridge
{
    private static ?mysqli $connection = null;

    public static function connection(): mysqli
    {
        if (self::$connection instanceof mysqli) {
            return self::$connection;
        }

        $config = config('database.connections.mysql');
        $con = mysqli_connect(
            $config['host'],
            $config['username'],
            $config['password'],
            $config['database'],
            (int) $config['port']
        );

        if (!$con) {
            throw new \RuntimeException('Unable to connect to catalog database.');
        }

        mysqli_set_charset($con, 'utf8');
        self::$connection = $con;

        return $con;
    }

    public static function bootstrap(): void
    {
        static $bootstrapped = false;
        if ($bootstrapped) {
            return;
        }

        $candidates = array_filter([
            env('CATALOG_BOOTSTRAP_PATH'),
            base_path('../API/catalog/bootstrap.php'),
            base_path('API/catalog/bootstrap.php'),
            dirname(base_path()) . '/API/catalog/bootstrap.php',
        ]);

        $apiCatalogPath = null;
        foreach ($candidates as $candidate) {
            if ($candidate !== null && $candidate !== '' && is_file($candidate)) {
                $apiCatalogPath = $candidate;
                break;
            }
        }

        if ($apiCatalogPath === null) {
            throw new \RuntimeException('Shared catalog module not found. Deploy API/catalog alongside the admin app or set CATALOG_BOOTSTRAP_PATH in .env.');
        }

        require_once $apiCatalogPath;
        require_once dirname($apiCatalogPath) . '/catalog_handlers.php';
        $bootstrapped = true;
    }

    /**
     * @return array{actor_type:string,actor_id:int}|null
     */
    public static function resolveWebActor(): ?array
    {
        $user = Auth::user();
        if ($user === null) {
            return null;
        }

        if ((int) $user->role_id === 1) {
            return ['actor_type' => 'admin', 'actor_id' => (int) $user->id];
        }

        if ((int) $user->role_id === 2) {
            return ['actor_type' => 'dealer', 'actor_id' => (int) $user->id];
        }

        return null;
    }

    public static function authorizeCustomer(int $customerId): bool
    {
        $actor = self::resolveWebActor();
        if ($actor === null) {
            return false;
        }

        self::bootstrap();
        $con = self::connection();

        return catalog_authorize_customer(
            $con,
            $actor['actor_type'],
            $actor['actor_id'],
            $customerId
        ) !== null;
    }
}
