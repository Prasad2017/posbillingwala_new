@php
    $path = trim(request()->path(), '/');
    $is = function ($patterns) use ($path) {
        foreach ((array) $patterns as $p) {
            $p = trim($p, '/');
            if ($p === '' && $path === '') {
                return true;
            }
            if ($p === '') {
                continue;
            }
            if (str_contains($p, '*')) {
                $regex = '#^' . str_replace('\*', '.*', preg_quote($p, '#')) . '$#';
                if (preg_match($regex, $path)) {
                    return true;
                }
                continue;
            }
            if ($path === $p || str_starts_with($path, $p . '/')) {
                return true;
            }
        }
        return false;
    };
    $navCurrent = function ($patterns) use ($path) {
        foreach ((array) $patterns as $p) {
            $p = trim($p, '/');
            if ($p !== '' && ($path === $p || str_starts_with($path, $p . '/'))) {
                return true;
            }
        }
        return false;
    };
    $roleId = Auth::user()->role_id;
@endphp
<div class="sidebar-wrapper" data-simplebar="true">
    <div class="sidebar-header">
        <div class="d-flex align-items-center gap-2">
            <img src="{{ $adminLogoUrl ?? asset('assets/images/app_logo.png') }}" class="logo-icon" alt="POS Billingwala">
        </div>
        <div class="toggle-icon ms-auto"><i class='bx bx-chevrons-left'></i></div>
    </div>

    <ul class="metismenu pb-nav" id="menu">

        <li class="{{ $navCurrent('home') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('home') }}">
                <div class="parent-icon"><i class='bx bx-grid-alt'></i></div>
                <div class="menu-title">Dashboard</div>
            </a>
        </li>

        @if($roleId == 1)
        <li class="{{ $is(['sales/dashboard', 'sales/overview']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-trending-up'></i></div>
                <div class="menu-title">Sales</div>
            </a>
            <ul class="{{ $is(['sales/dashboard', 'sales/overview']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('sales/dashboard') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('sales/dashboard') }}"><i class="sub-icon bx bx-pie-chart-alt-2"></i><span>Sales Dashboard</span></a>
                </li>
                <li class="{{ $navCurrent('sales/overview') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('sales/overview') }}"><i class="sub-icon bx bx-line-chart"></i><span>Sales Overview</span></a>
                </li>
                <li>
                    <a href="{{ url('sales/invoices') }}"><i class="sub-icon bx bx-receipt"></i><span>Recent Invoices</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $navCurrent('sales/invoices') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('sales/invoices') }}">
                <div class="parent-icon"><i class='bx bx-transfer-alt'></i></div>
                <div class="menu-title">Transactions</div>
            </a>
        </li>

        <li class="{{ $is('products') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-box'></i></div>
                <div class="menu-title">Products</div>
            </a>
            <ul class="{{ $is('products') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('products/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('products/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Product List</span></a>
                </li>
                <li class="{{ $navCurrent('products/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('products/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Product</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is(['categories', 'subcategories', 'portion-masters']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-category-alt'></i></div>
                <div class="menu-title">Catalog</div>
            </a>
            <ul class="{{ $is(['categories', 'subcategories', 'portion-masters']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $is('categories') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('categories/all') }}"><i class="sub-icon bx bx-category-alt"></i><span>Categories</span></a>
                </li>
                <li class="{{ $is('subcategories') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('subcategories/all') }}"><i class="sub-icon bx bx-list-check"></i><span>Subcategories</span></a>
                </li>
                <li class="{{ $is('portion-masters') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('portion-masters/all') }}"><i class="sub-icon bx bx-food-menu"></i><span>Portion Master</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is(['devices', 'reports/devices', 'reports/branches']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-archive-in'></i></div>
                <div class="menu-title">Inventory</div>
            </a>
            <ul class="{{ $is(['devices', 'reports/devices', 'reports/branches']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('devices') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('devices') }}"><i class="sub-icon bx bx-desktop"></i><span>POS Monitoring</span></a>
                </li>
                <li class="{{ $navCurrent('reports/branches') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('reports/branches') }}"><i class="sub-icon bx bx-sitemap"></i><span>Branch Reports</span></a>
                </li>
                <li class="{{ $navCurrent('reports/devices') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('reports/devices') }}"><i class="sub-icon bx bx-chip"></i><span>Device Reports</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is(['customers', 'dealer']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-group'></i></div>
                <div class="menu-title">Customers</div>
            </a>
            <ul class="{{ $is(['customers', 'dealer']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('customers/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('customers/all') }}"><i class="sub-icon bx bx-id-card"></i><span>All Customers</span></a>
                </li>
                <li class="{{ $navCurrent('customers/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('customers/add') }}"><i class="sub-icon bx bx-user-plus"></i><span>Add Customer</span></a>
                </li>
                <li class="{{ $navCurrent('customers/all-license') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('customers/all-license') }}"><i class="sub-icon bx bx-key"></i><span>Licenses</span></a>
                </li>
                <li class="{{ $navCurrent('dealer/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('dealer/all') }}"><i class="sub-icon bx bx-buildings"></i><span>All Dealers</span></a>
                </li>
                <li class="{{ $navCurrent('dealer/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('dealer/add') }}"><i class="sub-icon bx bx-store"></i><span>Add Dealer</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ ($path === 'reports' || $is(['reports/customers', 'reports/licenses', 'reports/dealers'])) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-bar-chart-alt-2'></i></div>
                <div class="menu-title">Reports</div>
            </a>
            <ul class="{{ ($path === 'reports' || $is(['reports/customers', 'reports/licenses', 'reports/dealers'])) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $path === 'reports' ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('reports') }}"><i class="sub-icon bx bx-grid-alt"></i><span>Reports Hub</span></a>
                </li>
                <li class="{{ $navCurrent('reports/customers') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('reports/customers') }}"><i class="sub-icon bx bx-user-check"></i><span>Customer Reports</span></a>
                </li>
                <li class="{{ $navCurrent('reports/licenses') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('reports/licenses') }}"><i class="sub-icon bx bx-key"></i><span>License Reports</span></a>
                </li>
                <li class="{{ $navCurrent('reports/dealers') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('reports/dealers') }}"><i class="sub-icon bx bx-store"></i><span>Dealer Reports</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is(['import-export', 'product-import', 'product-export', 'catalog-import-export']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-import'></i></div>
                <div class="menu-title">Import / Export</div>
            </a>
            <ul class="{{ $is(['import-export', 'product-import', 'product-export', 'catalog-import-export']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $path === 'import-export' ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('import-export') }}"><i class="sub-icon bx bx-grid-alt"></i><span>Import / Export Hub</span></a>
                </li>
                <li class="{{ $is('catalog-import-export') || $is('product-import') || $is('product-export') ? 'pb-nav-current' : '' }}">
                    <a href="{{ route('catalog-import-export.index') }}"><i class="sub-icon bx bx-transfer"></i><span>Catalog Import / Export</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $navCurrent('users') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('users') }}">
                <div class="parent-icon"><i class='bx bx-user-check'></i></div>
                <div class="menu-title">Users &amp; Roles</div>
            </a>
        </li>

        <li class="{{ $is(['settings/profile', 'settings/password']) || $path === 'settings' ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-cog'></i></div>
                <div class="menu-title">Settings</div>
            </a>
            <ul class="{{ $is(['settings/profile', 'settings/password']) || $path === 'settings' ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $path === 'settings' ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('settings') }}"><i class="sub-icon bx bx-grid-alt"></i><span>Settings Hub</span></a>
                </li>
                <li class="{{ $navCurrent('settings/profile') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('settings/profile') }}"><i class="sub-icon bx bx-user"></i><span>Profile Update</span></a>
                </li>
                <li class="{{ $navCurrent('settings/password') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('settings/password') }}"><i class="sub-icon bx bx-lock-alt"></i><span>Change Password</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is(['settings/logo', 'settings/favicon', 'website']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-slider-alt'></i></div>
                <div class="menu-title">App Settings</div>
            </a>
            <ul class="{{ $is(['settings/logo', 'settings/favicon', 'website']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('settings/logo') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('settings/logo') }}"><i class="sub-icon bx bx-image"></i><span>Logo Update</span></a>
                </li>
                <li class="{{ $navCurrent('settings/favicon') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('settings/favicon') }}"><i class="sub-icon bx bx-star"></i><span>Favicon Update</span></a>
                </li>
                <li class="{{ $path === 'website' ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('website') }}"><i class="sub-icon bx bx-globe"></i><span>Website Content</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is('crashes') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-history'></i></div>
                <div class="menu-title">Activity Log</div>
            </a>
            <ul class="{{ $is('crashes') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $path === 'crashes' ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('crashes') }}"><i class="sub-icon bx bx-bug-alt"></i><span>Crash Logs</span></a>
                </li>
                <li class="{{ $navCurrent('crashes/errors') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('crashes/errors') }}"><i class="sub-icon bx bx-cloud-lightning"></i><span>Crash &amp; Error Logs</span></a>
                </li>
                <li class="{{ $navCurrent('crashes/analytics') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('crashes/analytics') }}"><i class="sub-icon bx bx-analyse"></i><span>Crash Analytics</span></a>
                </li>
            </ul>
        </li>

        <li class="{{ $is('support') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-support'></i></div>
                <div class="menu-title">Help &amp; Support</div>
            </a>
            <ul class="{{ $is('support') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $path === 'support' ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('support') }}"><i class="sub-icon bx bx-help-circle"></i><span>Help &amp; Support</span></a>
                </li>
                <li class="{{ $is('support/tickets') && !$is('support/tickets/create') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('support/tickets') }}"><i class="sub-icon bx bx-ticket"></i><span>My Tickets</span></a>
                </li>
                <li class="{{ $navCurrent('support/tickets/create') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('support/tickets/create') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Create Ticket</span></a>
                </li>
                <li class="{{ $navCurrent('support/faq') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('support/faq') }}"><i class="sub-icon bx bx-message-rounded-dots"></i><span>FAQs</span></a>
                </li>
            </ul>
        </li>
        @endif

        @if($roleId == 2)
        <li class="{{ $is('customers') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-group'></i></div>
                <div class="menu-title">Customers</div>
            </a>
            <ul class="{{ $is('customers') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('customers/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('customers/all') }}"><i class="sub-icon bx bx-id-card"></i><span>All Customers</span></a>
                </li>
                <li class="{{ $navCurrent('customers/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('customers/add') }}"><i class="sub-icon bx bx-user-plus"></i><span>Add Customer</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('product-import') || $is('product-export') || $is('import-export') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('import-export') }}">
                <div class="parent-icon"><i class='bx bx-import'></i></div>
                <div class="menu-title">Import / Export</div>
            </a>
        </li>
        <li class="{{ $is(['categories', 'subcategories', 'portion-masters', 'products']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-package'></i></div>
                <div class="menu-title">Catalog</div>
            </a>
            <ul class="{{ $is(['categories', 'subcategories', 'portion-masters', 'products']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('categories/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('categories/all') }}"><i class="sub-icon bx bx-category-alt"></i><span>Categories</span></a>
                </li>
                <li class="{{ $navCurrent('subcategories/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('subcategories/all') }}"><i class="sub-icon bx bx-list-check"></i><span>Subcategories</span></a>
                </li>
                <li class="{{ $navCurrent('portion-masters/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('portion-masters/all') }}"><i class="sub-icon bx bx-food-menu"></i><span>Portion Master</span></a>
                </li>
                <li class="{{ $navCurrent('products/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('products/all') }}"><i class="sub-icon bx bx-box"></i><span>Products</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('settings') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('settings') }}">
                <div class="parent-icon"><i class='bx bx-cog'></i></div>
                <div class="menu-title">Settings</div>
            </a>
        </li>
        @endif

        @if($roleId == 3)
        <li class="{{ $is('categories') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-category-alt'></i></div>
                <div class="menu-title">Catalog</div>
            </a>
            <ul class="{{ $is('categories') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('categories/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('categories/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Categories</span></a>
                </li>
                <li class="{{ $navCurrent('categories/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('categories/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Category</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('subcategories') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-list-check'></i></div>
                <div class="menu-title">Subcategories</div>
            </a>
            <ul class="{{ $is('subcategories') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('subcategories/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('subcategories/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Subcategory List</span></a>
                </li>
                <li class="{{ $navCurrent('subcategories/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('subcategories/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Subcategory</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('portion-masters') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-food-menu'></i></div>
                <div class="menu-title">Portion Master</div>
            </a>
            <ul class="{{ $is('portion-masters') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('portion-masters/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('portion-masters/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Portion List</span></a>
                </li>
                <li class="{{ $navCurrent('portion-masters/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('portion-masters/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Portion</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is('products') ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-box'></i></div>
                <div class="menu-title">Products</div>
            </a>
            <ul class="{{ $is('products') ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('products/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('products/all') }}"><i class="sub-icon bx bx-list-ul"></i><span>Product List</span></a>
                </li>
                <li class="{{ $navCurrent('products/add') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('products/add') }}"><i class="sub-icon bx bx-plus-circle"></i><span>Add Product</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $is(['inventory', 'expenses']) ? 'mm-active' : '' }}">
            <a href="javascript:;" class="has-arrow">
                <div class="parent-icon"><i class='bx bx-archive-in'></i></div>
                <div class="menu-title">Inventory</div>
            </a>
            <ul class="{{ $is(['inventory', 'expenses']) ? 'mm-show mm-collapse' : 'mm-collapse' }}">
                <li class="{{ $navCurrent('inventory/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('inventory/all') }}"><i class="sub-icon bx bx-archive-in"></i><span>Inventory List</span></a>
                </li>
                <li class="{{ $navCurrent('expenses/all') ? 'pb-nav-current' : '' }}">
                    <a href="{{ url('expenses/all') }}"><i class="sub-icon bx bx-money"></i><span>Expense List</span></a>
                </li>
            </ul>
        </li>
        <li class="{{ $navCurrent('invoices/all') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('invoices/all') }}">
                <div class="parent-icon"><i class='bx bx-transfer-alt'></i></div>
                <div class="menu-title">Transactions</div>
            </a>
        </li>
        <li class="{{ $is('settings') ? 'pb-nav-current' : '' }}">
            <a href="{{ url('settings') }}">
                <div class="parent-icon"><i class='bx bx-cog'></i></div>
                <div class="menu-title">Settings</div>
            </a>
        </li>
        @endif
    </ul>

    <div class="sidebar-footer">
        <button type="button" class="sidebar-collapse-btn toggle-icon" title="Collapse sidebar">
            <i class='bx bx-chevrons-left'></i>
            <span class="sidebar-collapse-label">Collapse</span>
        </button>
    </div>

    <form id="sidebar-logout-form" action="{{ route('logout') }}" method="POST" style="display:none;">
        {{ csrf_field() }}
    </form>
</div>
