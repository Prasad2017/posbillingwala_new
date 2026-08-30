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
                        <li class="breadcrumb-item active">Dealers</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <a href="{{ url('website/dealers/add') }}" class="btn btn-primary px-4"><i class="bx bx-plus"></i> Add Dealer</a>
            </div>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-striped table-bordered mb-0">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Area</th>
                                <th>Dealer / Office</th>
                                <th>Contact</th>
                                <th>Mobile</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            @forelse($dealers as $dealer)
                            <tr>
                                <td>{{ $dealer->id }}</td>
                                <td>{{ $dealer->area }}</td>
                                <td>{{ $dealer->dealer_name }}</td>
                                <td>{{ $dealer->contact_person ?: '—' }}</td>
                                <td>{{ $dealer->mobile ?: '—' }}</td>
                                <td>{{ $dealer->dealer_type === 'head_office' ? 'Head Office' : 'Authorized Dealer' }}</td>
                                <td>
                                    @if($dealer->is_published)
                                    <span class="badge bg-success">Published</span>
                                    @else
                                    <span class="badge bg-secondary">Hidden</span>
                                    @endif
                                </td>
                                <td>
                                    <a href="{{ url('website/dealers/edit/'.$dealer->id) }}" class="btn btn-sm btn-outline-primary">Edit</a>
                                    <a href="{{ url('website/dealers/toggle/'.$dealer->id) }}" class="btn btn-sm btn-outline-warning">{{ $dealer->is_published ? 'Hide' : 'Publish' }}</a>
                                    <a href="{{ url('website/dealers/delete/'.$dealer->id) }}" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete this dealer?')">Delete</a>
                                </td>
                            </tr>
                            @empty
                            <tr><td colspan="8" class="text-center text-muted py-4">No dealers yet. Add your first area dealer.</td></tr>
                            @endforelse
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
