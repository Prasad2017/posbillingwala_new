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
                        <li class="breadcrumb-item active">Testimonials</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <a href="{{ url('website/testimonials/add') }}" class="btn btn-primary px-4"><i class="bx bx-plus"></i> Add Testimonial</a>
            </div>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-striped table-bordered mb-0">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Author</th>
                                <th>Business</th>
                                <th>Quote</th>
                                <th>Rating</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            @forelse($testimonials as $item)
                            <tr>
                                <td>{{ $item->id }}</td>
                                <td>{{ $item->author_name }}</td>
                                <td>{{ $item->business_name ?: '—' }}</td>
                                <td>{{ \Illuminate\Support\Str::limit($item->quote, 80) }}</td>
                                <td>{{ $item->rating }}/5</td>
                                <td>
                                    @if($item->is_published)
                                    <span class="badge bg-success">Published</span>
                                    @else
                                    <span class="badge bg-secondary">Hidden</span>
                                    @endif
                                </td>
                                <td>
                                    <a href="{{ url('website/testimonials/edit/'.$item->id) }}" class="btn btn-sm btn-outline-primary">Edit</a>
                                    <a href="{{ url('website/testimonials/toggle/'.$item->id) }}" class="btn btn-sm btn-outline-warning">{{ $item->is_published ? 'Hide' : 'Publish' }}</a>
                                    <a href="{{ url('website/testimonials/delete/'.$item->id) }}" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete this testimonial?')">Delete</a>
                                </td>
                            </tr>
                            @empty
                            <tr><td colspan="7" class="text-center text-muted py-4">No testimonials yet.</td></tr>
                            @endforelse
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
