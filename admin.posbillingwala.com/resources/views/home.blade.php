@extends('layouts.app')
@section('content')
@php
  $trendClass = function ($label) {
      return (strpos((string)$label, '↓') !== false) ? 'down' : 'up';
  };
  $dealerLabels = collect($dealerSales['dealers'])->pluck('dealerName')->values();
  $dealerValues = collect($dealerSales['dealers'])->map(function ($d) {
      return $d['totalSales'] > 0 ? $d['totalSales'] : $d['totalCustomer'];
  })->values();
@endphp
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex flex-wrap justify-content-between align-items-end mb-3 gap-2">
            <div>
                <h4 class="dash-hello mb-0">{{ $kpis['greeting'] }}, Admin 👋</h4>
                <p class="text-secondary mb-0">{{ $kpis['todayLabel'] }}</p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <a class="quick-chip" href="{{ url('customers/add') }}"><i class='bx bx-user-plus'></i> Add Customer</a>
                <a class="quick-chip" href="{{ url('dealer/add') }}"><i class='bx bx-store'></i> Add Dealer</a>
                <a class="quick-chip" href="{{ url('customers/all') }}"><i class='bx bx-key'></i> Licenses</a>
                <a class="quick-chip" href="{{ url('reports') }}"><i class='bx bx-bar-chart-alt-2'></i> Reports</a>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-blue" href="{{ url('customers/all') }}">
                    <span class="kpi-icon blue"><i class='bx bx-group'></i></span>
                    <span class="kpi-label">Total Customers</span>
                    <span class="kpi-value">{{ number_format($kpis['totalCustomer']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['totalCustomerTrendLabel']) }}">{{ $kpis['totalCustomerTrendLabel'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-green" href="{{ url('customers/all') }}">
                    <span class="kpi-icon green"><i class='bx bx-check-circle'></i></span>
                    <span class="kpi-label">Active Customers</span>
                    <span class="kpi-value">{{ number_format($kpis['activeCustomer']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['activeCustomerTrendLabel']) }}">{{ $kpis['activeCustomerTrendLabel'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-orange" href="{{ url('customers/all') }}">
                    <span class="kpi-icon orange"><i class='bx bx-calendar'></i></span>
                    <span class="kpi-label">Trial Customers</span>
                    <span class="kpi-value">{{ number_format($kpis['trialCustomer']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['trialCustomerTrendLabel']) }}">{{ $kpis['trialCustomerTrendLabel'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-red" href="{{ url('customers/all') }}">
                    <span class="kpi-icon red"><i class='bx bx-error-circle'></i></span>
                    <span class="kpi-label">Expired Customers</span>
                    <span class="kpi-value">{{ number_format($kpis['expiredCustomer']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['expiredCustomerTrendLabel']) }}">{{ $kpis['expiredCustomerTrendLabel'] }}</span>
                </a>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-body">
                        <h6 class="section-title">Customers</h6>
                        <div class="donut-wrap">
                            <canvas id="customerDonut"></canvas>
                            <div class="donut-center">
                                <small>Total</small>
                                <strong>{{ number_format($kpis['totalCustomer']) }}</strong>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-body">
                        <h6 class="section-title">Licenses</h6>
                        <div class="donut-wrap">
                            <canvas id="licenseDonut"></canvas>
                            <div class="donut-center">
                                <small>Active</small>
                                <strong>{{ number_format($kpis['activeLicenses']) }}</strong>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center">
                            <h6 class="section-title mb-0">Dealer Sales</h6>
                            <a href="{{ url('reports/dealers') }}">View</a>
                        </div>
                        <div class="donut-wrap">
                            <canvas id="dealerDonut"></canvas>
                            <div class="donut-center">
                                <small>This month</small>
                                <strong>{{ \App\Services\AdminMetrics::rupee($dealerSales['totalSales']) }}</strong>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <h6 class="section-title mb-2">License Overview</h6>
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-green" href="{{ url('reports/licenses') }}">
                    <span class="kpi-icon green"><i class='bx bx-certification'></i></span>
                    <span class="kpi-label">Active Licenses</span>
                    <span class="kpi-value">{{ number_format($kpis['activeLicenses']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['activeLicensesTrendLabel']) }}">{{ $kpis['activeLicensesTrendLabel'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-orange" href="{{ url('reports/licenses') }}">
                    <span class="kpi-icon orange"><i class='bx bx-time-five'></i></span>
                    <span class="kpi-label">Expiring Soon</span>
                    <span class="kpi-value">{{ number_format($kpis['expiringLicenses']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['expiringLicensesTrendLabel']) }}">{{ $kpis['expiringLicensesTrendLabel'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-blue" href="{{ url('reports/licenses') }}">
                    <span class="kpi-icon blue"><i class='bx bx-calendar'></i></span>
                    <span class="kpi-label">Trial Licenses</span>
                    <span class="kpi-value">{{ number_format($kpis['trialLicenses']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['trialLicensesTrendLabel']) }}">{{ $kpis['trialLicensesTrendLabel'] }}</span>
                </a>
            </div>
            <div class="col-md-3 col-6">
                <a class="kpi-card kpi-red" href="{{ url('reports/licenses') }}">
                    <span class="kpi-icon red"><i class='bx bx-trending-down'></i></span>
                    <span class="kpi-label">Expired Licenses</span>
                    <span class="kpi-value">{{ number_format($kpis['expiredLicenses']) }}</span>
                    <span class="kpi-trend {{ $trendClass($kpis['expiredLicensesTrendLabel']) }}">{{ $kpis['expiredLicensesTrendLabel'] }}</span>
                </a>
            </div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-8">
                <div class="card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="section-title mb-0">7-day sales</h6>
                            <span class="kpi-trend {{ $trendClass($kpis['netSalesTrendLabel']) }}">{{ $kpis['netSalesTrendLabel'] }}</span>
                        </div>
                        <div class="line-wrap"><canvas id="salesLine"></canvas></div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="row g-3">
                    <div class="col-12">
                        <a class="kpi-card kpi-navy" href="{{ url('sales/overview') }}">
                            <span class="kpi-icon blue"><i class='bx bx-rupee'></i></span>
                            <span class="kpi-label">This Month Sales</span>
                            <span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($kpis['netSales']) }}</span>
                        </a>
                    </div>
                    <div class="col-12">
                        <a class="kpi-card kpi-blue" href="{{ url('sales/dashboard') }}">
                            <span class="kpi-icon blue"><i class='bx bx-line-chart'></i></span>
                            <span class="kpi-label">Today's Sales</span>
                            <span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($kpis['todaySales']) }}</span>
                            <span class="kpi-trend {{ $trendClass($kpis['todaySalesTrendLabel']) }}">{{ $kpis['todaySalesTrendLabel'] }}</span>
                        </a>
                    </div>
                    <div class="col-6">
                        <a class="kpi-card kpi-purple" href="{{ url('customers/all') }}">
                            <span class="kpi-label">Added</span>
                            <span class="kpi-value">{{ number_format($kpis['customersAddedThisMonth']) }}</span>
                        </a>
                    </div>
                    <div class="col-6">
                        <a class="kpi-card kpi-green" href="{{ url('reports/branches') }}">
                            <span class="kpi-label">Branches</span>
                            <span class="kpi-value">{{ number_format($kpis['totalBranches']) }}</span>
                        </a>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-3">
            <div class="col-lg-6">
                <div class="card mb-3">
                    <div class="card-body">
                        <h6 class="section-title mb-2">Attention Required</h6>
                        <a class="alert-row orange" href="{{ url('reports/licenses') }}">
                            <i class='bx bx-calendar text-warning fs-4'></i>
                            <span>{{ $kpis['expiringLicenses7Days'] }} licenses expire within 7 days</span>
                        </a>
                        <a class="alert-row red" href="{{ url('customers/all') }}">
                            <i class='bx bx-error-circle text-danger fs-4'></i>
                            <span>{{ $kpis['expiredLicenses'] }} customers have expired licenses</span>
                        </a>
                        <a class="alert-row blue" href="{{ url('reports/licenses') }}">
                            <i class='bx bx-group text-primary fs-4'></i>
                            <span>{{ $kpis['trialLicensesExpiringTomorrow'] }} trial licenses expire tomorrow</span>
                        </a>
                    </div>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="section-title mb-0">Recent Customers</h6>
                            <a href="{{ url('customers/all') }}">View All</a>
                        </div>
                        @forelse($recentCustomers as $c)
                        @php $st = \App\Services\AdminMetrics::licenseDisplayStatus($c); @endphp
                        <a href="{{ url('customers/edit/'.$c->id) }}" class="d-flex justify-content-between align-items-center py-2 text-decoration-none border-bottom">
                            <div>
                                <div class="fw-bold text-dark">{{ $c->shopName ?: $c->name }}</div>
                                <small class="text-secondary">{{ $c->address ?: '—' }}</small>
                            </div>
                            <span class="status-badge status-{{ strtolower($st) }}">{{ $st }}</span>
                        </a>
                        @empty
                        @include('layouts.empty-state', [
                            'title' => 'No customers yet',
                            'subtitle' => 'New customers will show up here once you add them to the system.',
                            'actionUrl' => url('customers/add'),
                            'actionLabel' => 'Add Customer',
                        ])
                        @endforelse
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
    PB.donut('customerDonut', ['Active', 'Trial', 'Expired'], [
        {{ (int) $kpis['activeCustomer'] }},
        {{ (int) $kpis['trialCustomer'] }},
        {{ (int) $kpis['expiredCustomer'] }}
    ], ['#16a34a', '#ea580c', '#dc2626']);

    PB.donut('licenseDonut', ['Active', 'Trial', 'Expiring', 'Expired'], [
        {{ (int) $kpis['activeLicenses'] }},
        {{ (int) $kpis['trialLicenses'] }},
        {{ (int) $kpis['expiringLicenses'] }},
        {{ (int) $kpis['expiredLicenses'] }}
    ], ['#16a34a', '#2563eb', '#ea580c', '#dc2626']);

    PB.donut('dealerDonut', @json($dealerLabels), @json($dealerValues));

    var spark = @json($kpis['salesSparkline']);
    var days = [];
    for (var i = spark.length - 1; i >= 0; i--) {
        var d = new Date();
        d.setDate(d.getDate() - i);
        days.push(('0' + d.getDate()).slice(-2) + ' ' + d.toLocaleString('en', { month: 'short' }));
    }
    PB.line('salesLine', days, spark, '#2563eb');
})();
</script>
@endpush
