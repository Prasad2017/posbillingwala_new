@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h5 class="dash-hello mb-0">Crash &amp; Error Logs</h5>
                <p class="text-secondary mb-0">POS crashes (NPE, ANR, native), API, printer, and database failures</p>
            </div>
            <a href="{{ url('crashes') }}" class="btn btn-outline-primary btn-sm">Crash logs</a>
        </div>
        <div class="card"><div class="card-body table-responsive">
            <table class="table">
                <thead><tr><th>Summary</th><th>Type</th><th>Exception</th><th>Severity</th><th>Shop</th><th>Seen</th><th>Count</th></tr></thead>
                <tbody>
                @forelse($logs as $log)
                    <tr>
                        <td><a href="{{ url('crashes/errors/'.$log->id) }}">{{ $log->summary ?: $log->original_exception_class }}</a></td>
                        <td>{{ $log->error_type }}</td>
                        <td><small class="text-secondary">{{ $log->original_exception_class ?: '—' }}</small></td>
                        <td><span class="status-badge {{ in_array(strtoupper($log->severity), ['ERROR','CRITICAL']) ? 'status-expired' : 'status-trial' }}">{{ $log->severity }}</span></td>
                        <td>{{ $log->shop_name ?: '—' }}</td>
                        <td>{{ $log->last_seen_at }}</td>
                        <td>{{ $log->occurrence_count }}</td>
                    </tr>
                @empty
                    <tr><td colspan="7">
                        @include('layouts.empty-state', [
                            'compact' => true,
                            'title' => 'No crash or error logs',
                            'subtitle' => 'Crashes (including null pointer exceptions), API failures, printer, and database errors from POS apps appear here.',
                        ])
                    </td></tr>
                @endforelse
                </tbody>
            </table>
        </div></div>
    </div>
</div>
@endsection
