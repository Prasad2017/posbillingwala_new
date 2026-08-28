@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Profile Update</h5>
                <p class="text-secondary mb-0">Manage your account details</p>
            </div>
            <a href="{{ url('settings') }}" class="btn btn-outline-primary btn-sm">Back to Settings</a>
        </div>
        @if(session('success'))
            <div class="alert alert-success">{{ session('success') }}</div>
        @endif
        <div class="card"><div class="card-body">
            <form method="post" action="{{ url('settings/profile') }}">
                @csrf
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label">Full Name</label>
                        <input class="form-control @error('name') is-invalid @enderror" name="name" value="{{ old('name', $user->name) }}" required>
                        @error('name')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control @error('email') is-invalid @enderror" name="email" value="{{ old('email', $user->email) }}" @if($user->role_id == 1) required @endif>
                        @error('email')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Phone Number</label>
                        <input class="form-control @error('contact_number') is-invalid @enderror" name="contact_number" maxlength="10" value="{{ old('contact_number', $user->contact_number) }}">
                        @error('contact_number')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    @if($user->role_id == 2 || $user->role_id == 3)
                    <div class="col-md-6">
                        <label class="form-label">Shop Name</label>
                        <input class="form-control @error('shopName') is-invalid @enderror" name="shopName" value="{{ old('shopName', $user->shopName) }}">
                        @error('shopName')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                    @endif
                    <div class="col-12">
                        <label class="form-label">Address</label>
                        <textarea class="form-control @error('address') is-invalid @enderror" name="address" rows="3">{{ old('address', $user->address) }}</textarea>
                        @error('address')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    </div>
                </div>
                <button class="btn btn-primary mt-3">Save Profile</button>
            </form>
        </div></div>
    </div>
</div>
@endsection
