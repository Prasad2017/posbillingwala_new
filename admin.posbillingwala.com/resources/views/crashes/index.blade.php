@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h5 class="dash-hello mb-0">Crash Logs</h5>
                <p class="text-secondary mb-0">POS crashes reported to admin</p>
            </div>
            <div class="d-flex gap-2">
                <a href="{{ url('crashes/errors') }}" class="btn btn-outline-primary btn-sm">API errors ({{ $errorCount }})</a>
                <a href="{{ url('crashes/analytics') }}" class="btn btn-primary btn-sm">Analytics</a>
            </div>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-md-4"><div class="kpi-card kpi-red"><span class="kpi-label">Total crashes</span><span class="kpi-value">{{ number_format($total) }}</span></div></div>
            <div class="col-md-4"><div class="kpi-card kpi-orange"><span class="kpi-label">Affected users</span><span class="kpi-value">{{ number_format($affected) }}</span></div></div>
            <div class="col-md-4"><div class="kpi-card kpi-green"><span class="kpi-label">Resolved</span><span class="kpi-value">{{ number_format($resolved) }}</span></div></div>
        </div>
        <form class="row g-2 mb-3" method="get">
            <div class="col-md-4"><input class="form-control" name="q" value="{{ $q }}" placeholder="Search title or class"></div>
            <div class="col-md-3">
                <select class="form-control" name="app">
                    <option value="">All apps</option>
                    @foreach(['POS App','Dealer App','User App','Admin App'] as $a)
                        <option value="{{ $a }}" @if($app===$a) selected @endif>{{ $a }}</option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-3">
                <select class="form-control" name="status">
                    <option value="">All status</option>
                    @foreach(['New','Investigating','Resolved'] as $s)
                        <option value="{{ $s }}" @if($status===$s) selected @endif>{{ $s }}</option>
                    @endforeach
                </select>
            </div>
            <div class="col-md-2"><button class="btn btn-primary w-100">Filter</button></div>
        </form>
        <div class="card"><div class="card-body table-responsive">
            <table class="table">
                <thead><tr><th>Error</th><th>App</th><th>Device</th><th>User</th><th>Count</th><th>Status</th></tr></thead>
                <tbody>
                @forelse($crashes as $c)
                    <tr>
                        <td><a href="{{ url('crashes/'.$c->id) }}">{{ $c->error_title }}</a><br><small class="text-secondary">{{ $c->error_class }}</small></td>
                        <td>{{ $c->app_name }}</td>
                        <td>{{ $c->device_name }} · {{ $c->android_version }}</td>
                        <td>{{ $c->user_name }}</td>
                        <td>{{ $c->occurrences }}</td>
                        <td><span class="status-badge {{ strtolower($c->status)==='resolved' ? 'status-active' : (strtolower($c->status)==='investigating' ? 'status-trial' : 'status-expired') }}">{{ $c->status }}</span></td>
                    </tr>
                @empty
                    <tr><td colspan="6">
                        @include('layouts.empty-state', [
                            'compact' => true,
                            'title' => 'No crash logs yet',
                            'subtitle' => 'Crash reports from POS apps will appear here automatically.',
                        ])
                    </td></tr>
                @endforelse
                </tbody>
            </table>
        </div></div>
    </div>
</div>
@endsection
