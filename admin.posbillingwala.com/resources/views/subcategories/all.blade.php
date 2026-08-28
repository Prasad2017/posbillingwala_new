@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Subcategories</div>
            <div class="ps-3 text-secondary">{{ $customer->name }} / {{ $category->categoryName }}</div>
            <div class="ms-auto">
                <a href="{{ url('subcategories/all') }}" class="btn btn-outline-secondary btn-sm">All Subcategories</a>
                <a href="{{ url('categories/all') }}?customer_id={{ $customer->id }}" class="btn btn-outline-secondary btn-sm">Back to Categories</a>
            </div>
        </div>

        <div class="row g-3">
            <div class="col-xl-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="mb-3">Add Subcategory</h5>
                        <form method="POST" action="{{ url('subcategories/add/'.$customer->id.'/'.$category->categoryId) }}">
                            @csrf
                            <div class="mb-3">
                                <label class="form-label">Subcategory Name</label>
                                <input type="text" name="subcategory_name" class="form-control" required>
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
        ajax: "{{ url('subcategories/all/'.$customer->id.'/'.$category->categoryId) }}",
        columns: [
            { mData: "subcategoryName", bSortable: false },
            { mData: "subcategoryStatus", bSortable: false },
            { mData: "subcategoryId", bSortable: false, mRender: function(id, type, row){
                var label = row.subcategoryStatus === 'active' ? 'Deactivate' : 'Activate';
                return '<a href="{{ url("subcategories/delete") }}/'+id+'" class="btn btn-sm btn-outline-danger">'+label+'</a>';
            }}
        ]
    });
});
</script>
@endsection
