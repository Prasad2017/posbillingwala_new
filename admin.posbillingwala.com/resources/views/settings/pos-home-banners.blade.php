@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <div>
                <h5 class="dash-hello mb-1">Banner Upload</h5>
                <p class="text-secondary mb-0">POS app home screen banners. Upload one image or several — they auto-slide with dots.</p>
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
            <form method="post" action="{{ url('settings/pos-banners') }}" enctype="multipart/form-data">
                @csrf
                <div class="mb-3">
                    <label class="form-label">Upload banners</label>
                    <input type="file" class="form-control" name="banners[]" accept="image/png,image/jpeg,image/webp" multiple required>
                    <small class="text-secondary">JPG, PNG or WEBP · Max 2 MB each · Up to 8 at a time · Wide image (about 2.4:1) looks best</small>
                </div>
                <button class="btn btn-primary">Upload</button>
            </form>
        </div></div>

        <div class="card"><div class="card-body">
            <div class="table-responsive">
                <table class="table table-striped table-bordered mb-0">
                    <thead>
                        <tr>
                            <th style="width:180px">Preview</th>
                            <th>Order</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        @forelse($banners as $banner)
                        <tr>
                            <td>
                                <img src="{{ \App\Services\WebsiteMedia::url($banner->image_path) ?: $banner->image_url }}" alt="" style="width:160px;height:68px;object-fit:cover;border-radius:10px;background:#f7f9fc;">
                            </td>
                            <td>{{ $banner->sort_order }}</td>
                            <td>
                                @if($banner->is_active)
                                <span class="badge bg-success">Showing</span>
                                @else
                                <span class="badge bg-secondary">Hidden</span>
                                @endif
                            </td>
                            <td>
                                <a href="{{ url('settings/pos-banners/move/'.$banner->bannerId.'/up') }}" class="btn btn-sm btn-outline-secondary">Up</a>
                                <a href="{{ url('settings/pos-banners/move/'.$banner->bannerId.'/down') }}" class="btn btn-sm btn-outline-secondary">Down</a>
                                <a href="{{ url('settings/pos-banners/toggle/'.$banner->bannerId) }}" class="btn btn-sm btn-outline-warning">{{ $banner->is_active ? 'Hide' : 'Show' }}</a>
                                <a href="{{ url('settings/pos-banners/delete/'.$banner->bannerId) }}" class="btn btn-sm btn-outline-danger" onclick="return confirm('Remove this banner?')">Delete</a>
                            </td>
                        </tr>
                        @empty
                        <tr><td colspan="4" class="text-center text-muted py-4">No banners yet. The POS home screen hides this area until you upload one.</td></tr>
                        @endforelse
                    </tbody>
                </table>
            </div>
        </div></div>
    </div>
</div>
@endsection
