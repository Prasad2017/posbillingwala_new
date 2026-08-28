<?php

namespace App\Providers;

use App\Services\AdminBranding;
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
        //
    }

    /**
     * Bootstrap any application services.
     *
     * @return void
     */
    public function boot()
    {
        if ($root = config('app.url')) {
            \Illuminate\Support\Facades\URL::forceRootUrl(rtrim($root, '/'));
        }

        View::composer('*', function ($view) {
            $view->with('adminLogoUrl', AdminBranding::logoUrl());
            $view->with('adminFaviconUrl', AdminBranding::faviconUrl());
        });
    }
}
