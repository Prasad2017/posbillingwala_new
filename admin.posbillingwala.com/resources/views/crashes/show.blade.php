@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <a href="{{ url('crashes') }}" class="mb-3 d-inline-block">&larr; Back to crashes</a>
        <div class="card mb-3"><div class="card-body">
            <h5 class="dash-hello">{{ $crash->error_title }}</h5>
            <p class="text-secondary mb-1">{{ $crash->error_class }}</p>
            <p class="mb-0">{{ $crash->app_name }} · {{ $crash->device_name }} · Android {{ $crash->android_version }} · v{{ $crash->app_version }}</p>
            <p class="mb-0">User: {{ $crash->user_name }} ({{ $crash->user_id }}) · {{ $crash->occurrences }} occurrences</p>
            <p class="mb-0">Status: <span class="status-badge status-trial">{{ $crash->status }}</span></p>
        </div></div>
        <form method="post" action="{{ url('crashes/'.$crash->id.'/status') }}" class="d-flex gap-2 mb-3">
            @csrf
            <select name="status" class="form-control" style="max-width:220px">
                @foreach(['New','Investigating','Resolved'] as $s)
                    <option value="{{ $s }}" @if($crash->status===$s) selected @endif>{{ $s }}</option>
                @endforeach
            </select>
            <button class="btn btn-primary">Update status</button>
        </form>
        <div class="card"><div class="card-body">
            <h6 class="section-title">Stack trace</h6>
            <pre class="bg-light p-3" style="white-space:pre-wrap;">{{ $crash->stack_trace }}</pre>
        </div></div>
    </div>
</div>
@endsection
