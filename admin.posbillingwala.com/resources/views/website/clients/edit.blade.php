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
                            <h5 class="mb-0 text-primary">Edit Client Showcase</h5>
                        </div>
                        <hr>
                        <form class="row g-3" method="POST" action="{{ url('website/clients/edit/'.$client->id) }}" enctype="multipart/form-data">
                            @csrf
                            <div class="col-md-6">
                                <label class="form-label">Business name *</label>
                                <input type="text" class="form-control @error('business_name') is-invalid @enderror" name="business_name" value="{{ old('business_name', $client->business_name) }}" required>
                                @error('business_name')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Subtitle</label>
                                <input type="text" class="form-control" name="subtitle" value="{{ old('subtitle', $client->subtitle) }}">
                            </div>
                            <div class="col-12">
                                <label class="form-label">How they use the app</label>
                                <textarea class="form-control" name="description" rows="3">{{ old('description', $client->description) }}</textarea>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Logo</label>
                                @php $logo = \App\Services\WebsiteMedia::url($client->logo_path); @endphp
                                @if($logo)<img src="{{ $logo }}" alt="" class="d-block mb-2" style="width:64px;height:64px;object-fit:cover;border-radius:10px;">@endif
                                <input type="file" class="form-control" name="logo" accept="image/*">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Photo</label>
                                @php $photo = \App\Services\WebsiteMedia::url($client->photo_path); @endphp
                                @if($photo)<img src="{{ $photo }}" alt="" class="d-block mb-2" style="max-width:160px;max-height:80px;object-fit:cover;border-radius:10px;">@endif
                                <input type="file" class="form-control" name="photo" accept="image/*">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Button link</label>
                                <input type="url" class="form-control" name="cta_url" value="{{ old('cta_url', $client->cta_url) }}">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label">Sort order</label>
                                <input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', $client->sort_order) }}" min="0">
                            </div>
                            <div class="col-md-3 d-flex align-items-end">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="is_published" value="1" id="pub" @if(old('is_published', $client->is_published)) checked @endif>
                                    <label class="form-check-label" for="pub">Published</label>
                                </div>
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary px-5">Save changes</button>
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
