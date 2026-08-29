@extends('layouts.app')
@section('page_title', 'Reports')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.page-header', [
            'title' => 'Reports',
            'subtitle' => 'Analytics and insights across customers, licenses, dealers, and devices.',
        ])
        <a class="hub-row" href="{{ url('sales/overview') }}">
            <span class="hub-icon blue"><i class='bx bx-line-chart'></i></span>
            <span class="flex-grow-1">
                <strong>Sales Overview</strong>
                <small class="d-block text-secondary">Track sales performance &amp; trends</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('reports/customers') }}">
            <span class="hub-icon purple"><i class='bx bx-group'></i></span>
            <span class="flex-grow-1">
                <strong>Customer Reports</strong>
                <small class="d-block text-secondary">Customer growth &amp; status analytics</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('reports/licenses') }}">
            <span class="hub-icon green"><i class='bx bx-certification'></i></span>
            <span class="flex-grow-1">
                <strong>License Reports</strong>
                <small class="d-block text-secondary">License status &amp; expiry insights</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('reports/dealers') }}">
            <span class="hub-icon orange"><i class='bx bx-store'></i></span>
            <span class="flex-grow-1">
                <strong>Dealer Reports</strong>
                <small class="d-block text-secondary">Dealer performance &amp; sales</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('reports/branches') }}">
            <span class="hub-icon blue"><i class='bx bx-buildings'></i></span>
            <span class="flex-grow-1">
                <strong>Branch Reports</strong>
                <small class="d-block text-secondary">Branch status &amp; distribution</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
        <a class="hub-row" href="{{ url('reports/devices') }}">
            <span class="hub-icon purple"><i class='bx bx-mobile-alt'></i></span>
            <span class="flex-grow-1">
                <strong>Device Reports</strong>
                <small class="d-block text-secondary">Device usage &amp; activity</small>
            </span>
            <i class='bx bx-chevron-right'></i>
        </a>
    </div>
</div>
@endsection
