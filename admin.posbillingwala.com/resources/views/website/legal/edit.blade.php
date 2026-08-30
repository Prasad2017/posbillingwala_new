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
                        <li class="breadcrumb-item active">{{ $label }}</li>
                    </ol>
                </nav>
            </div>
        </div>
        <div class="row">
            <div class="col-xl-10">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <h5 class="text-primary mb-0">Edit {{ $label }}</h5>
                        <p class="text-muted small mb-3">Basic HTML tags supported: &lt;h2&gt;, &lt;p&gt;, &lt;ul&gt;, &lt;a&gt;, &lt;strong&gt;.</p>
                        <hr>
                        <form method="POST" action="{{ url($backUrl) }}">
                            @csrf
                            <div class="mb-3">
                                <label class="form-label">Page title</label>
                                <input type="text" class="form-control" name="title" value="{{ old('title', $page->title) }}" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Content (HTML)</label>
                                <textarea class="form-control" name="body_html" rows="18" required>{{ old('body_html', $page->body_html) }}</textarea>
                            </div>
                            <button type="submit" class="btn btn-primary px-5">Save page</button>
                            <a href="{{ url('website') }}" class="btn btn-outline-secondary ms-2">Back</a>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
