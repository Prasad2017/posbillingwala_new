@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <h5 class="dash-hello mb-3">Customer Reports</h5>
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-label">Total</span><span class="kpi-value">{{ number_format($data['totalCustomer']) }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-label">Active</span><span class="kpi-value">{{ number_format($data['activeCustomer']) }}</span><span class="kpi-trend">{{ $data['activePercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-label">Trial</span><span class="kpi-value">{{ number_format($data['trialCustomer']) }}</span><span class="kpi-trend">{{ $data['trialPercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-red"><span class="kpi-label">Expired</span><span class="kpi-value">{{ number_format($data['expiredCustomer']) }}</span><span class="kpi-trend">{{ $data['expiredPercent'] }}%</span></div></div>
        </div>
        <div class="row g-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Status mix</h6>
                    <div class="donut-wrap">
                        <canvas id="mix"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($data['totalCustomer']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">New customers (7 days)</h6>
                    <div class="bar-wrap"><canvas id="growth"></canvas></div>
                </div></div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    PB.donut('mix', ['Active', 'Trial', 'Expired'], [
        {{ (int) $data['activeCustomer'] }},
        {{ (int) $data['trialCustomer'] }},
        {{ (int) $data['expiredCustomer'] }}
    ], ['#16a34a', '#ea580c', '#dc2626']);
    var g = @json($data['growthBars']);
    PB.bar('growth', g.map(function (x) { return x.label; }), g.map(function (x) { return x.count; }), '#7c3aed');
})();
</script>
@endpush
