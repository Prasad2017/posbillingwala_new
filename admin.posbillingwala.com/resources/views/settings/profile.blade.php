@extends('layouts.app')
@section('page_title', 'Profile Update')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Profile Update',
            'subtitle' => 'Web admin account — saved directly to the users table (not the mobile API).',
            'actionUrl' => url('settings'),
            'actionLabel' => 'Settings Hub',
            'actionIcon' => 'bx-grid-alt',
        ])

        <div class="card">
            <div class="card-body">
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
                            <input type="email" class="form-control @error('email') is-invalid @enderror" name="email" value="{{ old('email', $user->email) }}" @if((int) $user->role_id === 1) required @endif>
                            @error('email')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Phone Number</label>
                            <input class="form-control @error('contact_number') is-invalid @enderror" name="contact_number" maxlength="10" value="{{ old('contact_number', $user->contact_number) }}" @if((int) $user->role_id === 2) required @endif>
                            @error('contact_number')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        </div>
                        @if((int) $user->role_id === 2)
                        <div class="col-md-6">
                            <label class="form-label">Aadhaar Number</label>
                            <input class="form-control @error('aadhar_number') is-invalid @enderror" name="aadhar_number" maxlength="12" value="{{ old('aadhar_number', $user->aadhar_number) }}" required>
                            @error('aadhar_number')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        </div>
                        @endif
                        @if((int) $user->role_id === 2 || (int) $user->role_id === 3)
                        <div class="col-md-6">
                            <label class="form-label">Shop Name</label>
                            <input class="form-control @error('shopName') is-invalid @enderror" name="shopName" value="{{ old('shopName', $user->shopName ?? '') }}">
                            @error('shopName')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        </div>
                        @endif
                        <div class="col-12">
                            <label class="form-label">Address</label>
                            <textarea class="form-control @error('address') is-invalid @enderror" name="address" rows="3" @if((int) $user->role_id === 2) required @endif>{{ old('address', $user->address ?? '') }}</textarea>
                            @error('address')<div class="invalid-feedback">{{ $message }}</div>@enderror
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary mt-3">Save Profile</button>
                </form>
            </div>
        </div>
    </div>
</div>
@endsection
