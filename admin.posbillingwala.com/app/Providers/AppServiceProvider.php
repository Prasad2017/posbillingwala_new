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
        /* Always follow .env APP_URL (and ASSET_URL via config) — never request host. */
        $root = rtrim((string) config('app.url'), '/');
        if ($root !== '') {
            URL::forceRootUrl($root);
        }

        /* If APP_URL is https://..., never emit http://asset links (mixed content blocks CSS). */
        $scheme = parse_url($root, PHP_URL_SCHEME);
        if (is_string($scheme) && strtolower($scheme) === 'https') {
            URL::forceScheme('https');
        }
    }
}
