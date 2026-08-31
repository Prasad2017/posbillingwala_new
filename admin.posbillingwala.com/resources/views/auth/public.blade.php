<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <link rel="icon" href="{{ $adminFaviconUrl ?? admin_asset('images/app_logo.png') }}" type="image/png" />
    <link href="{{ admin_asset('css/bootstrap.min.css') }}" rel="stylesheet">
    <link href="{{ admin_asset('css/bootstrap-extended.css') }}" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700&family=Syne:wght@600;700&display=swap" rel="stylesheet">
    <link href="{{ admin_asset('css/icons.css') }}" rel="stylesheet">
    <link href="{{ admin_asset('css/pos-brand.css') }}" rel="stylesheet">
    <link href="{{ admin_asset('css/pos-responsive.css') }}" rel="stylesheet">
    <title>POS Billingwala | Login</title>
</head>
<body class="bg-login">
    <div class="pb-login-page">
        <div class="pb-login-shell">
            <div class="pb-login-brand text-center">
                <img src="{{ $adminLogoUrl ?? admin_asset('images/pos_billingwala_logo.png') }}" alt="POS Billingwala" class="pb-login-logo" width="420" height="120">
            </div>
            @yield('content')
        </div>
    </div>
    <script src="{{ admin_asset('js/jquery.min.js') }}"></script>
    <script src="{{ admin_asset('js/bootstrap.bundle.min.js') }}"></script>
    @stack('scripts')
</body>
</html>
