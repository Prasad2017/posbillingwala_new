@extends('layouts.app')
@section('page_title', 'Licenses')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'All Licenses',
            'subtitle' => 'License keys and validity — filter by customer.',
            'actionUrl' => url('customers/all'),
            'actionLabel' => 'All Customers',
            'actionIcon' => 'bx-group',
        ])
        @if(isset($customers) && count($customers))
        <div class="row mb-3">
            <div class="col-md-4">
                <select id="customerFilter" class="form-select pb-select-search" data-placeholder="All customers" onchange="getSearchFilter()">
                    <option value="">All Customers</option>
                    @foreach($customers as $customer)
                    <option value="{{ $customer->id }}">{{ $customer->name }} — {{ $customer->shopName }}</option>
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
                                <th>Customer</th>
                                <th>Shop</th>
                                <th>License Key</th>
                                <th>User Type</th>
                                <th>Branch</th>
                                <th>Expiry</th>
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
    if (params.get('customer_id')) {
        $("#customerFilter").val(params.get('customer_id'));
    }
    myTable();
});

function getSearchFilter() {
    $("#myTable").DataTable().clear().destroy();
    myTable();
}

function myTable() {
    $("#myTable").dataTable({
        processing: true,
        serverSide: true,
        responsive: true,
        searching: true,
        lengthChange: true,
        ajax: "{{ url('customers/all-license') }}?customer_id=" + ($("#customerFilter").val() || ''),
        columns: [
            { mData: "customerName", bSortable: false },
            { mData: "shopName", bSortable: false },
            { mData: "licenseKey", bSortable: false },
            { mData: "userType", bSortable: false },
            { mData: "branchName", bSortable: false, mRender: function(data){ return data || '-'; } },
            { mData: "expiryDate", bSortable: false },
            { mData: "licenseStatus", bSortable: false, mRender: function(data) {
                if (data === 'active') {
                    return '<span class="badge bg-light-success text-success">Active</span>';
                }
                return '<span class="badge bg-light-danger text-danger">' + (data || '-') + '</span>';
            }},
            { mData: "id", bSortable: false, mRender: function(id) {
                return '<a href="{{ url("customers/edit-license") }}/' + id + '" class="btn btn-sm btn-primary"><i class="bx bx-edit-alt"></i></a>';
            }}
        ]
    });
}
</script>
@endsection
