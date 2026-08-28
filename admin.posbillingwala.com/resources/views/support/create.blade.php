@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <h5 class="dash-hello mb-3">Create Ticket</h5>
        <div class="card"><div class="card-body">
            <form method="post" action="{{ url('support/tickets') }}">
                @csrf
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label">App</label>
                        <select name="app_name" class="form-control">
                            @foreach(['POS App','Dealer App','Owner App','Admin App'] as $a)
                                <option>{{ $a }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Category</label>
                        <select name="category" class="form-control">
                            @foreach(['General','Billing','License','Device','Crash','Other'] as $c)
                                <option>{{ $c }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-12">
                        <label class="form-label">Subject</label>
                        <input class="form-control" name="subject" required>
                    </div>
                    <div class="col-12">
                        <label class="form-label">Description</label>
                        <textarea class="form-control" name="description" rows="5"></textarea>
                    </div>
                </div>
                <button class="btn btn-primary mt-3">Submit ticket</button>
            </form>
        </div></div>
    </div>
</div>
@endsection
