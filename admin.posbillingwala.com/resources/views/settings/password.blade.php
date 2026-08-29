@extends('layouts.app')
@section('page_title', 'Change Password')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Change Password',
            'subtitle' => 'Web admin password — separate from the Dealer mobile app API.',
            'actionUrl' => url('settings'),
            'actionLabel' => 'Settings Hub',
            'actionIcon' => 'bx-grid-alt',
        ])
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
