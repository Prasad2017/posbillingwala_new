@extends('layouts.app')
@section('content')
<div class="page-wrapper"><div class="page-content">@include('layouts.flash')
<div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
<div class="breadcrumb-title pe-3">Website</div><div class="ps-3"><nav aria-label="breadcrumb"><ol class="breadcrumb mb-0 p-0"><li class="breadcrumb-item"><a href="{{ url('website') }}"><i class="bx bx-home-alt"></i></a></li><li class="breadcrumb-item active">Products</li></ol></nav></div>
<div class="ms-auto"><a href="{{ url('website/products/add') }}" class="btn btn-primary px-4"><i class="bx bx-plus"></i> Add Product</a></div>
</div>
<div class="card"><div class="card-body"><div class="table-responsive">
<table class="table table-striped table-bordered mb-0"><thead><tr><th>#</th><th>Icon</th><th>Name</th><th>Category</th><th>Order</th><th>Status</th><th>Action</th></tr></thead>
<tbody>@forelse($products as $product)<tr><td>{{ $product->id }}</td><td>{{ $product->icon ?: '—' }}</td><td>{{ $product->name }}</td><td>{{ ucfirst($product->category) }}</td><td>{{ $product->sort_order }}</td>
<td>@if($product->is_published)<span class="badge bg-success">Published</span>@else<span class="badge bg-secondary">Hidden</span>@endif</td>
<td><a href="{{ url('website/products/edit/'.$product->id) }}" class="btn btn-sm btn-outline-primary">Edit</a>
<a href="{{ url('website/products/toggle/'.$product->id) }}" class="btn btn-sm btn-outline-warning">{{ $product->is_published ? 'Hide' : 'Publish' }}</a>
<a href="{{ url('website/products/delete/'.$product->id) }}" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete?')">Delete</a></td></tr>@empty<tr><td colspan="7" class="text-center text-muted py-4">No products yet.</td></tr>@endforelse</tbody></table>
</div></div></div></div></div>
@endsection
