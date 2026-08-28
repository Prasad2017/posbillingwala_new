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
                            <div><i class="bx bx-message-square-dots me-1 font-22 text-primary"></i></div>
                            <h5 class="mb-0 text-primary">Edit Testimonial</h5>
                        </div>
                        <hr>
                        <form class="row g-3" method="POST" action="{{ url('website/testimonials/edit/'.$testimonial->id) }}" enctype="multipart/form-data">
                            @csrf
                            <div class="col-md-6">
                                <label class="form-label">Author name *</label>
                                <input type="text" class="form-control @error('author_name') is-invalid @enderror" name="author_name" value="{{ old('author_name', $testimonial->author_name) }}" required>
                                @error('author_name')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Business name</label>
                                <input type="text" class="form-control" name="business_name" value="{{ old('business_name', $testimonial->business_name) }}">
                            </div>
                            <div class="col-12">
                                <label class="form-label">Quote *</label>
                                <textarea class="form-control @error('quote') is-invalid @enderror" name="quote" rows="4" required>{{ old('quote', $testimonial->quote) }}</textarea>
                                @error('quote')<div class="invalid-feedback">{{ $message }}</div>@enderror
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Rating</label>
                                <select class="form-select" name="rating">
                                    @for($i = 5; $i >= 1; $i--)
                                    <option value="{{ $i }}" @if(old('rating', $testimonial->rating) == $i) selected @endif>{{ $i }} stars</option>
                                    @endfor
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Sort order</label>
                                <input type="number" class="form-control" name="sort_order" value="{{ old('sort_order', $testimonial->sort_order) }}" min="0">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Photo</label>
                                @php $photo = \App\Services\WebsiteMedia::url($testimonial->photo_path); @endphp
                                @if($photo)<img src="{{ $photo }}" alt="" class="d-block mb-2" style="width:48px;height:48px;object-fit:cover;border-radius:50%;">@endif
                                <input type="file" class="form-control" name="photo" accept="image/*">
                            </div>
                            <div class="col-12">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="is_published" value="1" id="pub" @if(old('is_published', $testimonial->is_published)) checked @endif>
                                    <label class="form-check-label" for="pub">Published</label>
                                </div>
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary px-5">Save changes</button>
                                <a href="{{ url('website/testimonials') }}" class="btn btn-outline-secondary ms-2">Cancel</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
