@php
    $path = trim(request()->path(), '/');
    $is = function ($patterns) use ($path) {
        foreach ((array) $patterns as $p) {
            $p = trim($p, '/');
            if ($p === '' && $path === '') {
                return true;
            }
            if ($p !== '' && ($path === $p || str_starts_with($path, $p . '/'))) {
                return true;
            }
        }
        return false;
    };
@endphp
<div class="sidebar-wrapper" data-simplebar="true">
    <div class="sidebar-header">
        <div class="d-flex align-items-center gap-2">
            <img src="{{ $adminLogoUrl ?? asset('assets/images/app_logo.png') }}" class="logo-icon" alt="POS Billingwala">
            <div class="sidebar-brand-text">
                <small class="sidebar-tagline">Admin Panel</small>
            </div>
        </div>
        <div class="toggle-icon ms-auto"><i class='bx bx-chevrons-left'></i></div>
    </div>

    <ul class="metismenu pb-nav" id="menu">

        <li class="menu-section"><span>Main</span></li>
        <li class="{{ $is('home') ? 'mm-active' : '' }}">
            <a href="{{ url('home') }}">
                <div class="parent-icon"><i class='bx bx-grid-alt'></i></div>
                <div class="menu-title">Dashboard</div>
            </a>
        </li>

        @if(Auth::user()->role_id == 1)
        <li class="menu-section"><span>Management</span></li>
        <li class="{{ $is('dealer/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-store-alt'></i></div>
                <div class="menu-title">Dealers</div>
            </a>
            <ul class="{{ $is('dealer/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('dealer/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('dealer/all') }}"><i class="sub-icon bx bx-buildings"></i><span>All Dealers</span></a>
                </li>
                <li class="{{ $is('dealer/add') ? 'mm-active' : '' }}">
                    <a href="{{ url('dealer/add') }}"><i class="sub-icon bx bx-user-plus"></i><span>Add Dealer</span></a>
                </li>
            </ul>
        </li>
        @endif

        @if(Auth::user()->role_id == 1 || Auth::user()->role_id == 2)
        <li class="{{ $is('customers/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-group'></i></div>
                <div class="menu-title">Customers</div>
            </a>
            <ul class="{{ $is('customers/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('customers/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('customers/all') }}"><i class="sub-icon bx bx-id-card"></i><span>All Customers</span></a>
                </li>
                <li class="{{ $is('customers/add') ? 'mm-active' : '' }}">
                    <a href="{{ url('customers/add') }}"><i class="sub-icon bx bx-user-plus"></i><span>Add Customer</span></a>
                </li>
            </ul>
        </li>
        @endif

        @if(Auth::user()->role_id == 1)
        <li class="menu-section"><span>Analytics</span></li>
        <li class="{{ $is('sales/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-trending-up'></i></div>
                <div class="menu-title">Sales</div>
            </a>
            <ul class="{{ $is('sales/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('sales/dashboard') ? 'mm-active' : '' }}">
                    <a href="{{ url('sales/dashboard') }}"><i class="sub-icon bx bx-pie-chart-alt-2"></i><span>Sales Dashboard</span></a>
                </li>
                <li class="{{ $is('sales/overview') ? 'mm-active' : '' }}">
                    <a href="{{ url('sales/overview') }}"><i class="sub-icon bx bx-line-chart"></i><span>Sales Overview</span></a>
                </li>
                <li class="{{ $is('sales/invoices*') ? 'mm-active' : '' }}">
                    <a href="{{ url('sales/invoices') }}"><i class="sub-icon bx bx-receipt"></i><span>Recent Invoices</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('reports*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-bar-chart-alt-2'></i></div>
                <div class="menu-title">Reports</div>
            </a>
            <ul class="{{ $is('reports*') ? 'mm-show' : '' }}">
                <li class="{{ $path === 'reports' ? 'mm-active' : '' }}">
                    <a href="{{ url('reports') }}"><i class="sub-icon bx bx-grid-alt"></i><span>Reports Hub</span></a>
                </li>
                <li class="{{ $is('reports/customers') ? 'mm-active' : '' }}">
                    <a href="{{ url('reports/customers') }}"><i class="sub-icon bx bx-user-check"></i><span>Customer Reports</span></a>
                </li>
                <li class="{{ $is('reports/licenses') ? 'mm-active' : '' }}">
                    <a href="{{ url('reports/licenses') }}"><i class="sub-icon bx bx-key"></i><span>License Reports</span></a>
                </li>
                <li class="{{ $is('reports/dealers') ? 'mm-active' : '' }}">
                    <a href="{{ url('reports/dealers') }}"><i class="sub-icon bx bx-store"></i><span>Dealer Reports</span></a>
                </li>
                <li class="{{ $is('reports/branches') ? 'mm-active' : '' }}">
                    <a href="{{ url('reports/branches') }}"><i class="sub-icon bx bx-sitemap"></i><span>Branch Reports</span></a>
                </li>
                <li class="{{ $is('reports/devices') ? 'mm-active' : '' }}">
                    <a href="{{ url('reports/devices') }}"><i class="sub-icon bx bx-chip"></i><span>Device Reports</span></a>
                </li>
            </ul>
        </li>

        <li class="menu-section"><span>Monitoring</span></li>
        <li class="{{ $is('devices') ? 'mm-active' : '' }}">
            <a href="{{ url('devices') }}">
                <div class="parent-icon"><i class='bx bx-desktop'></i></div>
                <div class="menu-title">POS Monitoring</div>
            </a>
        </li>
        <li class="{{ $is('crashes*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-error-circle'></i></div>
                <div class="menu-title">Crash &amp; Errors</div>
            </a>
            <ul class="{{ $is('crashes*') ? 'mm-show' : '' }}">
                <li class="{{ $path === 'crashes' ? 'mm-active' : '' }}">
                    <a href="{{ url('crashes') }}"><i class="sub-icon bx bx-bug-alt"></i><span>Crash Logs</span></a>
                </li>
                <li class="{{ $is('crashes/errors*') ? 'mm-active' : '' }}">
                    <a href="{{ url('crashes/errors') }}"><i class="sub-icon bx bx-cloud-lightning"></i><span>API Error Logs</span></a>
                </li>
                <li class="{{ $is('crashes/analytics') ? 'mm-active' : '' }}">
                    <a href="{{ url('crashes/analytics') }}"><i class="sub-icon bx bx-analyse"></i><span>Crash Analytics</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('support*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-support'></i></div>
                <div class="menu-title">Support</div>
            </a>
            <ul class="{{ $is('support*') ? 'mm-show' : '' }}">
                <li class="{{ $path === 'support' ? 'mm-active' : '' }}">
                    <a href="{{ url('support') }}"><i class="sub-icon bx bx-help-circle"></i><span>Help &amp; Support</span></a>
                </li>
                <li class="{{ $is('support/tickets') && !$is('support/tickets/create') ? 'mm-active' : '' }}">
                    <a href="{{ url('support/tickets') }}"><i class="sub-icon bx bx-ticket"></i><span>My Tickets</span></a>
                </li>
                <li class="{{ $is('support/tickets/create') ? 'mm-active' : '' }}">
                    <a href="{{ url('support/tickets/create') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Create Ticket</span></a>
                </li>
                <li class="{{ $is('support/faq') ? 'mm-active' : '' }}">
                    <a href="{{ url('support/faq') }}"><i class="sub-icon bx bx-message-rounded-dots"></i><span>FAQs</span></a>
                </li>
            </ul>
        </li>
        @endif

        @if(Auth::user()->role_id == 1 || Auth::user()->role_id == 2)
        <li class="menu-section"><span>Catalog</span></li>
        <li class="{{ $is('product-import') ? 'mm-active' : '' }}">
            <a href="{{ url('product-import') }}">
                <div class="parent-icon"><i class='bx bx-cloud-upload'></i></div>
                <div class="menu-title">Product Import</div>
            </a>
        </li>
        <li class="{{ $is(['categories/*', 'subcategories/*', 'portion-masters/*', 'products/*']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-package'></i></div>
                <div class="menu-title">Catalog</div>
            </a>
            <ul class="{{ $is(['categories/*', 'subcategories/*', 'portion-masters/*', 'products/*']) ? 'mm-show' : '' }}">
                <li class="{{ $is('categories/*') ? 'mm-active' : '' }}">
                    <a href="{{ url('categories/all') }}"><i class="sub-icon bx bx-category-alt"></i><span>Categories</span></a>
                </li>
                <li class="{{ $is('subcategories/*') ? 'mm-active' : '' }}">
                    <a href="{{ url('subcategories/all') }}"><i class="sub-icon bx bx-list-check"></i><span>Subcategories</span></a>
                </li>
                <li class="{{ $is('portion-masters/*') ? 'mm-active' : '' }}">
                    <a href="{{ url('portion-masters/all') }}"><i class="sub-icon bx bx-food-menu"></i><span>Portion Master</span></a>
                </li>
                <li class="{{ $is('products/*') ? 'mm-active' : '' }}">
                    <a href="{{ url('products/all') }}"><i class="sub-icon bx bx-box'></i><span>Products</span></a>
                </li>
            </ul>
        </li>
        @endif

        @if(Auth::user()->role_id == 3)
        <li class="menu-section"><span>My Shop</span></li>
        <li class="{{ $is('categories/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-category-alt'></i></div>
                <div class="menu-title">Category</div>
            </a>
            <ul class="{{ $is('categories/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('categories/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('categories/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Category List</span></a>
                </li>
                <li class="{{ $is('categories/add') ? 'mm-active' : '' }}">
                    <a href="{{ url('categories/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Category</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('subcategories/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-list-check'></i></div>
                <div class="menu-title">Subcategory</div>
            </a>
            <ul class="{{ $is('subcategories/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('subcategories/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('subcategories/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Subcategory List</span></a>
                </li>
                <li class="{{ $is('subcategories/add') ? 'mm-active' : '' }}">
                    <a href="{{ url('subcategories/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Subcategory</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('portion-masters/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-food-menu'></i></div>
                <div class="menu-title">Portion Master</div>
            </a>
            <ul class="{{ $is('portion-masters/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('portion-masters/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('portion-masters/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Portion List</span></a>
                </li>
                <li class="{{ $is('portion-masters/add') ? 'mm-active' : '' }}">
                    <a href="{{ url('portion-masters/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Portion</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('products/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-box'></i></div>
                <div class="menu-title">Products</div>
            </a>
            <ul class="{{ $is('products/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('products/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('products/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Product List</span></a>
                </li>
                <li class="{{ $is('products/add') ? 'mm-active' : '' }}">
                    <a href="{{ url('products/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Product</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is(['inventory/*', 'expenses/*']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-wallet-alt'></i></div>
                <div class="menu-title">Inventory / Expenses</div>
            </a>
            <ul class="{{ $is(['inventory/*', 'expenses/*']) ? 'mm-show' : '' }}">
                <li class="{{ $is('inventory/*') ? 'mm-active' : '' }}">
                    <a href="{{ url('inventory/all') }}"><i class="sub-icon bx bx-archive-in"></i><span>Inventory List</span></a>
                </li>
                <li class="{{ $is('expenses/*') ? 'mm-active' : '' }}">
                    <a href="{{ url('expenses/all') }}"><i class="sub-icon bx bx-money'></i><span>Expense List</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('invoices/*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-receipt'></i></div>
                <div class="menu-title">Invoice</div>
            </a>
            <ul class="{{ $is('invoices/*') ? 'mm-show' : '' }}">
                <li class="{{ $is('invoices/all') ? 'mm-active' : '' }}">
                    <a href="{{ url('invoices/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Invoice List</span></a>
                </li>
            </ul>
        </li>
        @endif

        @if(Auth::user()->role_id == 1)
        <li class="menu-section"><span>Website</span></li>
        <li class="{{ $is('website*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-globe'></i></div>
                <div class="menu-title">Website Content</div>
            </a>
            <ul class="{{ $is('website*') ? 'mm-show' : '' }}">
                <li class="{{ $path === 'website' ? 'mm-active' : '' }}">
                    <a href="{{ url('website') }}"><i class="sub-icon bx bx-grid-alt"></i><span>Content Hub</span></a>
                </li>
                <li class="{{ $is('website/privacy') ? 'mm-active' : '' }}">
                    <a href="{{ url('website/privacy') }}"><i class="sub-icon bx bx-file"></i><span>Privacy Policy</span></a>
                </li>
                <li class="{{ $is('website/about') ? 'mm-active' : '' }}">
                    <a href="{{ url('website/about') }}"><i class="sub-icon bx bx-info-circle"></i><span>About Us</span></a>
                </li>
                <li class="{{ $is('website/clients') ? 'mm-active' : '' }}">
                    <a href="{{ url('website/clients') }}"><i class="sub-icon bx bx-store"></i><span>Client Showcase</span></a>
                </li>
                <li class="{{ $is('website/testimonials') ? 'mm-active' : '' }}">
                    <a href="{{ url('website/testimonials') }}"><i class="sub-icon bx bx-message-square-dots"></i><span>Testimonials</span></a>
                </li>
                <li class="{{ $is('website/contacts') ? 'mm-active' : '' }}">
                    <a href="{{ url('website/contacts') }}"><i class="sub-icon bx bx-envelope"></i><span>Contact Enquiries</span></a>
                </li>
            </ul>
        </li>
        @endif

        <li class="menu-section"><span>Account</span></li>
        <li class="{{ $is('settings*') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-cog'></i></div>
                <div class="menu-title">Settings</div>
            </a>
            <ul class="{{ $is('settings*') ? 'mm-show' : '' }}">
                <li class="{{ $path === 'settings' ? 'mm-active' : '' }}">
                    <a href="{{ url('settings') }}"><i class="sub-icon bx bx-grid-alt"></i><span>Settings Hub</span></a>
                </li>
                <li class="{{ $is('settings/profile') ? 'mm-active' : '' }}">
                    <a href="{{ url('settings/profile') }}"><i class="sub-icon bx bx-user"></i><span>Profile Update</span></a>
                </li>
                <li class="{{ $is('settings/password') ? 'mm-active' : '' }}">
                    <a href="{{ url('settings/password') }}"><i class="sub-icon bx bx-lock-alt"></i><span>Change Password</span></a>
                </li>
                @if(Auth::user()->role_id == 1)
                <li class="{{ $is('settings/logo') ? 'mm-active' : '' }}">
                    <a href="{{ url('settings/logo') }}"><i class="sub-icon bx bx-image"></i><span>Logo Update</span></a>
                </li>
                <li class="{{ $is('settings/favicon') ? 'mm-active' : '' }}">
                    <a href="{{ url('settings/favicon') }}"><i class="sub-icon bx bx-star"></i><span>Favicon Update</span></a>
                </li>
                @endif
            </ul>
        </li>

        <li class="pb-logout-item">
            <a href="javascript:;" onclick="event.preventDefault(); document.getElementById('sidebar-logout-form').submit();">
                <div class="parent-icon"><i class='bx bx-log-out-circle'></i></div>
                <div class="menu-title">Logout</div>
            </a>
        </li>
    </ul>
    <form id="sidebar-logout-form" action="{{ route('logout') }}" method="POST" style="display:none;">
        {{ csrf_field() }}
    </form>
</div>
