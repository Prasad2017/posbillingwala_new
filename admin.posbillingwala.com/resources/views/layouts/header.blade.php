@php
    $roleLabels = [1 => 'Admin', 2 => 'Dealer', 3 => 'Customer'];
    $userRole = $roleLabels[Auth::user()->role_id ?? 0] ?? 'User';
    $notifCount = 0;
    if (Auth::check() && Auth::user()->role_id == 1) {
        try {
            $notifCount = \App\Services\AdminMetrics::dashboard()['notificationCount'] ?? 0;
        } catch (\Throwable $e) {
            $notifCount = 0;
        }
    }
@endphp
<header>
    <div class="topbar pb-topbar d-flex align-items-center">
        <nav class="navbar navbar-expand w-100">
            <div class="d-flex align-items-center gap-2 flex-grow-1">
                <div class="mobile-toggle-menu"><i class='bx bx-menu'></i></div>
                <h1 class="pb-page-title mb-0">@yield('page_title', 'Dashboard')</h1>
            </div>
            <div class="d-flex align-items-center gap-2 ms-auto">
                @if(Auth::user()->role_id == 1)
                <a href="{{ url('reports/licenses') }}" class="pb-notif-btn" title="Notifications">
                    <i class='bx bx-bell'></i>
                    @if($notifCount > 0)
                    <span class="pb-notif-badge">{{ $notifCount > 9 ? '9+' : $notifCount }}</span>
                    @endif
                </a>
                @endif
                <div class="user-box dropdown">
                    <a class="d-flex align-items-center nav-link dropdown-toggle dropdown-toggle-nocaret pb-user-link" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <span class="pb-user-avatar">{{ strtoupper(substr(Auth::user()->name ?? 'A', 0, 1)) }}</span>
                        <div class="user-info ps-2">
                            <p class="user-name mb-0">{{ Auth::user()->name ?? '' }}</p>
                            <p class="designattion mb-0">{{ $userRole }}</p>
                        </div>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><a class="dropdown-item" href="{{ url('home') }}"><i class='bx bx-home-circle'></i><span>Dashboard</span></a></li>
                        <li><a class="dropdown-item" href="{{ url('settings') }}"><i class='bx bx-cog'></i><span>Settings</span></a></li>
                        <li><a class="dropdown-item" href="{{ url('settings/profile') }}"><i class='bx bx-user'></i><span>Profile</span></a></li>
                        <li><a class="dropdown-item" href="{{ url('settings/password') }}"><i class='bx bx-lock-alt'></i><span>Change Password</span></a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item text-danger" href="javascript:;" onclick="event.preventDefault(); document.getElementById('logout-form').submit();"><i class='bx bx-log-out-circle'></i><span>Logout</span></a></li>
                    </ul>
                </div>
                <form id="logout-form" action="{{ route('logout') }}" method="POST" style="display: none;">
                    {{ csrf_field() }}
                </form>
            </div>
        </nav>
    </div>
</header>
