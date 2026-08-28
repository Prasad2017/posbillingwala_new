@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="row">
            <div class="col-xl-8">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <div class="card-title d-flex align-items-center">
                            <div><i class="bx bx-store me-1 font-22 text-primary"></i></div>
                            <h5 class="mb-0 text-primary">Add Client Showcase</h5>
                        </div>
                        <p class="text-muted small">Shown on the website homepage — business logo/name and how they use POS Billingwala.</p>
                        <hr>
                        <form class="row g-3" method="POST" action="{{ url('website/clients/add') }}" enctype="multipart/form-data">
                            @csrf
                            <div class="col-md-6">
                                <label class="form-label">Business name *</label>
                                <input type="text" class="form-control @error('business_name') is-invalid @enderror" name="business_name" value="{{ old('business_name') }}" required>
                                @error('business_name')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Subtitle (owner / location)</label>
                                <input type="text" class="form-control" name="subtitle" value="{{ old('subtitle') }}" placeholder="e.g. Velu · Pune">
                            </div>
                            <div class="col-12">
                                <label class="form-label">How they use the app</label>
                                <textarea class="form-control" name="description" rows="3" placeholder="Short story for the website">{{ old('description') }}</textarea>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Logo (square, optional)</label>
                                <input type="file" class="form-control" name="logo" accept="image/*">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Photo (wide, optional)</label>
                                <input type="file" class="form-control" name="photo" accept="image/*">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Button link (optional)</label>
                                <input type="url" class="form-control" name="cta_url" value="{{ old('cta_url') }}" placeholder="https://play.google.com/...">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Sort order</label>
                                <input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', 0) }}" min="0">
                            </div>
                            <div class="col-md-3 d-flex align-items-end">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="is_published" value="1" id="pub" checked>
                                    <label class="form-check-label" for="pub">Published on website</label>
                                </div>
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary px-5">Add client</button>
                                <a href="{{ url('website/clients') }}" class="btn btn-outline-secondary ms-2">Cancel</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
