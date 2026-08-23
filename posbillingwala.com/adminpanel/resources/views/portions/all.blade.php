@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Product Portions</div>
            <div class="ps-3 text-secondary">{{ $customer->name }} / {{ $product->productName }}</div>
            <div class="ms-auto">
                <a href="{{ url('products/all') }}?customer_id={{ $customer->id }}" class="btn btn-outline-secondary btn-sm">Back to Products</a>
            </div>
        </div>

        <div class="row g-3">
            <div class="col-xl-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="mb-3">Add Portion</h5>
                        <form method="POST" action="{{ url('portions/add/'.$customer->id.'/'.$product->productId) }}">
                            @csrf
                            <div class="mb-3">
                                <label class="form-label">Portion Name</label>
                                <input type="text" name="portion_name" class="form-control" placeholder="Half / Full" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Price</label>
                                <input type="number" step="0.01" name="portion_price" class="form-control" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Sort Order</label>
                                <input type="number" name="portion_sort_order" class="form-control" value="0">
                            </div>
                            <button type="submit" class="btn btn-primary">Add</button>
                        </form>
                    </div>
                </div>
            </div>
            <div class="col-xl-8">
                <div class="card">
                    <div class="card-body">
                        <div class="table-responsive">
                            <table id="myTable" class="table table-striped table-bordered" style="width:100%">
                                <thead>
                                    <tr>
                                        <th>Name</th>
                                        <th>Price</th>
                                        <th>Sort</th>
                                        <th>Status</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script>
$(document).ready(function(){
    $("#myTable").DataTable({
        processing: true,
        serverSide: true,
        ajax: "{{ url('portions/all/'.$customer->id.'/'.$product->productId) }}",
        columns: [
            { mData: "portionName", bSortable: false },
            { mData: "portionPrice", bSortable: false },
            { mData: "portionSortOrder", bSortable: false },
            { mData: "portionStatus", bSortable: false },
            { mData: "portionId", bSortable: false, mRender: function(id, type, row){
                var label = row.portionStatus === 'active' ? 'Deactivate' : 'Activate';
                return '<a href="{{ url("portions/delete") }}/'+id+'" class="btn btn-sm btn-outline-danger">'+label+'</a>';
            }}
        ]
    });
});
</script>
@endsection
