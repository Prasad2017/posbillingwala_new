@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <h5 class="dash-hello mb-1">Crash Analytics</h5>
        <p class="text-secondary mb-3">{{ $data['periodLabel'] }} · {{ $data['totalCrashes'] }} crashes</p>
        <div class="row g-3">
            <div class="col-lg-6">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">By app</h6>
                    <div class="donut-wrap">
                        <canvas id="byApp"></canvas>
                        <div class="donut-center">
                            <small>Total</small>
                            <strong>{{ number_format($data['totalCrashes']) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-6">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Over time</h6>
                    <div class="line-wrap"><canvas id="overTime"></canvas></div>
                </div></div>
            </div>
            <div class="col-12">
                <div class="card"><div class="card-body">
                    <h6 class="section-title">Top errors</h6>
                    <div class="bar-wrap" style="height:220px"><canvas id="topErrors"></canvas></div>
                </div></div>
            </div>
        </div>
    </div>
</div>
@endsection
@push('scripts')
<script>
(function () {
    var byApp = @json($data['byApp']);
    var over = @json($data['overTime']);
    var top = @json($data['topErrors']);
    PB.donut('byApp', byApp.map(function (x) { return x.label; }), byApp.map(function (x) { return x.count; }));
    PB.line('overTime', over.map(function (x) { return x.date; }), over.map(function (x) { return x.total; }), '#2563eb', 'rgba(37, 99, 235, 0.14)');
    PB.bar('topErrors', top.map(function (x) { return x.label; }), top.map(function (x) { return x.count; }), '#7c3aed');
})();
</script>
@endpush
