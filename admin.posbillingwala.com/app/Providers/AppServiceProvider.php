<?php

namespace App\Providers;

use App\Services\AdminBranding;
use Illuminate\Support\Facades\URL;
use Illuminate\Support\Facades\View;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     *
     * @return void
     */
    public function register()
    {
        require_once app_path('helpers.php');
    }

    /**
     * Bootstrap any application services.
     *
     * @return void
     */
    public function boot()
    {
        \admin_sync_public_assets();
        $this->configureRequestUrls();

        View::composer('*', function ($view) {
            try {
                $view->with('adminLogoUrl', AdminBranding::logoUrl());
                $view->with('adminFaviconUrl', AdminBranding::faviconUrl());
            } catch (\Throwable $e) {
                $view->with('adminLogoUrl', \admin_asset('images/pos_billingwala_logo.png'));
                $view->with('adminFaviconUrl', \admin_asset('images/app_logo.png'));
            }
        });
    }

    private function configureRequestUrls(): void
    {
        if ($this->app->runningInConsole()) {
            if ($root = config('app.url')) {
                URL::forceRootUrl(rtrim($root, '/'));
            }

            return;
        }

        $request = request();
        if (! $request || ! $request->getHttpHost()) {
            return;
        }

        $configured = rtrim((string) config('app.url'), '/');
        $configuredHost = $configured ? (parse_url($configured, PHP_URL_HOST) ?: '') : '';
        $requestHost = $request->getHost();
        $requestRoot = $request->getSchemeAndHttpHost();

        $assetUrl = (string) env('ASSET_URL', '');
        $assetHost = $assetUrl ? (parse_url($assetUrl, PHP_URL_HOST) ?: '') : '';

        // Wrong ASSET_URL in .env (old /adminpanel path or different domain) breaks CSS on subdomains.
        if ($assetHost !== '' && strcasecmp($assetHost, $requestHost) !== 0) {
            config(['app.asset_url' => null]);
        }

        if ($configuredHost === '' || strcasecmp($configuredHost, $requestHost) !== 0) {
            URL::forceRootUrl($requestRoot);
        } elseif ($configured !== '') {
            URL::forceRootUrl($configured);
        }
    }
}
