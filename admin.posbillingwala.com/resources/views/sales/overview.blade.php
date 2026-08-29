@extends('layouts.app')
@section('page_title', 'Sales Overview')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="pb-page-header mb-3">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div>
                    <h4 class="dash-hello mb-1">Sales Overview</h4>
                    <p class="text-secondary mb-0">{{ $data['periodLabel'] }}</p>
                </div>
                <form class="d-flex gap-2" method="get">
                    <input type="month" name="month" value="{{ $data['month'] }}" class="form-control pb-field">
                    <button class="btn btn-primary btn-sm">Go</button>
                </form>
            </div>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-icon blue"><i class='bx bx-rupee'></i></span><span class="kpi-label">Total Sales</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($data['totalSales']) }}</span><span class="kpi-trend up">{{ $data['totalSalesTrend'] }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-icon green"><i class='bx bx-trending-up'></i></span><span class="kpi-label">Net Sales</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($data['netSales']) }}</span><span class="kpi-trend up">{{ $data['netSalesTrend'] }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-icon orange"><i class='bx bx-receipt'></i></span><span class="kpi-label">Invoices</span><span class="kpi-value">{{ number_format($data['totalInvoices']) }}</span><span class="kpi-trend up">{{ $data['invoicesTrend'] }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-purple"><span class="kpi-icon purple"><i class='bx bx-calculator'></i></span><span class="kpi-label">Avg Bill</span><span class="kpi-value">{{ \App\Services\AdminMetrics::rupee($data['avgBill']) }}</span><span class="kpi-trend up">{{ $data['avgBillTrend'] }}</span></div></div>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-lg-8">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Sales trend</h6>
                    <div class="bar-wrap"><canvas id="monthTrend"></canvas></div>
                </div></div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Top customers</h6>
                    <div class="donut-wrap">
                        <canvas id="topDonut"></canvas>
                        <div class="donut-center">
                            <small>Month</small>
                            <strong>{{ \App\Services\AdminMetrics::rupee($data['totalSales']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
        </div>
        <div class="card">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h6 class="section-title mb-0">Top customers</h6>
                </div>
                <div class="table-responsive">
                    <table class="table pb-transactions-table">
                        <thead><tr><th>Customer</th><th>Shop</th><th>Sales</th></tr></thead>
                        <tbody>
                        @forelse($data['topCustomers'] as $c)
                            <tr>
                                <td><a href="{{ url('customers/edit/'.$c->customerId) }}">{{ $c->customerName }}</a></td>
                                <td>{{ $c->shopName }}</td>
                                <td>{{ \App\Services\AdminMetrics::rupee($c->totalSales) }}</td>
                            </tr>
                        @empty
                            <tr><td colspan="3">
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No sales this month',
                                    'subtitle' => 'Top customers by sales will appear when invoices are recorded.',
                                ])
                            </td></tr>
                        @endforelse
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    var rows = @json($data['salesTrend']);
    var top = @json($data['topCustomers']);
    PB.bar('monthTrend', rows.map(function (r) { return r.date; }), rows.map(function (r) { return r.total; }), '#2563eb');
    PB.donut('topDonut',
        top.map(function (c) { return c.customerName || c.shopName; }),
        top.map(function (c) { return c.totalSales; })
    );
})();
</script>
@endpush
