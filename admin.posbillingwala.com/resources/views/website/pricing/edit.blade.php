@extends('layouts.app')
@section('content')
<div class="page-wrapper"><div class="page-content">@include('layouts.flash')
<div class="row"><div class="col-xl-8"><div class="card border-top border-0 border-4 border-primary"><div class="card-body p-5">
<h5 class="text-primary mb-3">Edit Pricing Plan</h5>
<form method="POST" action="{{ url('website/pricing/edit/'.$plan->id) }}">@csrf @include('website.pricing._form', ['plan' => $plan])
<div class="mt-4"><button type="submit" class="btn btn-primary px-5">Save plan</button><a href="{{ url('website/pricing') }}" class="btn btn-outline-secondary ms-2">Cancel</a></div>
</form></div></div></div></div></div></div>
@endsection
