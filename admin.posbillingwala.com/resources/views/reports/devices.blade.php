@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <h5 class="dash-hello mb-3">Device Reports</h5>
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-label">Total devices</span><span class="kpi-value">{{ number_format($data['totalDevices']) }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-label">Active</span><span class="kpi-value">{{ number_format($data['activeDevices']) }}</span><span class="kpi-trend">{{ $data['activePercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-label">Inactive</span><span class="kpi-value">{{ number_format($data['inactiveDevices']) }}</span><span class="kpi-trend">{{ $data['inactivePercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-red"><span class="kpi-label">Not used</span><span class="kpi-value">{{ number_format($data['notUsedDevices']) }}</span><span class="kpi-trend">{{ $data['notUsedPercent'] }}%</span></div></div>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Device status</h6>
                    <div class="donut-wrap">
                        <canvas id="deviceDonut"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($data['totalDevices']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Top customers by devices</h6>
                    <div class="bar-wrap"><canvas id="topDevices"></canvas></div>
                </div></div>
            </div>
        </div>
        <a class="btn btn-primary" href="{{ url('devices') }}">Open POS Monitoring</a>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    PB.donut('deviceDonut', ['Active', 'Inactive', 'Not used'], [
        {{ (int) $data['activeDevices'] }},
        {{ (int) $data['inactiveDevices'] }},
        {{ (int) $data['notUsedDevices'] }}
    ], ['#16a34a', '#ea580c', '#dc2626']);
    var top = @json($data['topCustomers']);
    PB.bar('topDevices',
        top.map(function (c) { return c.shopName || c.customerName; }),
        top.map(function (c) { return c.deviceCount; }),
        '#2563eb'
    );
})();
</script>
@endpush
