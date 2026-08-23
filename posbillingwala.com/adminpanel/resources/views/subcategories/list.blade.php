@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Subcategories</div>
            <div class="ps-3">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 p-0">
                        <li class="breadcrumb-item"><a href="javascript:;"><i class="bx bx-home-alt"></i></a></li>
                        <li class="breadcrumb-item active" aria-current="page">Subcategories</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <div class="btn-group">
                    <a href="{{url('subcategories/add')}}"><button type="button" class="btn btn-primary px-5"><i class="bx bx-plus mr-1"></i>Add Subcategory</button></a>
                </div>
            </div>
        </div>
        <h6 class="mb-0 text-uppercase">All Subcategories</h6>
        <hr/>
        @if(isset($customers) && count($customers))
        <div class="row mb-3">
            <div class="col-md-4">
                <select id="customerFilter" class="form-select" onchange="getSearchFilter()">
                    <option value="">All Customers</option>
                    @foreach($customers as $customer)
                    <option value="{{ $customer->id }}">{{ $customer->name }}</option>
                    @endforeach
                </select>
            </div>
        </div>
        @endif
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table id="myTable" class="table table-striped table-bordered" style="width:100%">
                        <thead>
                            <tr>
                                <th>Id</th>
                                <th>Subcategory Name</th>
                                <th>Category</th>
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
<script type="text/javascript">
$(document).ready(function(){
    var params = new URLSearchParams(window.location.search);
    if(params.get('customer_id')) {
        $("#customerFilter").val(params.get('customer_id'));
    }
    myTable();
});

function getSearchFilter()
{
    $("#myTable").DataTable().clear().destroy();
    myTable();
}

function myTable()
{
    $("#myTable").dataTable({
        "processing": true,
        "serverSide": true,
        "responsive": true,
        "searching": true,
        "lengthChange": true,
        "columnDefs": [{
            "width": "20%",
            "targets": "_all"
        }],
        ajax:"{{url('subcategories/all')}}?customer_id="+($("#customerFilter").val() || ''),
        "columns":[
            {
                "mData": "subcategoryId",
                "bSortable": false,
            },
            {
                "mData": "subcategoryName",
                "bSortable": false,
            },
            {
                "mData": "categoryName",
                "bSortable": false,
                "mRender": function(data){ return data || '-'; }
            },
            {
                "mData": "subcategoryStatus",
                "bSortable": false,
                "mRender": function(data, type, row){
                    if(row.subcategoryStatus=='inactive'){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Inactive</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
            },
            {
                "mData": "subcategoryId",
                "bSortable": false,
                "mRender": function(data, type, row){
                    var label = row.subcategoryStatus === 'active' ? 'Deactivate' : 'Activate';
                    return '<a href="{{ url("subcategories/delete") }}/'+data+'" class="btn btn-sm btn-outline-danger">'+label+'</a>';
                },
            },
        ]
    });
}
</script>
@endsection
