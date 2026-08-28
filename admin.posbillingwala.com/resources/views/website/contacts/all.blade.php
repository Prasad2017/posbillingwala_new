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
                        <li class="breadcrumb-item active">Contact Enquiries</li>
                    </ol>
                </nav>
            </div>
        </div>

        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-striped table-bordered mb-0">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Subject</th>
                                <th>Status</th>
                                <th>Received</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            @forelse($contacts as $contact)
                            <tr>
                                <td>{{ $contact->id }}</td>
                                <td>{{ $contact->name }}</td>
                                <td>{{ $contact->email }}</td>
                                <td>{{ $contact->subject ?: '—' }}</td>
                                <td>
                                    @if($contact->status === 'New')
                                    <span class="badge bg-danger">New</span>
                                    @elseif($contact->status === 'Read')
                                    <span class="badge bg-warning text-dark">Read</span>
                                    @elseif($contact->status === 'Replied')
                                    <span class="badge bg-success">Replied</span>
                                    @else
                                    <span class="badge bg-secondary">{{ $contact->status }}</span>
                                    @endif
                                </td>
                                <td>{{ optional($contact->created_at)->format('d M Y, h:i A') }}</td>
                                <td><a href="{{ url('website/contacts/'.$contact->id) }}" class="btn btn-sm btn-outline-primary">View</a></td>
                            </tr>
                            @empty
                            <tr><td colspan="7" class="text-center text-muted py-4">No contact form submissions yet.</td></tr>
                            @endforelse
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
