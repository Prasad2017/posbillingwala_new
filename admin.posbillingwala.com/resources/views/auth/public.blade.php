<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="icon" href="{{ $adminFaviconUrl ?? asset('assets/images/app_logo.png') }}" type="image/png" />
    <link href="{{ asset('assets/css/bootstrap.min.css') }}" rel="stylesheet">
    <link href="{{ asset('assets/css/bootstrap-extended.css') }}" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700&family=Syne:wght@600;700&display=swap" rel="stylesheet">
    <link href="{{ asset('assets/css/icons.css') }}" rel="stylesheet">
    <link href="{{ asset('assets/css/pos-brand.css') }}" rel="stylesheet">
    <title>POS Billingwala | Login</title>
</head>
<body class="bg-login">
    <div class="pb-login-page">
        <div class="pb-login-shell">
            <div class="pb-login-brand text-center">
                <img src="{{ $adminLogoUrl ?? asset('assets/images/app_logo.png') }}" alt="POS Billingwala" class="pb-login-logo">
            </div>
            @yield('content')
        </div>
    </div>
    <script src="{{ asset('assets/js/jquery.min.js') }}"></script>
    <script src="{{ asset('assets/js/bootstrap.bundle.min.js') }}"></script>
    @stack('scripts')
</body>
</html>
