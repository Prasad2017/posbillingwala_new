@extends('layouts.app')
@section('page_title', 'License Reports')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.page-header', [
            'title' => 'License Reports',
            'subtitle' => 'Active, trial, expiring, and expired license breakdown.',
            'actionUrl' => url('reports'),
            'actionLabel' => 'Reports Hub',
            'actionIcon' => 'bx-grid-alt',
        ])
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-icon green"><i class='bx bx-certification'></i></span><span class="kpi-label">Active</span><span class="kpi-value">{{ number_format($data['activeLicenses']) }}</span><span class="kpi-trend up">{{ $data['activePercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-icon blue"><i class='bx bx-time-five'></i></span><span class="kpi-label">Trial</span><span class="kpi-value">{{ number_format($data['trialLicenses']) }}</span><span class="kpi-trend up">{{ $data['trialPercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-icon orange"><i class='bx bx-calendar'></i></span><span class="kpi-label">Expiring</span><span class="kpi-value">{{ number_format($data['expiringLicenses']) }}</span><span class="kpi-trend up">{{ $data['expiringPercent'] }}%</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-red"><span class="kpi-icon red"><i class='bx bx-error-circle'></i></span><span class="kpi-label">Expired</span><span class="kpi-value">{{ number_format($data['expiredLicenses']) }}</span><span class="kpi-trend down">{{ $data['expiredPercent'] }}%</span></div></div>
        </div>
        <div class="row g-3">
            <div class="col-lg-5">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">License mix</h6>
                    <div class="donut-wrap">
                        <canvas id="mix"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($data['totalLicenses']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-7">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Expiry windows</h6>
                    <div class="bar-wrap"><canvas id="expiryBars"></canvas></div>
                </div></div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    PB.donut('mix', ['Active', 'Trial', 'Expiring', 'Expired'], [
        {{ (int) $data['activeLicenses'] }},
        {{ (int) $data['trialLicenses'] }},
        {{ (int) $data['expiringLicenses'] }},
        {{ (int) $data['expiredLicenses'] }}
    ], ['#16a34a', '#2563eb', '#ea580c', '#dc2626']);
    var w = @json($data['expiryWindows']);
    PB.bar('expiryBars',
        w.map(function (x) { return x.label.split('(')[0].trim(); }),
        w.map(function (x) { return x.count; }),
        '#ea580c'
    );
})();
</script>
@endpush
