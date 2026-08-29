@extends('layouts.app')
@section('page_title', 'Users & Roles')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.page-header', [
            'title' => 'Users & Roles',
            'subtitle' => 'Manage dealers, shop owners, and access levels.',
        ])
        <a class="hub-row" href="{{ url('dealer/all') }}">
            <span class="hub-icon blue"><i class='bx bx-store-alt'></i></span>
            <span class="flex-grow-1">
                <strong>Dealers</strong>
                <small class="d-block text-secondary">View and manage dealer accounts</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('dealer/add') }}">
            <span class="hub-icon green"><i class='bx bx-user-plus'></i></span>
            <span class="flex-grow-1">
                <strong>Add Dealer</strong>
                <small class="d-block text-secondary">Register a new dealer</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('customers/all') }}">
            <span class="hub-icon orange"><i class='bx bx-group'></i></span>
            <span class="flex-grow-1">
                <strong>Customers / Shop Owners</strong>
                <small class="d-block text-secondary">All registered shop accounts</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('customers/all-license') }}">
            <span class="hub-icon purple"><i class='bx bx-key'></i></span>
            <span class="flex-grow-1">
                <strong>Licenses</strong>
                <small class="d-block text-secondary">License keys and validity</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
    </div>
</div>
@endsection
