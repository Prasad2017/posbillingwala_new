@extends('layouts.app')
@section('page_title', 'Dashboard')
@section('content')
@php
  $trendClass = function ($label) {
      return (strpos((string)$label, '↓') !== false) ? 'down' : 'up';
  };
  $isDealerDashboard = $isDealerDashboard ?? false;
  $userName = Auth::user()->name ?? ($isDealerDashboard ? 'Dealer' : 'Admin');
  $payments = $dashboard['paymentSummary']['items'] ?? [];
  $categories = $dashboard['topCategories'] ?? [];
  $categoryColors = ['#2563eb', '#16a34a', '#ea580c', '#7c3aed', '#94a3b8'];
  $selectedDate = $dashboard['selectedDate'] ?? date('Y-m-d');
  $isToday = $selectedDate === date('Y-m-d');
  $filters = $filters ?? ($dashboard['filters'] ?? ['dealer_id' => 0, 'customer_id' => 0, 'payment' => '']);
  $dealers = $dealers ?? collect();
  $customers = $customers ?? collect();
  $recentCustomers = $recentCustomers ?? [];
  $dealerSales = $dealerSales ?? ['dealers' => [], 'totalSales' => 0];
@endphp
<div class="page-wrapper">
    <div class="page-content">
        <div class="welcome-card mb-3">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div>
                    <h4 class="dash-hello mb-1">Welcome back, {{ $userName }}! 👋</h4>
                    <p class="text-secondary mb-0">
                        @if($isToday)
                            Here's what's happening with your business today.
                        @else
                            Sales snapshot for {{ $dashboard['periodLabel'] }}.
                        @endif
                    </p>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <form method="GET" action="{{ url('home') }}" id="dashboardFilterForm" class="d-flex flex-wrap align-items-end gap-2 pb-date-filter">
                        <div class="pb-date-chip pb-date-chip-input">
                            <i class='bx bx-calendar'></i>
                            <input type="date" name="date" class="pb-date-input pb-filter-auto" value="{{ $selectedDate }}" max="{{ date('Y-m-d') }}" aria-label="Select date">
                        </div>
                        @unless($isDealerDashboard)
                        <div class="pb-filter-field">
                            <label class="pb-filter-label">Dealer</label>
                            <select name="dealer_id" class="form-select form-select-sm pb-select-search pb-filter-auto" data-placeholder="All dealers">
                                <option value="">All dealers</option>
                                @foreach($dealers as $dealer)
                                <option value="{{ $dealer->id }}" @if((int)($filters['dealer_id'] ?? 0) === (int)$dealer->id) selected @endif>{{ $dealer->name }}</option>
                                @endforeach
                            </select>
                        </div>
                        @endunless
                        <div class="pb-filter-field">
                            <label class="pb-filter-label">Customer</label>
                            <select name="customer_id" class="form-select form-select-sm pb-select-search pb-filter-auto" data-placeholder="All customers">
                                <option value="">All customers</option>
                                @foreach($customers as $customer)
                                <option value="{{ $customer->id }}" @if((int)($filters['customer_id'] ?? 0) === (int)$customer->id) selected @endif>{{ $customer->name }} — {{ $customer->shopName }}</option>
                                @endforeach
                            </select>
                        </div>
                        <div class="pb-filter-field">
                            <label class="pb-filter-label">Payment</label>
                            <select name="payment" class="form-select form-select-sm pb-filter-auto">
                                <option value="" @if(empty($filters['payment'])) selected @endif>All</option>
                                <option value="cash" @if(($filters['payment'] ?? '') === 'cash') selected @endif>Cash</option>
                                <option value="online" @if(($filters['payment'] ?? '') === 'online') selected @endif>Online (UPI/Bank/Card)</option>
                            </select>
                        </div>
                        @if(!$isToday || (!$isDealerDashboard && !empty($filters['dealer_id'])) || !empty($filters['customer_id']) || !empty($filters['payment']))
                        <a href="{{ url('home') }}" class="btn btn-outline-primary btn-sm">Reset</a>
                        @endif
                    </form>
                    <a href="{{ request()->fullUrl() }}" class="btn btn-primary btn-sm pb-refresh-btn">
                        <i class='bx bx-refresh'></i> Refresh
                    </a>
                </div>
            </div>
        </div>

        <div class="row g-3 mb-3">
            @unless($isDealerDashboard)
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-blue" href="{{ url('dealer/all') }}">
                    <span class="kpi-icon blue"><i class='bx bx-store-alt'></i></span>
                    <span class="kpi-label">Total Dealers</span>
                    <span class="kpi-value">{{ number_format($dashboard['totalDealers'] ?? 0) }}</span>
                </a>
            </div>
            @endunless
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-purple" href="{{ url('customers/all') }}">
                    <span class="kpi-icon purple"><i class='bx bx-group'></i></span>
                    <span class="kpi-label">Total Customers</span>
                    <span class="kpi-value">{{ number_format($dashboard['totalCustomers']) }}</span>
                    <span class="kpi-trend {{ $trendClass($dashboard['totalCustomersTrend']) }}">{{ $dashboard['totalCustomersTrend'] }}</span>
                </a>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-blue" href="{{ $isDealerDashboard ? url('home') : url('sales/dashboard') }}">
                    <span class="kpi-icon blue"><i class='bx bx-shopping-bag'></i></span>
                    <span class="kpi-label">Total Sales</span>
                    <span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($dashboard['totalSales']) }}</span>
                    <span class="kpi-trend {{ $trendClass($dashboard['totalSalesTrend']) }}">{{ $dashboard['totalSalesTrend'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-green" href="{{ $isDealerDashboard ? url('home') : url('sales/invoices').'?date='.$selectedDate.'&dealer_id='.($filters['dealer_id'] ?? '').'&customer_id='.($filters['customer_id'] ?? '').'&payment='.($filters['payment'] ?? '') }}">
                    <span class="kpi-icon green"><i class='bx bx-receipt'></i></span>
                    <span class="kpi-label">Total Bills</span>
                    <span class="kpi-value">{{ number_format($dashboard['totalBills']) }}</span>
                    <span class="kpi-trend {{ $trendClass($dashboard['totalBillsTrend']) }}">{{ $dashboard['totalBillsTrend'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-orange" href="{{ $isDealerDashboard ? url('home') : url('sales/overview') }}">
                    <span class="kpi-icon orange"><i class='bx bx-package'></i></span>
                    <span class="kpi-label">Total Items Sold</span>
                    <span class="kpi-value">{{ number_format($dashboard['itemsSold']) }}</span>
                    <span class="kpi-trend {{ $trendClass($dashboard['itemsSoldTrend']) }}">{{ $dashboard['itemsSoldTrend'] }}</span>
                </a>
            </div>
        </div>

        <div class="row g-3 mb-3">
            @unless($isDealerDashboard)
            <div class="col-lg-6">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h6 class="section-title mb-0">Dealers</h6>
                            <a href="{{ url('dealer/all') }}" class="btn btn-outline-primary btn-sm">View All</a>
                        </div>
                        <div class="pb-dash-list">
                            @forelse(($dealerSales['dealers'] ?? []) as $dealerRow)
                            <a class="pb-dash-row" href="{{ url('dealer/edit/'.$dealerRow['dealerId']) }}">
                                <span class="pb-customer-avatar">{{ strtoupper(substr($dealerRow['dealerName'] ?? 'D', 0, 1)) }}</span>
                                <div class="pb-dash-meta">
                                    <strong>{{ $dealerRow['dealerName'] }}</strong>
                                    <small>{{ number_format($dealerRow['totalCustomer']) }} customers · {{ number_format($dealerRow['activeLicenses']) }} active licenses</small>
                                </div>
                                <span class="pb-dash-amount">{{ \App\Services\AdminMetrics::rupee($dealerRow['totalSales']) }}</span>
                            </a>
                            @empty
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No dealers yet',
                                    'subtitle' => 'Add dealers to see them here.',
                                    'actionUrl' => url('dealer/add'),
                                    'actionLabel' => 'Add Dealer',
                                ])
                            @endforelse
                        </div>
                    </div>
                </div>
            </div>
            @endunless
            <div class="{{ $isDealerDashboard ? 'col-lg-12' : 'col-lg-6' }}">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h6 class="section-title mb-0">Customers</h6>
                            <a href="{{ url('customers/all') }}" class="btn btn-outline-primary btn-sm">View All</a>
                        </div>
                        <div class="pb-dash-list">
                            @forelse($recentCustomers as $cust)
                            @php
                                $custName = $cust->name ?: ($cust->shopName ?: 'Customer');
                                $custStatus = !empty($cust->licenseKey)
                                    ? \App\Services\AdminMetrics::licenseDisplayStatus($cust)
                                    : 'Pending';
                                $custStatusClass = [
                                    'Active' => 'status-active',
                                    'Trial' => 'status-trial',
                                    'Expired' => 'status-expired',
                                ][$custStatus] ?? 'status-pending';
                            @endphp
                            <a class="pb-dash-row" href="{{ url('customers/edit/'.$cust->id) }}">
                                <span class="pb-customer-avatar">{{ strtoupper(substr($custName, 0, 1)) }}</span>
                                <div class="pb-dash-meta">
                                    <strong>{{ $custName }}</strong>
                                    <small>
                                        {{ $cust->shopName && $cust->shopName !== $custName ? $cust->shopName : ($cust->dealerName ?: '—') }}
                                        @if(!empty($cust->contact_number)) · {{ $cust->contact_number }} @endif
                                    </small>
                                </div>
                                <span class="status-badge {{ $custStatusClass }}">{{ $custStatus }}</span>
                            </a>
                            @empty
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No customers yet',
                                    'subtitle' => 'New customers will appear here.',
                                    'actionUrl' => url('customers/add'),
                                    'actionLabel' => 'Add Customer',
                                ])
                            @endforelse
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-8">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="section-title mb-0">Sales Overview</h6>
                            <span class="pb-chart-filter">{{ $dashboard['chartPeriodLabel'] ?? 'Today' }}</span>
                        </div>
                        <div class="line-wrap"><canvas id="hourlySalesChart"></canvas></div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-body d-flex flex-column">
                        <h6 class="section-title">Top Selling Categories</h6>
                        <div class="donut-wrap flex-grow-1">
                            <canvas id="categoryDonut"></canvas>
                            @if(count($categories))
                            <div class="donut-center">
                                <small>Top</small>
                                <strong>{{ $categories[0]['name'] ?? '—' }}</strong>
                            </div>
                            @endif
                        </div>
                        @if(count($categories))
                        <div class="pb-category-legend mt-2">
                            @foreach($categories as $i => $cat)
                            <div class="pb-legend-row">
                                <span class="pb-legend-dot" style="background:{{ $categoryColors[$i % count($categoryColors)] }}"></span>
                                <span class="pb-legend-name">{{ $cat['name'] }}</span>
                                <span class="pb-legend-pct">{{ $cat['percent'] }}%</span>
                            </div>
                            @endforeach
                        </div>
                        @endif
                        @unless($isDealerDashboard)
                        <a href="{{ url('reports') }}" class="pb-view-report-link mt-2">View Full Report</a>
                        @endunless
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3">
            <div class="col-lg-8">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h6 class="section-title mb-0">Recent Transactions</h6>
                            @unless($isDealerDashboard)
                            <a href="{{ url('sales/invoices') }}?date={{ $selectedDate }}&dealer_id={{ $filters['dealer_id'] ?? '' }}&customer_id={{ $filters['customer_id'] ?? '' }}&payment={{ $filters['payment'] ?? '' }}" class="btn btn-outline-primary btn-sm">View All</a>
                            @endunless
                        </div>
                        <div class="table-responsive">
                            <table class="table pb-transactions-table">
                                <thead>
                                    <tr>
                                        <th>Bill No.</th>
                                        @unless($isDealerDashboard)
                                        <th>Dealer</th>
                                        @endunless
                                        <th>Customer / Shop</th>
                                        <th>License</th>
                                        <th>Amount</th>
                                        <th>Payment</th>
                                        <th>Time</th>
                                    </tr>
                                </thead>
                                <tbody>
                                @forelse($dashboard['recentInvoices'] as $inv)
                                    @php
                                        $customerLabel = $inv->shopName ?: $inv->customerName ?: 'Walk-in';
                                        $initial = strtoupper(substr($customerLabel, 0, 1));
                                        $timeLabel = $inv->invoiceDate ? \Carbon\Carbon::parse($inv->invoiceDate)->format('h:i A') : '—';
                                        $paymentLabel = \App\Services\AdminMetrics::normalizePaymentLabel($inv->paymentMode ?? '');
                                    @endphp
                                    <tr>
                                        <td>
                                            @if($isDealerDashboard)
                                            {{ $inv->invoiceNumber }}
                                            @else
                                            <a href="{{ url('sales/invoices/'.$inv->invoiceId) }}">{{ $inv->invoiceNumber }}</a>
                                            @endif
                                        </td>
                                        @unless($isDealerDashboard)
                                        <td>{{ $inv->dealerName ?: '—' }}</td>
                                        @endunless
                                        <td>
                                            <span class="pb-customer-cell">
                                                <span class="pb-customer-avatar">{{ $initial }}</span>
                                                {{ $customerLabel }}
                                            </span>
                                        </td>
                                        <td>
                                            <small class="d-block">{{ $inv->licenseKey ?: '—' }}</small>
                                            @if(!empty($inv->branchName))
                                            <small class="text-secondary">{{ $inv->branchName }}</small>
                                            @endif
                                        </td>
                                        <td>{{ \App\Services\AdminMetrics::rupee($inv->totalAmount) }}</td>
                                        <td><span class="badge bg-light text-dark">{{ $paymentLabel }}</span></td>
                                        <td>{{ $timeLabel }}</td>
                                    </tr>
                                @empty
                                    <tr><td colspan="{{ $isDealerDashboard ? '6' : '7' }}">
                                        @include('layouts.empty-state', [
                                            'compact' => true,
                                            'title' => 'No transactions for this date',
                                            'subtitle' => 'Bills from the selected day will appear here.',
                                        ])
                                    </td></tr>
                                @endforelse
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="section-title mb-0">Payment Summary</h6>
                            @if(!empty($filters['payment']))
                            <span class="pb-chart-filter">{{ $filters['payment'] === 'cash' ? 'Cash only' : 'Online only' }}</span>
                            @endif
                        </div>
                        <div class="pb-payment-list">
                            @forelse($payments as $pay)
                            <div class="pb-payment-row">
                                <span class="pb-payment-icon"><i class='bx {{ \App\Services\AdminMetrics::paymentIcon($pay['mode']) }}'></i></span>
                                <div class="pb-payment-meta flex-grow-1">
                                    <strong>{{ $pay['mode'] }}</strong>
                                    <small class="d-block text-secondary">{{ $pay['percent'] }}% of total</small>
                                </div>
                                <span class="pb-payment-amount">{{ \App\Services\AdminMetrics::rupee($pay['total']) }}</span>
                            </div>
                            @empty
                            <p class="text-secondary mb-0">No payment data for this date.</p>
                            @endforelse
                        </div>
                        @if(($dashboard['paymentSummary']['grandTotal'] ?? 0) > 0)
                        <div class="pb-payment-total">
                            <span>Total</span>
                            <strong>{{ \App\Services\AdminMetrics::rupee($dashboard['paymentSummary']['grandTotal']) }}</strong>
                        </div>
                        @endif
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    var hourly = @json($dashboard['hourlySales']);
    PB.line(
        'hourlySalesChart',
        hourly.map(function (r) { return r.label; }),
        hourly.map(function (r) { return r.total; }),
        '#2563eb'
    );

    var cats = @json($categories);
    var catLabels = cats.length ? cats.map(function (c) { return c.name; }) : ['No data'];
    var catValues = cats.length ? cats.map(function (c) { return c.percent; }) : [1];
    var catColors = @json($categoryColors);
    PB.donut('categoryDonut', catLabels, catValues, cats.length ? catColors.slice(0, catLabels.length) : ['#e5e7eb']);
})();
</script>
@endpush
