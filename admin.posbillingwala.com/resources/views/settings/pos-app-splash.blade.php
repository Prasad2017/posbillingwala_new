@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Splash Screen Upload</h5>
                <p class="text-secondary mb-0">One image per screen. The POS app (web, phone, tablet) picks the matching picture in portrait and landscape, and fills that screen with it.</p>
            </div>
            <a href="{{ url('settings') }}" class="btn btn-outline-primary btn-sm">Back to Settings</a>
        </div>
        @if(session('success'))
            <div class="alert alert-success">{{ session('success') }}</div>
        @endif
        @if($errors->any())
            <div class="alert alert-danger">{{ $errors->first() }}</div>
        @endif

        <div class="alert alert-light border mb-3">
            Empty slots fall back to the closest uploaded image (same orientation first), then the older single splash, then the app logo. Every splash fills the screen edge to edge (sides may crop on odd device sizes).
        </div>

        @if($legacy && ($legacy->image_url || $legacy->image_path))
        <div class="card mb-3"><div class="card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
            <div class="d-flex align-items-center gap-3">
                <img
                    src="{{ \App\Services\WebsiteMedia::url($legacy->image_path) ?: $legacy->image_url }}"
                    alt="Older splash"
                    style="width:72px;height:72px;object-fit:cover;border-radius:10px;background:#fff;"
                >
                <div>
                    <div class="fw-semibold">Older single splash</div>
                    <div class="text-secondary small mb-0">Still used only where a size above is missing. Upload the sizes you need, then you can remove this.</div>
                </div>
            </div>
            <form method="post" action="{{ url('settings/pos-splash/delete') }}" onsubmit="return confirm('Remove the older single splash?')">
                @csrf
                <input type="hidden" name="slot" value="legacy">
                <button class="btn btn-outline-danger btn-sm">Remove older splash</button>
            </form>
        </div></div>
        @endif

        <div class="row g-3">
            @foreach($slots as $slotId => $meta)
                @php $row = $rows->get($slotId); @endphp
                <div class="col-md-6 col-xl-4">
                    <div class="card h-100"><div class="card-body d-flex flex-column">
                        <h6 class="mb-1">{{ $meta['label'] }}</h6>
                        <p class="text-secondary small">{{ $meta['hint'] }}</p>
                        <div class="mb-3 p-3 text-center" style="background:#f7f9fc;border-radius:12px;">
                            @if($row && ($row->image_url || $row->image_path))
                                <img
                                    src="{{ \App\Services\WebsiteMedia::url($row->image_path) ?: $row->image_url }}"
                                    alt="{{ $meta['label'] }}"
                                    style="width:min(100%, {{ $meta['previewWidth'] }});aspect-ratio:{{ $meta['ratio'] }};object-fit:cover;border-radius:12px;background:#fff;"
                                >
                                <p class="text-secondary small mt-2 mb-0">Active for this screen</p>
                            @else
                                <div class="text-secondary small d-flex align-items-center justify-content-center mx-auto" style="width:min(100%, {{ $meta['previewWidth'] }});aspect-ratio:{{ $meta['ratio'] }};border:1px dashed #c5ced9;border-radius:12px;background:#fff;">
                                    No image
                                </div>
                            @endif
                        </div>
                        <form method="post" action="{{ url('settings/pos-splash') }}" enctype="multipart/form-data" class="mt-auto">
                            @csrf
                            <input type="hidden" name="slot" value="{{ $slotId }}">
                            <input type="file" class="form-control form-control-sm mb-2" name="splash" accept="image/png,image/jpeg,image/webp" required>
                            <button class="btn btn-primary btn-sm">Save</button>
                        </form>
                        @if($row)
                        <form method="post" action="{{ url('settings/pos-splash/delete') }}" class="mt-2" onsubmit="return confirm('Remove this splash image?')">
                            @csrf
                            <input type="hidden" name="slot" value="{{ $slotId }}">
                            <button class="btn btn-outline-danger btn-sm">Remove</button>
                        </form>
                        @endif
                    </div></div>
                </div>
            @endforeach
        </div>

        <p class="text-secondary small mt-3 mb-0">JPG, PNG or WEBP · Max 6 MB each · Keep logos and text near the center so slight crop on odd phone sizes still looks right.</p>

        @if($rows->isNotEmpty())
        <form method="post" action="{{ url('settings/pos-splash/delete') }}" class="mt-3" onsubmit="return confirm('Remove every splash image?')">
            @csrf
            <input type="hidden" name="slot" value="all">
            <button class="btn btn-outline-danger btn-sm">Remove all splash images</button>
        </form>
        @endif
    </div>
</div>
@endsection
