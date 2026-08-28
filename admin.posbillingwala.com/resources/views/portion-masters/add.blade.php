@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="card border-top border-0 border-4 border-primary">
            <div class="card-body p-5">
                <h5 class="mb-0 text-primary">Add Portion Master</h5>
                <p class="text-secondary mt-2">Name only — no price. Assign prices when adding portions to a product.</p>
                <hr>
                <form class="row g-3" method="POST" action="{{url('portion-masters/add')}}">
                    @csrf
                    @if(Auth::user()->role_id==2 || Auth::user()->role_id==1)
                    <div class="col-md-4">
                        <label class="form-label">Customer</label>
                        <select class="form-select" name="user_id" required>
                            <option value="">Select</option>
                            @foreach($users as $user)
                            <option value="{{$user->id}}">{{$user->name}}</option>
                            @endforeach
                        </select>
                    </div>
                    @endif
                    <div class="col-md-4">
                        <label class="form-label">Portion Name</label>
                        <input type="text" class="form-control" name="portion_name" placeholder="Half / Full / Small" required maxlength="64">
                    </div>
                    <div class="col-12">
                        <button type="submit" class="btn btn-primary px-5">Save</button>
                        <a href="{{url('portion-masters/all')}}" class="btn btn-outline-secondary">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
@endsection
