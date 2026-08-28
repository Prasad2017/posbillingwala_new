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
                        <li class="breadcrumb-item"><a href="{{ url('website/contacts') }}">Contact Enquiries</a></li>
                        <li class="breadcrumb-item active">#{{ $contact->id }}</li>
                    </ol>
                </nav>
            </div>
        </div>

        <div class="row">
            <div class="col-xl-8">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <h5 class="text-primary mb-1">{{ $contact->name }}</h5>
                        <p class="mb-3"><a href="mailto:{{ $contact->email }}">{{ $contact->email }}</a></p>
                        <p class="text-muted small mb-4">Received {{ optional($contact->created_at)->format('d M Y, h:i A') }}</p>
                        <h6>Subject</h6>
                        <p>{{ $contact->subject ?: '(No subject)' }}</p>
                        <h6>Message</h6>
                        <p style="white-space:pre-wrap;">{{ $contact->message }}</p>

                        <form method="POST" action="{{ url('website/contacts/'.$contact->id.'/status') }}" class="row g-3 align-items-end mt-3">
                            @csrf
                            <div class="col-md-4">
                                <label class="form-label">Status</label>
                                <select class="form-select" name="status">
                                    @foreach(['New','Read','Replied','Closed'] as $s)
                                    <option value="{{ $s }}" @if($contact->status === $s) selected @endif>{{ $s }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-4">
                                <button type="submit" class="btn btn-primary">Update status</button>
                                <a href="{{ url('website/contacts') }}" class="btn btn-outline-secondary ms-2">Back</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
