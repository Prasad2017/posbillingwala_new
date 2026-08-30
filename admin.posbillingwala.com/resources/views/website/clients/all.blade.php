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
                        <li class="breadcrumb-item active">Clients</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <a href="{{ url('website/clients/add') }}" class="btn btn-primary px-4"><i class="bx bx-plus"></i> Add Client</a>
            </div>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-striped table-bordered mb-0">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Logo</th>
                                <th>Business</th>
                                <th>City</th>
                                <th>Category</th>
                                <th>Order</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            @forelse($clients as $client)
                            <tr>
                                <td>{{ $client->id }}</td>
                                <td>
                                    @php $logo = \App\Services\WebsiteMedia::url($client->logo_path); @endphp
                                    @if($logo)
                                    <img src="{{ $logo }}" alt="" style="width:42px;height:42px;object-fit:cover;border-radius:8px;">
                                    @else
                                    <span class="text-muted">—</span>
                                    @endif
                                </td>
                                <td>{{ $client->business_name }}</td>
                                <td>{{ $client->city ?: '—' }}</td>
                                <td>{{ $client->business_category ?: '—' }}</td>
                                <td>{{ $client->sort_order }}</td>
                                <td>
                                    @if($client->is_published)
                                    <span class="badge bg-success">Published</span>
                                    @else
                                    <span class="badge bg-secondary">Hidden</span>
                                    @endif
                                </td>
                                <td>
                                    <a href="{{ url('website/clients/edit/'.$client->id) }}" class="btn btn-sm btn-outline-primary">Edit</a>
                                    <a href="{{ url('website/clients/toggle/'.$client->id) }}" class="btn btn-sm btn-outline-warning">{{ $client->is_published ? 'Hide' : 'Publish' }}</a>
                                    <a href="{{ url('website/clients/delete/'.$client->id) }}" class="btn btn-sm btn-outline-danger" onclick="return confirm('Delete this client?')">Delete</a>
                                </td>
                            </tr>
                            @empty
                            <tr><td colspan="8" class="text-center text-muted py-4">No clients yet. Add your first customer showcase.</td></tr>
                            @endforelse
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
