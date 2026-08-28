@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h5 class="dash-hello mb-0">POS Monitoring</h5>
                <p class="text-secondary mb-0">Live device status · online if active in last {{ $onlineMinutes ?? 15 }} minutes</p>
            </div>
            <a href="{{ url('reports/devices') }}" class="btn btn-outline-primary btn-sm">Device report</a>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-md-3 col-6"><div class="kpi-card kpi-blue"><span class="kpi-label">Total Bound</span><span class="kpi-value">{{ number_format($report['totalDevices']) }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-green"><span class="kpi-icon green"><i class='bx bx-wifi'></i></span><span class="kpi-label">Online Now</span><span class="kpi-value">{{ number_format($report['onlineDevices'] ?? 0) }}</span><span class="kpi-trend up">{{ $report['onlinePercent'] ?? 0 }}% live</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-orange"><span class="kpi-label">Active License</span><span class="kpi-value">{{ number_format($report['activeDevices']) }}</span></div></div>
            <div class="col-md-3 col-6"><div class="kpi-card kpi-red"><span class="kpi-label">Offline</span><span class="kpi-value">{{ number_format($report['offlineDevices'] ?? max(0, $report['totalDevices'] - ($report['onlineDevices'] ?? 0))) }}</span></div></div>
        </div>
        <div class="row g-3 mb-3">
            <div class="col-lg-4">
                <div class="card h-100"><div class="card-body">
                    <h6 class="section-title">Live vs offline</h6>
                    <div class="donut-wrap">
                        <canvas id="deviceStatus"></canvas>
                        <div class="donut-center">
                            <small>Online</small>
                            <strong>{{ number_format($report['onlineDevices'] ?? 0) }}</strong>
                        </div>
                    </div>
                </div></div>
            </div>
            <div class="col-lg-8">
                <div class="card h-100"><div class="card-body table-responsive">
                    <table class="table">
                        <thead><tr><th>Shop</th><th>Device</th><th>Last seen</th><th>License</th><th>Expiry</th><th>Live</th><th>License</th></tr></thead>
                        <tbody>
                        @forelse($devices as $d)
                            @php
                                $live = strtoupper($d->connectionStatus ?? 'OFFLINE') === 'ONLINE';
                                $licSt = $d->licenseDisplayStatus ?? 'Pending';
                            @endphp
                            <tr>
                                <td>
                                    <a href="{{ url('customers/edit/'.$d->userId) }}">{{ $d->shopName }}</a>
                                    <br><small class="text-secondary">{{ $d->ownerName }} · {{ $d->contact_number }}</small>
                                </td>
                                <td>
                                    <strong>{{ $d->android_device_name ?: '—' }}</strong>
                                    <br><small class="text-secondary">{{ $d->android_device_id }}</small>
                                </td>
                                <td>
                                    <span title="{{ $d->lastSeenAt ?: '—' }}">{{ $d->lastSeenLabel ?? 'Never' }}</span>
                                    @if(!empty($d->lastLoginAt))
                                    <br><small class="text-secondary">Login: {{ $d->lastLoginAt }}</small>
                                    @endif
                                </td>
                                <td><small>{{ $d->licenseKey }}</small></td>
                                <td>{{ $d->expiryDate }}</td>
                                <td>
                                    <span class="presence-badge {{ $live ? 'presence-online' : 'presence-offline' }}">
                                        <i class='bx {{ $live ? 'bx-wifi' : 'bx-wifi-off' }}'></i>
                                        {{ $live ? 'Online' : 'Offline' }}
                                    </span>
                                </td>
                                <td><span class="status-badge status-{{ strtolower($licSt) }}">{{ $licSt }}</span></td>
                            </tr>
                        @empty
                            <tr><td colspan="7">
                                @include('layouts.empty-state', [
                                    'compact' => true,
                                    'title' => 'No devices bound',
                                    'subtitle' => 'POS devices appear here when customers bind their Android app to a license.',
                                    'actionUrl' => url('reports/devices'),
                                    'actionLabel' => 'View Device Report',
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
PB.donut('deviceStatus', ['Online', 'Offline'], [
    {{ (int) ($report['onlineDevices'] ?? 0) }},
    {{ (int) ($report['offlineDevices'] ?? max(0, ($report['totalDevices'] ?? 0) - ($report['onlineDevices'] ?? 0))) }}
], ['#16a34a', '#94a3b8']);
</script>
@endpush
