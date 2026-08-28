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
                        <li class="breadcrumb-item active">About Us</li>
                    </ol>
                </nav>
            </div>
        </div>

        <div class="row">
            <div class="col-xl-10">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <div class="card-title d-flex align-items-center">
                            <div><i class="bx bx-info-circle me-1 font-22 text-primary"></i></div>
                            <h5 class="mb-0 text-primary">Edit About Us Page</h5>
                        </div>
                        <p class="text-muted small mb-3">Shown at <strong>/about.html</strong> on the public website.</p>
                        <hr>
                        <form method="POST" action="{{ url('website/about') }}">
                            @csrf
                            <div class="mb-3">
                                <label class="form-label">Page title</label>
                                <input type="text" class="form-control @error('title') is-invalid @enderror" name="title" value="{{ old('title', $page->title) }}" required>
                                @error('title')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Content (HTML)</label>
                                <textarea class="form-control @error('body_html') is-invalid @enderror" name="body_html" rows="18" required>{{ old('body_html', $page->body_html) }}</textarea>
                                @error('body_html')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <p class="text-muted small">Last updated: {{ optional($page->updated_at)->format('d M Y, h:i A') ?? '—' }}</p>
                            <button type="submit" class="btn btn-primary px-5">Save About Us</button>
                            <a href="{{ url('website') }}" class="btn btn-outline-secondary ms-2">Back</a>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
