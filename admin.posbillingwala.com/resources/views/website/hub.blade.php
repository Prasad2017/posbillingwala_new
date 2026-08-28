@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="kpi-card kpi-blue mb-3" style="padding:1.25rem;">
            <h5 class="dash-hello mb-1">Website Content</h5>
            <p class="mb-0">Manage the public marketing website — privacy policy, client showcase, and testimonials.</p>
        </div>
        <a class="hub-row" href="{{ url('website/privacy') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-file'></i></span>
            <div><h6>Privacy Policy</h6><p>Edit the privacy policy shown on posbillingwala.com</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/about') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-info-circle'></i></span>
            <div><h6>About Us</h6><p>Company story and mission on the public website</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/clients') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-store'></i></span>
            <div><h6>Client Showcase</h6><p>{{ $clientCount }} client{{ $clientCount === 1 ? '' : 's' }} — logo, name, and how they use the app</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/testimonials') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-message-square-dots'></i></span>
            <div><h6>Testimonials</h6><p>{{ $testimonialCount }} testimonial{{ $testimonialCount === 1 ? '' : 's' }} — customer quotes for the homepage</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/contacts') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-envelope'></i></span>
            <div><h6>Contact Enquiries</h6><p>{{ $contactCount }} total · {{ $newContactCount }} new from website form</p></div>
        </a>
    </div>
</div>
@endsection
