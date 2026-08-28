@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <h5 class="dash-hello mb-1">Reports</h5>
        <p class="text-secondary mb-3">Same reports as the Android Admin app</p>
        <a class="hub-row" href="{{ url('sales/overview') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-line-chart'></i></span>
            <div><h6>Sales Overview</h6><p>Track sales performance &amp; trends</p></div>
        </a>
        <a class="hub-row" href="{{ url('reports/customers') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-group'></i></span>
            <div><h6>Customer Reports</h6><p>Customer growth &amp; status analytics</p></div>
        </a>
        <a class="hub-row" href="{{ url('reports/licenses') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-certification'></i></span>
            <div><h6>License Reports</h6><p>License status &amp; expiry insights</p></div>
        </a>
        <a class="hub-row" href="{{ url('reports/dealers') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-store'></i></span>
            <div><h6>Dealer Reports</h6><p>Dealer performance &amp; sales</p></div>
        </a>
        <a class="hub-row" href="{{ url('reports/branches') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-buildings'></i></span>
            <div><h6>Branch Reports</h6><p>Branch status &amp; distribution</p></div>
        </a>
        <a class="hub-row" href="{{ url('reports/devices') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-mobile-alt'></i></span>
            <div><h6>Device Reports</h6><p>Device usage &amp; activity</p></div>
        </a>
    </div>
</div>
@endsection
