@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="row">
            <div class="col-xl-9">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <h5 class="text-primary mb-3">Edit Dealer — {{ $dealer->area }}</h5>
                        <form method="POST" action="{{ url('website/dealers/edit/'.$dealer->id) }}">
                            @csrf
                            @include('website.dealers._form', ['dealer' => $dealer])
                            <div class="mt-4">
                                <button type="submit" class="btn btn-primary px-5">Save dealer</button>
                                <a href="{{ url('website/dealers') }}" class="btn btn-outline-secondary ms-2">Cancel</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
