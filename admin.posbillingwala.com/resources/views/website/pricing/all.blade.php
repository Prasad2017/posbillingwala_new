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
                        <li class="breadcrumb-item active">Pricing</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <a href="{{ url('website/pricing/add') }}" class="btn btn-primary px-4"><i class="bx bx-plus"></i> Add Plan</a>
            </div>
        </div>
        <div class="card"><div class="card-body">
            <div class="table-responsive">
                <table class="table table-striped table-bordered mb-0">
                    <thead><tr><th>#</th><th>Type</th><th>Validity</th><th>Price (₹)</th><th>GST</th><th>Order</th><th>Status</th><th>Action</th></tr></thead>
                    <tbody>
                        @forelse($plans as $plan)
                        <tr>
                            <td>{{ $plan->id }}</td>
                            <td>{{ ucfirst($plan->plan_type) }}</td>
                            <td>{{ $plan->validity_label }}</td>
                            <td>{{ number_format($plan->price, 2) }}</td>
                            <td>{{ $plan->gst_note }}</td>
                            <td>{{ $plan->sort_order }}</td>
                            <td>@if($plan->is_published)<span class="badge bg-success">Published</span>@else<span class="badge bg-secondary">Hidden</span>@endif</td>
                            <td>
                                <a href="{{ url('website/pricing/edit/'.$plan->id) }}" class="btn btn-sm btn-outline-primary">Edit</a>
                                <a href="{{ url('website/pricing/toggle/'.$plan->id) }}" class="btn btn-sm btn-outline-warning">{{ $plan->is_published ? 'Hide' : 'Publish' }}</a>
                                <a href="{{ url('website/pricing/delete/'.$plan->id) }}" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete this plan?')">Delete</a>
                            </td>
                        </tr>
                        @empty
                        <tr><td colspan="8" class="text-center text-muted py-4">No pricing plans yet.</td></tr>
                        @endforelse
                    </tbody>
                </table>
            </div>
        </div></div>
    </div>
</div>
@endsection
