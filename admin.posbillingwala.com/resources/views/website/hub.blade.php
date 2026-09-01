@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')

        <div class="website-hub-hero">
            <div class="website-hub-hero-copy">
                <span class="website-hub-kicker">Website CMS</span>
                <h5 class="dash-hello mb-1">Manage posbillingwala.com</h5>
                <p class="mb-0">Products, pricing, dealers, customers, legal pages — everything the public website shows.</p>
            </div>
            <div class="website-hub-hero-actions">
                <a class="btn btn-light btn-sm" href="https://posbillingwala.com" target="_blank" rel="noopener noreferrer">
                    <i class='bx bx-globe'></i> Live site
                </a>
                <a class="btn btn-outline-light btn-sm" href="http://127.0.0.1:8080" target="_blank" rel="noopener noreferrer">
                    <i class='bx bx-desktop'></i> Local preview
                </a>
            </div>
        </div>

        <div class="website-hub-grid">
        <a class="hub-row hub-row--card" href="{{ url('website/settings') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-buildings'></i></span>
            <div><h6>Company &amp; Legal Info</h6><p>Legal name, GSTIN, address, support numbers, tagline, app links</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/products') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-package'></i></span>
            <div><h6>Products Catalog</h6><p>{{ $productCount }} product{{ $productCount === 1 ? '' : 's' }} — software, hardware, rolls, accessories</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/pricing') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-rupee'></i></span>
            <div><h6>Software Pricing</h6><p>{{ $pricingCount }} plan{{ $pricingCount === 1 ? '' : 's' }} — subscription &amp; renewal with GST note</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/dealers') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-map'></i></span>
            <div><h6>Dealer Network</h6><p>{{ $dealerCount }} dealer{{ $dealerCount === 1 ? '' : 's' }} — area-wise contacts for the public website</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/company') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-network-chart'></i></span>
            <div><h6>Company Model</h6><p>Business hierarchy &amp; dealer-network model on website</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/support') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-support'></i></span>
            <div><h6>Support Page</h6><p>Customer support content shown on posbillingwala.com/support.html</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/about') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-info-circle'></i></span>
            <div><h6>About Us</h6><p>Company story and mission on the public website</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/clients') }}">
            <span class="hub-icon kpi-icon green"><i class='bx bx-store'></i></span>
            <div><h6>Trusted Customers</h6><p>{{ $clientCount }} client{{ $clientCount === 1 ? '' : 's' }} — logo, name, city, category</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/testimonials') }}">
            <span class="hub-icon kpi-icon purple"><i class='bx bx-message-square-dots'></i></span>
            <div><h6>Testimonials</h6><p>{{ $testimonialCount }} testimonial{{ $testimonialCount === 1 ? '' : 's' }} — customer quotes</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/privacy') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-file'></i></span>
            <div><h6>Privacy Policy</h6><p>Privacy policy shown on posbillingwala.com</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/terms') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-file-blank'></i></span>
            <div><h6>Terms &amp; Conditions</h6><p>Terms of use for software and services</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card" href="{{ url('website/refund') }}">
            <span class="hub-icon kpi-icon blue"><i class='bx bx-revision'></i></span>
            <div><h6>Refund &amp; Renewal Policy</h6><p>Renewal and refund rules for customers</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        <a class="hub-row hub-row--card hub-row--wide" href="{{ url('website/contacts') }}">
            <span class="hub-icon kpi-icon orange"><i class='bx bx-envelope'></i></span>
            <div><h6>Contact Enquiries</h6><p>{{ $contactCount }} total · {{ $newContactCount }} new from website form</p></div>
            <i class='bx bx-chevron-right hub-chevron'></i>
        </a>
        </div>

        <div class="website-local-tip mt-3">
            <i class='bx bx-terminal'></i>
            <div>
                <strong>Local preview on your PC</strong>
                <p class="mb-0">Run <code>.\scripts\start-local.ps1</code> from project root — website on <code>:8080</code>, admin on <code>:8000</code>.</p>
            </div>
        </div>
    </div>
</div>
@endsection
