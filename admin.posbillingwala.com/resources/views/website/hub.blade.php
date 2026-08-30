@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="kpi-card kpi-blue mb-3" style="padding:1.25rem;">
            <h5 class="dash-hello mb-1">Website Content</h5>
            <p class="mb-0">Manage the public marketing website — pages, products, pricing, dealers, and company details.</p>
        </div>

        <a class="hub-row" href="{{ url('website/settings') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-buildings'></i></span>
            <div><h6>Company &amp; Legal Info</h6><p>Legal name, GSTIN, address, support numbers, tagline, app links</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/products') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-package'></i></span>
            <div><h6>Products Catalog</h6><p>{{ $productCount }} product{{ $productCount === 1 ? '' : 's' }} — software, hardware, rolls, accessories</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/pricing') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-rupee'></i></span>
            <div><h6>Software Pricing</h6><p>{{ $pricingCount }} plan{{ $pricingCount === 1 ? '' : 's' }} — subscription &amp; renewal with GST note</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/dealers') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-map'></i></span>
            <div><h6>Dealer Network</h6><p>{{ $dealerCount }} dealer{{ $dealerCount === 1 ? '' : 's' }} — area-wise contacts for the public website</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/company') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-network-chart'></i></span>
            <div><h6>Company Model</h6><p>Business hierarchy &amp; dealer-network model on website</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/support') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-support'></i></span>
            <div><h6>Support Page</h6><p>Customer support content shown on posbillingwala.com/support.html</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/about') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-info-circle'></i></span>
            <div><h6>About Us</h6><p>Company story and mission on the public website</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/clients') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-store'></i></span>
            <div><h6>Trusted Customers</h6><p>{{ $clientCount }} client{{ $clientCount === 1 ? '' : 's' }} — logo, name, city, category</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/testimonials') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-message-square-dots'></i></span>
            <div><h6>Testimonials</h6><p>{{ $testimonialCount }} testimonial{{ $testimonialCount === 1 ? '' : 's' }} — customer quotes</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/privacy') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-file'></i></span>
            <div><h6>Privacy Policy</h6><p>Privacy policy shown on posbillingwala.com</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/terms') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-file-blank'></i></span>
            <div><h6>Terms &amp; Conditions</h6><p>Terms of use for software and services</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/refund') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-revision'></i></span>
            <div><h6>Refund &amp; Renewal Policy</h6><p>Renewal and refund rules for customers</p></div>
        </a>
        <a class="hub-row" href="{{ url('website/contacts') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-envelope'></i></span>
            <div><h6>Contact Enquiries</h6><p>{{ $contactCount }} total · {{ $newContactCount }} new from website form</p></div>
        </a>
    </div>
</div>
@endsection
