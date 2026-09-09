<?php

namespace App\Support;

use Auth;
use Illuminate\Support\Facades\DB;
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

    /**
     * Resolve shared catalog bootstrap.php (local API/ or production androidApp/).
     */
    public static function resolveBootstrapPath(): ?string
    {
        $androidApp = env('ANDROID_APP_PATH');
        $candidates = array_filter([
            env('CATALOG_BOOTSTRAP_PATH'),
            is_string($androidApp) && $androidApp !== ''
                ? rtrim(str_replace('\\', '/', $androidApp), '/') . '/catalog/bootstrap.php'
                : null,
            // Production: API is deployed as posbillingwala.com/androidApp
            '/home/rgusomuk/posbillingwala.com/androidApp/catalog/bootstrap.php',
            dirname(base_path()) . '/posbillingwala.com/androidApp/catalog/bootstrap.php',
            base_path('../posbillingwala.com/androidApp/catalog/bootstrap.php'),
            // Local monorepo: ../API/catalog
            base_path('../API/catalog/bootstrap.php'),
            base_path('API/catalog/bootstrap.php'),
            dirname(base_path()) . '/API/catalog/bootstrap.php',
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

        $apiCatalogPath = self::resolveBootstrapPath();
        if ($apiCatalogPath === null) {
            throw new \RuntimeException(
                'Shared catalog module not found. On the server, catalog lives at '
                . 'posbillingwala.com/androidApp/catalog/. Set CATALOG_BOOTSTRAP_PATH or ANDROID_APP_PATH in .env.'
            );
        }

        require_once $apiCatalogPath;
        $handlers = dirname($apiCatalogPath) . '/catalog_handlers.php';
        if (is_file($handlers)) {
            require_once $handlers;
        }
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

        // Prefer shared catalog auth when available; fall back to Laravel DB so
        // Mess Members / customer pages keep working if only path config is wrong.
        if (self::resolveBootstrapPath() !== null) {
            try {
                self::bootstrap();
                if (function_exists('catalog_authorize_customer')) {
                    return catalog_authorize_customer(
                        self::connection(),
                        $actor['actor_type'],
                        $actor['actor_id'],
                        $customerId
                    ) !== null;
                }
            } catch (\Throwable $e) {
                \Log::warning('CatalogBridge::authorizeCustomer bootstrap failed: ' . $e->getMessage());
            }
        }

        return self::authorizeCustomerLocal($actor['actor_type'], $actor['actor_id'], $customerId);
    }

    private static function authorizeCustomerLocal(string $actorType, int $actorId, int $customerId): bool
    {
        if ($customerId <= 0) {
            return false;
        }

        $query = DB::table('users')
            ->where('id', $customerId)
            ->where('role_id', 3)
            ->where('is_active', 1);

        if ($actorType === 'dealer') {
            $query->where('dealerId', $actorId);
        } elseif ($actorType === 'owner') {
            $query->where('id', $actorId);
        } elseif ($actorType !== 'admin') {
            return false;
        }

        return $query->exists();
    }
}
