@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="row row-cols-1 row-cols-md-2 row-cols-xl-4">
         <div class="col">
           <div class="card radius-10 border-start border-0 border-3 border-info">
            <div class="card-body">
                <div class="d-flex align-items-center">
                    <div>
                        <p class="mb-0 text-secondary">Total Dealers</p>
                        <h4 class="my-1 text-info">{{number_format($users->where('role_id',2)->count())}}</h4>
                    </div>
                    <div class="widgets-icons-2 rounded-circle bg-gradient-scooter text-white ms-auto"><i class='bx bxs-group'></i>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card radius-10 border-start border-0 border-3 border-danger">
         <div class="card-body">
             <div class="d-flex align-items-center">
                 <div>
                     <p class="mb-0 text-secondary">Total Customers</p>
                     <h4 class="my-1 text-danger">{{number_format($users->where('role_id',3)->count())}}</h4>
                 </div>
                 <div class="widgets-icons-2 rounded-circle bg-gradient-bloody text-white ms-auto"><i class='bx bxs-group'></i>
                 </div>
             </div>
         </div>
     </div>
 </div>
 <div class="col">
    <div class="card radius-10 border-start border-0 border-3 border-success">
     <div class="card-body">
         <div class="d-flex align-items-center">
             <div>
                 <p class="mb-0 text-secondary">Active Customers</p>
                 <h4 class="my-1 text-success">{{number_format($users->where('role_id',3)->where('is_active',1)->count())}}</h4>
             </div>
             <div class="widgets-icons-2 rounded-circle bg-gradient-ohhappiness text-white ms-auto"><i class='bx bxs-group' ></i>
             </div>
         </div>
     </div>
 </div>
</div>
<div class="col">
    <div class="card radius-10 border-start border-0 border-3 border-warning">
     <div class="card-body">
         <div class="d-flex align-items-center">
             <div>
                 <p class="mb-0 text-secondary">Inactive Customers</p>
                 <h4 class="my-1 text-warning">{{number_format($users->where('role_id',3)->where('is_active',0)->count())}}</h4>
             </div>
             <div class="widgets-icons-2 rounded-circle bg-gradient-blooker text-white ms-auto"><i class='bx bxs-group'></i>
             </div>
         </div>
     </div>
 </div>
</div> 
</div><!--end row-->

<div class="row mt-3">
    <div class="col-lg-6">
        <div class="card">
            <div class="card-body">
                <h5 class="mb-3">Quick Actions</h5>
                <div class="d-flex flex-wrap gap-2">
                    <a href="{{ url('customers/add') }}" class="btn btn-primary"><i class='bx bx-user-plus'></i> New Customer</a>
                    <a href="{{ url('customers/all') }}" class="btn btn-outline-primary"><i class='bx bx-list-ul'></i> Customer List</a>
                    <a href="{{ url('dealer/all') }}" class="btn btn-outline-primary"><i class='bx bx-store'></i> Dealers</a>
                    <a href="{{ url('product-import') }}" class="btn btn-outline-primary"><i class='bx bx-import'></i> Product Import</a>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-xl-12">
        <div class="card border-top border-0 border-4 border-primary">
            <div class="card-body p-5">
                <div class="card-title d-flex align-items-center">
                    <div><i class="bx bx-key me-1 font-22 text-primary"></i>
                        <h5 class="mb-0 text-primary" style="display: inline;">Recent Franchise History</h5>
                    </div>
                </div>
                <hr>
                <div class="table-responsive">
                    <table id="myTable" class="table table-striped table-bordered" style="width:100%">
                        <thead>
                            <tr>
                                <th>Customer Name</th>
                                <th>App License Key</th>
                                <th>License Type</th>
                                <th>Expiry Date</th>
                                <th>Payment Status</th>
                                <th>Amount</th>
                                <th>License Status</th>
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
<script type="text/javascript">
$(document).ready(function(){
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
        ajax:"{{url('home')}}",
            "columns":[
            
            {
                "mData": "name",
                "bSortable": false,
            },
            {
                "mData": "licenseKey",
                "bSortable": false,
            },
            
            {
                "mData": "licenseType",
                "bSortable": false,
            },
            {
                "mData": "expiryDate",
                "bSortable": false,
            },

            {
                "mData": "paymentStatus",
                "bSortable": false,
            },{
                "mData": "amount",
                "bSortable": false,
            },
            {
                "targets":-1,
                "mData": "licenseStatus",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                    if(row.licenseStatus=='expired'){
                        return '<div class="badge rounded-pill text-danger bg-light-danger p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Expired</div>';
                    }else{
                        return '<div class="badge rounded-pill text-success bg-light-success p-2 text-uppercase px-3"><i class="bx bxs-circle me-1"></i>Active</div>';
                    }
                },
                
            },
            {
                "targets":-1,
                "mData": "id",
                "bSortable": false,
                "ilter":false,
                "mRender": function(data, type, row){
                        return '<a href={{url("dealer/edit")}}/'+row.dealerId+'><button type="button" class="btn btn-primary btn-sm radius-30 px-4">View Dealer</button></a>';              
                },
                
            },
            ]
            
        });
}
</script>
@endsection
