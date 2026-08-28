@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Logo Update</h5>
                <p class="text-secondary mb-0">Shown in sidebar, header, and login page</p>
            </div>
            <a href="{{ url('settings') }}" class="btn btn-outline-primary btn-sm">Back to Settings</a>
        </div>
        @if(session('success'))
            <div class="alert alert-success">{{ session('success') }}</div>
        @endif
        <div class="card"><div class="card-body">
            <div class="text-center mb-4 p-4" style="background:#f7f9fc;border-radius:12px;">
                <img src="{{ $logoUrl }}" alt="Current logo" style="max-width:180px;max-height:180px;border-radius:12px;">
                <p class="text-secondary mt-2 mb-0">{{ $hasCustom ? 'Custom logo active' : 'Default logo in use' }}</p>
            </div>
            <form method="post" action="{{ url('settings/logo') }}" enctype="multipart/form-data">
                @csrf
                <div class="mb-3">
                    <label class="form-label">Upload New Logo</label>
                    <input type="file" class="form-control @error('logo') is-invalid @enderror" name="logo" accept="image/png,image/jpeg,image/webp,image/svg+xml" required>
                    @error('logo')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    <small class="text-secondary">PNG, JPG, WEBP or SVG · Max 2 MB · Square image recommended</small>
                </div>
                <button class="btn btn-primary">Save Logo</button>
            </form>
        </div></div>
    </div>
</div>
@endsection
