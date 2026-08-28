@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="kpi-card kpi-blue mb-3" style="padding:1.25rem;">
            <h5 class="dash-hello mb-1">Need Help?</h5>
            <p class="mb-0">Our support team is here to help you. {{ $open }} open · {{ $total }} total tickets</p>
        </div>
        <a class="hub-row" href="{{ url('support/tickets/create') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-plus-circle'></i></span>
            <div><h6>Create Ticket</h6><p>Submit a new support request</p></div>
        </a>
        <a class="hub-row" href="{{ url('support/tickets') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-list-ul'></i></span>
            <div><h6>My Tickets</h6><p>Track open and closed tickets</p></div>
        </a>
        <a class="hub-row" href="{{ url('support/faq') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-help-circle'></i></span>
            <div><h6>FAQs</h6><p>Common questions and answers</p></div>
        </a>
    </div>
</div>
@endsection
