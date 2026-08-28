@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <a href="{{ url('crashes/errors') }}" class="mb-3 d-inline-block">&larr; Back to error logs</a>
        <div class="card mb-3"><div class="card-body">
            <h5 class="dash-hello">{{ $log->summary ?: $log->original_exception_class }}</h5>
            <p class="mb-1">{{ $log->error_type }} · {{ $log->severity }} · {{ $log->error_category }}</p>
            <p class="mb-1">{{ $log->shop_name }} · {{ $log->branch_label }} · {{ $log->device_name }}</p>
            <p class="mb-0">{{ $log->api_method }} {{ $log->api_url }} {{ $log->http_status }}</p>
            <p class="text-secondary mb-0">First {{ $log->first_seen_at }} · Last {{ $log->last_seen_at }} · {{ $log->occurrence_count }} times</p>
        </div></div>
        <div class="card mb-3"><div class="card-body">
            <h6 class="section-title">What happened</h6>
            <p>{{ $log->what_happened ?: '—' }}</p>
            <h6 class="section-title">Stack trace</h6>
            <pre class="bg-light p-3" style="white-space:pre-wrap;">{{ $log->original_stack_trace }}</pre>
        </div></div>
        <div class="card"><div class="card-body">
            <h6 class="section-title">Resolution</h6>
            @if($log->resolved_at)
                <p class="mb-2">Resolved by {{ $log->resolved_by }} at {{ $log->resolved_at }}</p>
            @endif
            <form method="post" action="{{ url('crashes/errors/'.$log->id.'/resolve') }}">
                @csrf
                <textarea name="resolution_notes" class="form-control mb-2" rows="4" placeholder="Resolution notes">{{ $log->resolution_notes }}</textarea>
                <button class="btn btn-primary">Save resolution</button>
            </form>
        </div></div>
    </div>
</div>
@endsection
