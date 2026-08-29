@extends('layouts.app')
@section('page_title', 'Settings')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Settings',
            'subtitle' => $user->name . ' · ' . ($user->email ?: $user->contact_number),
        ])
        <a class="hub-row" href="{{ url('settings/profile') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-user'></i></span>
            <div><h6>Profile Update</h6><p>Update your name, email, and contact details</p></div>
        </a>
        <a class="hub-row" href="{{ url('settings/password') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-lock-alt'></i></span>
            <div><h6>Change Password</h6><p>Update your account password</p></div>
        </a>
        @if(Auth::user()->role_id == 1)
        <a class="hub-row" href="{{ url('settings/logo') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-image'></i></span>
            <div><h6>Logo Update</h6><p>Change sidebar and login logo</p></div>
        </a>
        <a class="hub-row" href="{{ url('settings/favicon') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-star'></i></span>
            <div><h6>Favicon Update</h6><p>Change browser tab icon</p></div>
        </a>
        @endif
    </div>
</div>
@endsection
