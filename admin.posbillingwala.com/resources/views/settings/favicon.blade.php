@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Favicon Update</h5>
                <p class="text-secondary mb-0">Browser tab icon for admin panel and public website</p>
            </div>
            <a href="{{ url('settings') }}" class="btn btn-outline-primary btn-sm">Back to Settings</a>
        </div>
        @if(session('success'))
            <div class="alert alert-success">{{ session('success') }}</div>
        @endif
        <div class="card"><div class="card-body">
            <div class="text-center mb-4 p-4" style="background:#f7f9fc;border-radius:12px;">
                <img src="{{ $faviconUrl }}" alt="Current favicon" style="width:64px;height:64px;border-radius:8px;">
                <p class="text-secondary mt-2 mb-0">{{ $hasCustom ? 'Custom favicon active' : 'Default favicon in use' }}</p>
            </div>
            <form method="post" action="{{ url('settings/favicon') }}" enctype="multipart/form-data">
                @csrf
                <div class="mb-3">
                    <label class="form-label">Upload New Favicon</label>
                    <input type="file" class="form-control @error('favicon') is-invalid @enderror" name="favicon" accept="image/png,image/jpeg,image/webp,image/x-icon,image/svg+xml,.ico" required>
                    @error('favicon')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    <small class="text-secondary">PNG, ICO, JPG, WEBP or SVG · Max 512 KB · 32×32 or 64×64 recommended</small>
                </div>
                <button class="btn btn-primary">Save Favicon</button>
            </form>
        </div></div>
    </div>
</div>
@endsection
