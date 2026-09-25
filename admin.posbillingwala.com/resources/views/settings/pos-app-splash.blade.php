@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Splash Screen Upload</h5>
                <p class="text-secondary mb-0">Shown full-screen when the POS app starts. Use one image — it scales to fit phones, tablets, and landscape.</p>
            </div>
            <a href="{{ url('settings') }}" class="btn btn-outline-primary btn-sm">Back to Settings</a>
        </div>
        @if(session('success'))
            <div class="alert alert-success">{{ session('success') }}</div>
        @endif
        @if($errors->any())
            <div class="alert alert-danger">{{ $errors->first() }}</div>
        @endif

        <div class="card mb-3"><div class="card-body">
            <div class="text-center mb-4 p-4" style="background:#f7f9fc;border-radius:12px;">
                @if($splash && ($splash->image_url || $splash->image_path))
                    <img
                        src="{{ \App\Services\WebsiteMedia::url($splash->image_path) ?: $splash->image_url }}"
                        alt="Current splash"
                        style="max-width:min(100%,420px);max-height:520px;width:auto;height:auto;border-radius:12px;object-fit:contain;background:#fff;"
                    >
                    <p class="text-secondary mt-2 mb-0">Custom splash active</p>
                @else
                    <p class="text-secondary mb-0">No custom splash — app uses built-in default art</p>
                @endif
            </div>
            <form method="post" action="{{ url('settings/pos-splash') }}" enctype="multipart/form-data">
                @csrf
                <div class="mb-3">
                    <label class="form-label">Upload splash image</label>
                    <input type="file" class="form-control @error('splash') is-invalid @enderror" name="splash" accept="image/png,image/jpeg,image/webp" required>
                    @error('splash')<div class="invalid-feedback">{{ $message }}</div>@enderror
                    <small class="text-secondary">JPG, PNG or WEBP · Max 4 MB · Portrait ~9:16 or square works best; landscape tablets use cover fit</small>
                </div>
                <button class="btn btn-primary">Save Splash</button>
            </form>
            @if($splash)
            <form method="post" action="{{ url('settings/pos-splash/delete') }}" class="mt-3" onsubmit="return confirm('Remove custom splash?')">
                @csrf
                <button class="btn btn-outline-danger btn-sm">Remove custom splash</button>
            </form>
            @endif
        </div></div>
    </div>
</div>
@endsection
