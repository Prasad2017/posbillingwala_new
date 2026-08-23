@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <!--breadcrumb-->
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Categories</div>
            <div class="ps-3">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 p-0">
                        <li class="breadcrumb-item"><a href="javascript:;"><i class="bx bx-home-alt"></i></a>
                        </li>
                        <li class="breadcrumb-item active" aria-current="page">Categories</li>
                    </ol>
                </nav>
            </div>
            <div class="ms-auto">
                <div class="btn-group">
                    <a href="{{url('categories/add')}}"><button type="button" class="btn btn-primary px-5"><i class="bx bx-plus mr-1"></i>Add Category</button></a>
                </div>
            </div>
        </div>
        <!--end breadcrumb-->
        <h6 class="mb-0 text-uppercase">All Categories</h6>
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
                                <th>Category Name</th>
                                <th>Food Type</th>
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
                "width": "25%",
                "targets": "_all" 
            }],
        ajax:"{{url('categories/all')}}?customer_id="+($("#customerFilter").val() || ''),
            "columns":[
            {
                "mData": "categoryId",
                "bSortable": false,
            },
            {
                "mData": "categoryName",
                "bSortable": false,
            },
            {
                "mData": "foodTypeName",
                "bSortable": false,
                "mRender": function(data){ return data || '-'; }
            },
            {
                "targets":-1,
                "mData": "categoryStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    if(row.categoryStatus=='inactive'){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Inactive</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            {
                "targets":-1,
                "mData": "categoryStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    var actions = '<div class="d-flex flex-wrap gap-1 order-actions">';
                    actions += '<a href="{{url("categories/edit")}}/'+row.categoryId+'" class="btn btn-sm btn-light" title="Edit"><i class="bx bxs-edit"></i></a>';
                    actions += '<a href="{{url("subcategories/all")}}/'+row.userId+'/'+row.categoryId+'" class="btn btn-sm btn-light" title="Subcategories"><i class="bx bx-list-ul"></i></a>';
                    actions += '<a href="{{url("categories/delete")}}/'+row.categoryId+'" class="btn btn-sm btn-light" title="Toggle"><i class="bx bxs-trash"></i></a>';
                    actions += '</div>';
                    return actions;
                },
                
            },
            ]
            
        });
}
</script>
@endsection