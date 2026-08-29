@extends('layouts.app')
@section('page_title', 'Dealer Reports')
@section('content')
@php
  $dealerLabels = collect($data['dealers'] ?? [])->pluck('dealerName')->values();
  $dealerValues = collect($data['dealers'] ?? [])->map(function ($d) {
      return $d['totalSales'] > 0 ? $d['totalSales'] : $d['totalCustomer'];
  })->values();
@endphp
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.page-header', [
            'title' => 'Dealer Reports',
            'subtitle' => 'Status mix and this month sales · ' . \App\Services\AdminMetrics::rupee($data['totalSales'] ?? 0),
            'actionUrl' => url('reports'),
            'actionLabel' => 'Reports Hub',
            'actionIcon' => 'bx-grid-alt',
        ])

        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-icon blue"><i class='bx bx-store-alt'></i></span><span class="kpi-label">Total</span><span class="kpi-value">{{ number_format($data['totalDealer'] ?? 0) }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-icon green"><i class='bx bx-check-circle'></i></span><span class="kpi-label">Active</span><span class="kpi-value">{{ number_format($data['activeDealer'] ?? 0) }}</span><span class="kpi-trend up">{{ $data['activePercent'] ?? 0 }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-red"><span class="kpi-icon red"><i class='bx bx-block'></i></span><span class="kpi-label">Inactive</span><span class="kpi-value">{{ number_format($data['inactiveDealer'] ?? 0) }}</span><span class="kpi-trend down">{{ $data['inactivePercent'] ?? 0 }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-icon orange"><i class='bx bx-group'></i></span><span class="kpi-label">Customers</span><span class="kpi-value">{{ number_format($data['totalCustomers'] ?? 0) }}</span></div></div>
        </div>

        <div class="row g-3 mb-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Status mix</h6>
                    <div class="donut-wrap">
                        <canvas id="statusMix"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($data['totalDealer'] ?? 0) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">New dealers (7 days)</h6>
                    <div class="bar-wrap"><canvas id="growth"></canvas></div>
                </div></div>
            </div>
        </div>

        <div class="row g-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Sales mix</h6>
                    <div class="donut-wrap">
                        <canvas id="mix"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ \App\Services\AdminMetrics::rupee($data['totalSales'] ?? 0) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body table-responsive">
                    <h6 class="section-title">Dealer performance</h6>
                    <table class="table pb-transactions-table">
                        <thead><tr><th>Dealer</th><th>Customers</th><th>Active licenses</th><th>Sales</th></tr></thead>
                        <tbody>
                        @forelse($data['dealers'] ?? [] as $d)
                            <tr>
                                <td><a href="{{ url('dealer/edit/'.$d['dealerId']) }}">{{ $d['dealerName'] }}</a></td>
                                <td>{{ $d['totalCustomer'] }}</td>
                                <td>{{ $d['activeLicenses'] }}</td>
                                <td>{{ \App\Services\AdminMetrics::rupee($d['totalSales']) }}</td>
                            </tr>
                        @empty
                            <tr><td colspan="4">
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No dealer data',
                                    'subtitle' => 'Dealer performance will show once dealers onboard customers.',
                                ])
                            </td></tr>
                        @endforelse
                        </tbody>
                    </table>
                </div></div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    PB.donut('statusMix', ['Active', 'Inactive'], [
        {{ (int) ($data['activeDealer'] ?? 0) }},
        {{ (int) ($data['inactiveDealer'] ?? 0) }}
    ], ['#16a34a', '#6b7280']);
    var g = @json($data['growthBars'] ?? []);
    PB.bar('growth', g.map(function (x) { return x.label; }), g.map(function (x) { return x.count; }), '#f59e0b');
    PB.donut('mix', @json($dealerLabels), @json($dealerValues));
})();
</script>
@endpush
