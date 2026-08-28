@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Change Password</h5>
                <p class="text-secondary mb-0">Use at least 8 characters</p>
            </div>
            <a href="{{ url('settings') }}" class="btn btn-outline-primary btn-sm">Back to Settings</a>
        </div>
        @if(session('success'))
            <div class="alert alert-success">{{ session('success') }}</div>
        @endif
        <div class="card"><div class="card-body">
            <form method="post" action="{{ url('settings/password') }}">
                @csrf
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label">Current Password</label>
                        <input type="password" class="form-control @error('current_password') is-invalid @enderror" name="current_password" required autocomplete="current-password">
                        @error('current_password')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">New Password</label>
                        <input type="password" class="form-control @error('password') is-invalid @enderror" name="password" required autocomplete="new-password">
                        @error('password')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Confirm New Password</label>
                        <input type="password" class="form-control" name="password_confirmation" required autocomplete="new-password">
                    </div>
                </div>
                <button class="btn btn-primary mt-3">Update Password</button>
            </form>
        </div></div>
    </div>
</div>
@endsection
