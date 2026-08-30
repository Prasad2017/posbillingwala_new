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
                            <h5 class="mb-0 text-primary">Add Trusted Customer</h5>
                        </div>
                        <p class="text-muted small">Shown on website — logo, business name, city, category (with customer permission).</p>
                        <hr>
                        <form class="row g-3" method="POST" action="{{ url('website/clients/add') }}" enctype="multipart/form-data">
                            @csrf
                            <div class="col-md-6">
                                <label class="form-label">Business name *</label>
                                <input type="text" class="form-control @error('business_name') is-invalid @enderror" name="business_name" value="{{ old('business_name') }}" required>
                                @error('business_name')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">City</label>
                                <input type="text" class="form-control" name="city" value="{{ old('city') }}" placeholder="e.g. Pune">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Business category</label>
                                <input type="text" class="form-control" name="business_category" value="{{ old('business_category') }}" placeholder="Restaurant / Retail / Mess / Hotel">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Subtitle (owner name, optional)</label>
                                <input type="text" class="form-control" name="subtitle" value="{{ old('subtitle') }}" placeholder="e.g. Hotel Shree · Owner">
                            </div>
                            <div class="col-12">
                                <label class="form-label">Short story (optional)</label>
                                <textarea class="form-control" name="description" rows="3">{{ old('description') }}</textarea>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Logo</label>
                                <input type="file" class="form-control" name="logo" accept="image/*">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Photo (wide, optional)</label>
                                <input type="file" class="form-control" name="photo" accept="image/*">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Button link (optional)</label>
                                <input type="url" class="form-control" name="cta_url" value="{{ old('cta_url') }}">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Sort order</label>
                                <input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', 0) }}" min="0">
                            </div>
                            <div class="col-md-3 d-flex align-items-end">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="is_published" value="1" id="pub" checked>
                                    <label class="form-check-label" for="pub">Published</label>
                                </div>
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary px-5">Add customer</button>
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
