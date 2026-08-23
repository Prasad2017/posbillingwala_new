<div class="sidebar-wrapper" data-simplebar="true">
            <div class="sidebar-header">
                <div class="d-flex align-items-center gap-2">
                    <img src="{{url('assets/images/play-store-icon.png')}}" class="logo-icon" alt="POS Billingwala" style="width:36px;height:36px;border-radius:9px;">
                    <h4 class="logo-text mb-0">POS Billingwala</h4>
                </div>
                <div class="toggle-icon ms-auto"><i class='bx bx-arrow-to-left'></i>
                </div>
            </div>
            <ul class="metismenu" id="menu">
                <li>
                    <a href="{{url('home')}}">
                        <div class="parent-icon"><i class='bx bx-home-circle'></i></div>
                        <div class="menu-title">Dashboard</div>
                    </a>
                </li>

                @if(Auth::user()->role_id==1)
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class='bx bx-user-circle'></i></div>
                        <div class="menu-title">Dealers</div>
                    </a>
                    <ul>
                        <li><a href="{{url('dealer/all')}}"><i class="bx bx-right-arrow-alt"></i>Dealers List</a></li>
                        <li><a href="{{url('dealer/add')}}"><i class="bx bx-right-arrow-alt"></i>Add Dealer</a></li>
                    </ul>
                </li>
                @endif

                @if(Auth::user()->role_id==1 || Auth::user()->role_id==2)
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class='bx bx-group'></i></div>
                        <div class="menu-title">Customers</div>
                    </a>
                    <ul>
                        <li><a href="{{url('customers/all')}}"><i class="bx bx-right-arrow-alt"></i>Customer List</a></li>
                        <li><a href="{{url('customers/add')}}"><i class="bx bx-right-arrow-alt"></i>New Customer</a></li>
                    </ul>
                </li>
                <li>
                    <a href="{{url('product-import')}}">
                        <div class="parent-icon"><i class='bx bx-import'></i></div>
                        <div class="menu-title">Product Import</div>
                    </a>
                </li>
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class='bx bx-category'></i></div>
                        <div class="menu-title">Catalog</div>
                    </a>
                    <ul>
                        <li><a href="{{url('categories/all')}}"><i class="bx bx-right-arrow-alt"></i>Categories</a></li>
                        <li><a href="{{url('products/all')}}"><i class="bx bx-right-arrow-alt"></i>Products</a></li>
                    </ul>
                </li>
                @endif

                @if(Auth::user()->role_id==3)
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class='bx bx-category'></i></div>
                        <div class="menu-title">Category</div>
                    </a>
                    <ul>
                        <li><a href="{{url('categories/all')}}"><i class="bx bx-right-arrow-alt"></i>Category List</a></li>
                        <li><a href="{{url('categories/add')}}"><i class="bx bx-right-arrow-alt"></i>Add Category</a></li>
                    </ul>
                </li>
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class="bx bx-message-square-edit"></i></div>
                        <div class="menu-title">Products</div>
                    </a>
                    <ul>
                        <li><a href="{{url('products/all')}}"><i class="bx bx-right-arrow-alt"></i>Product List</a></li>
                        <li><a href="{{url('products/add')}}"><i class="bx bx-right-arrow-alt"></i>Add Product</a></li>
                    </ul>
                </li>
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class='bx bx-cart'></i></div>
                        <div class="menu-title">Inventory/Expenses</div>
                    </a>
                    <ul>
                        <li><a href="{{url('inventory/all')}}"><i class="bx bx-right-arrow-alt"></i>Inventory List</a></li>
                        <li><a href="{{url('expenses/all')}}"><i class="bx bx-right-arrow-alt"></i>Expense List</a></li>
                    </ul>
                </li>
                <li>
                    <a href="javascript:;" class="has-arrow">
                        <div class="parent-icon"><i class='bx bx-file'></i></div>
                        <div class="menu-title">Invoice</div>
                    </a>
                    <ul>
                        <li><a href="{{url('invoices/all')}}"><i class="bx bx-right-arrow-alt"></i>Invoice List</a></li>
                    </ul>
                </li>
                @endif
            </ul>
        </div>
