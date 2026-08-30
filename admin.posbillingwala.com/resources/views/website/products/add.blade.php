@extends('layouts.app')
@section('content')
<div class="page-wrapper"><div class="page-content">@include('layouts.flash')
<div class="row"><div class="col-xl-8"><div class="card border-top border-0 border-4 border-primary"><div class="card-body p-5">
<h5 class="text-primary mb-3">Add Product</h5><form method="POST" action="{{ url('website/products/add') }}">@csrf @include('website.products._form')
<div class="mt-4"><button type="submit" class="btn btn-primary px-5">Add product</button><a href="{{ url('website/products') }}" class="btn btn-outline-secondary ms-2">Cancel</a></div></form>
</div></div></div></div></div></div>
@endsection
