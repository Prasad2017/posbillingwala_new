@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Portion Master</div>
            <div class="ms-auto">
                <a href="{{url('portion-masters/add')}}" class="btn btn-primary px-4"><i class="bx bx-plus"></i> Add Portion</a>
            </div>
        </div>
        <div class="alert alert-info">
            Portion Master stores <strong>names only</strong> (Half, Full, Small…). Prices are set on each Product under Portions.
        </div>
        @if(isset($customers) && count($customers))
        <div class="row mb-3">
            <div class="col-md-4">
                <select id="customerFilter" class="form-select" onchange="reloadTable()">
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
                                <th>Portion Name</th>
                                <th>Customer</th>
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
<script>
function reloadTable(){ $("#myTable").DataTable().ajax.reload(); }
$(document).ready(function(){
    $("#myTable").DataTable({
        processing: true,
        serverSide: true,
        ajax: {
            url: "{{ url('portion-masters/all') }}",
            data: function(d){ d.customer_id = $("#customerFilter").val() || ''; }
        },
        columns: [
            { mData: "portionMasterId", bSortable: false },
            { mData: "portionName", bSortable: false },
            { mData: "customerName", bSortable: false, mRender: function(data, type, row){
                if (data) { return data + (row.shopName ? ' — ' + row.shopName : ''); }
                return row.userId || '-';
            }},
            { mData: "portionMasterStatus", bSortable: false },
            { mData: "portionMasterId", bSortable: false, mRender: function(id, type, row){
                var label = row.portionMasterStatus === 'active' ? 'Deactivate' : 'Activate';
                return '<a href="{{ url("portion-masters/toggle") }}/'+id+'" class="btn btn-sm btn-outline-danger">'+label+'</a>';
            }}
        ]
    });
});
</script>
@endsection
