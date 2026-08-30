@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Website</div>
            <div class="ps-3">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 p-0">
                        <li class="breadcrumb-item"><a href="{{ url('website') }}"><i class="bx bx-home-alt"></i></a></li>
                        <li class="breadcrumb-item active">Company Settings</li>
                    </ol>
                </nav>
            </div>
        </div>

        <div class="row">
            <div class="col-xl-10">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <h5 class="text-primary mb-1">Company &amp; Legal Info</h5>
                        <p class="text-muted small mb-3">Shown in the website footer, contact page, and download section.</p>
                        <hr>
                        <form method="POST" action="{{ url('website/settings') }}">
                            @csrf
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label">Legal company name</label>
                                    <input type="text" class="form-control" name="legal_company_name" value="{{ old('legal_company_name', $settings['legal_company_name'] ?? '') }}">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Brand tagline</label>
                                    <input type="text" class="form-control" name="brand_tagline" value="{{ old('brand_tagline', $settings['brand_tagline'] ?? '') }}">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">GSTIN</label>
                                    <input type="text" class="form-control" name="gstin" value="{{ old('gstin', $settings['gstin'] ?? '') }}" placeholder="27XXXXX1234X1ZX">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Business hours</label>
                                    <input type="text" class="form-control" name="business_hours" value="{{ old('business_hours', $settings['business_hours'] ?? '') }}">
                                </div>
                                <div class="col-12">
                                    <label class="form-label">Registered office address</label>
                                    <textarea class="form-control" name="office_address" rows="2">{{ old('office_address', $settings['office_address'] ?? '') }}</textarea>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label">Support phone</label>
                                    <input type="text" class="form-control" name="support_phone" value="{{ old('support_phone', $settings['support_phone'] ?? '') }}">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label">Support WhatsApp</label>
                                    <input type="text" class="form-control" name="support_whatsapp" value="{{ old('support_whatsapp', $settings['support_whatsapp'] ?? '') }}">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label">Support email</label>
                                    <input type="email" class="form-control" name="support_email" value="{{ old('support_email', $settings['support_email'] ?? '') }}">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Sales email</label>
                                    <input type="email" class="form-control" name="sales_email" value="{{ old('sales_email', $settings['sales_email'] ?? '') }}">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">App latest version (display)</label>
                                    <input type="text" class="form-control" name="app_latest_version" value="{{ old('app_latest_version', $settings['app_latest_version'] ?? '') }}" placeholder="e.g. 2.4.0">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">Play Store URL</label>
                                    <input type="url" class="form-control" name="play_store_url" value="{{ old('play_store_url', $settings['play_store_url'] ?? '') }}">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label">APK download URL (optional)</label>
                                    <input type="url" class="form-control" name="apk_download_url" value="{{ old('apk_download_url', $settings['apk_download_url'] ?? '') }}">
                                </div>
                            </div>
                            <div class="mt-4">
                                <button type="submit" class="btn btn-primary px-5">Save settings</button>
                                <a href="{{ url('website') }}" class="btn btn-outline-secondary ms-2">Back</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
