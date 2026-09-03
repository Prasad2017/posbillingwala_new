<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Cross-Origin Resource Sharing (CORS) Configuration
    |--------------------------------------------------------------------------
    |
    | Public marketing site (posbillingwala.com) calls /api/website/* on this
    | admin app. Prefer explicit origins; patterns cover www / apex variants.
    |
    | To learn more: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
    |
    */

    'paths' => ['api/*', 'sanctum/csrf-cookie'],

    'allowed_methods' => ['*'],

    'allowed_origins' => [
        'https://posbillingwala.com',
        'https://www.posbillingwala.com',
        'http://posbillingwala.com',
        'http://www.posbillingwala.com',
        'http://127.0.0.1:8000',
        'http://localhost:8000',
        'http://127.0.0.1:5500',
        'http://localhost:5500',
    ],

    'allowed_origins_patterns' => [
        '#^https?://([a-z0-9-]+\.)?posbillingwala\.com$#i',
        '#^https?://(localhost|127\.0\.0\.1)(:\d+)?$#i',
    ],

    'allowed_headers' => ['*'],

    'exposed_headers' => [],

    'max_age' => 60 * 60 * 24,

    'supports_credentials' => false,

];
