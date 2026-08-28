@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h5 class="dash-hello mb-0">Sales Dashboard</h5>
                <p class="text-secondary mb-0">{{ $data['periodLabel'] }}</p>
            </div>
            <a href="{{ url('sales/invoices') }}" class="btn btn-primary btn-sm">All invoices</a>
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
                    <h6 class="section-title">7-day sales trend</h6>
                    <div class="line-wrap"><canvas id="salesTrend"></canvas></div>
                </div></div>
            </div>
            <div class="col-lg-4">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Today vs yesterday</h6>
                    <div class="donut-wrap">
                        <canvas id="todayDonut"></canvas>
                        <div class="donut-center">
                            <small>Today</small>
                            <strong>{{ \App\Services\AdminMetrics::rupee($data['totalSales']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
        </div>
        <div class="card">
            <div class="card-body">
                <h6 class="section-title">Recent invoices</h6>
                <div class="table-responsive">
                    <table class="table">
                        <thead><tr><th>Invoice</th><th>Shop</th><th>Date</th><th>Amount</th><th></th></tr></thead>
                        <tbody>
                        @forelse($data['recentInvoices'] as $inv)
                            <tr>
                                <td>{{ $inv->invoiceNumber }}</td>
                                <td>{{ $inv->shopName ?: $inv->customerName }}</td>
                                <td>{{ $inv->invoiceDate }}</td>
                                <td>{{ \App\Services\AdminMetrics::rupee($inv->totalAmount) }}</td>
                                <td><a href="{{ url('sales/invoices/'.$inv->invoiceId) }}">View</a></td>
                            </tr>
                        @empty
                            <tr><td colspan="5">
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No invoices today',
                                    'subtitle' => 'Sales invoices for today will appear here once customers start billing.',
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
    PB.line('salesTrend', rows.map(function (r) { return r.date; }), rows.map(function (r) { return r.total; }), '#2563eb');
    var today = {{ (float) $data['totalSales'] }};
    var invoices = {{ (int) $data['totalInvoices'] }};
    PB.donut('todayDonut', ['Sales', 'Bills'], [today || invoices, invoices], ['#2563eb', '#16a34a']);
})();
</script>
@endpush
