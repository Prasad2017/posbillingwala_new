@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="row row-cols-1 row-cols-md-2 row-cols-xl-3">
           <div class="col">
             <div class="card radius-10 border-start border-0 border-3 border-info">
                <div class="card-body">
                    <div class="d-flex align-items-center">
                        <div>
                            <p class="mb-0 text-secondary">Total Categories</p>
                            <h4 class="my-1 text-info">{{number_format($categories->count())}}</h4>
                        </div>
                        <div class="widgets-icons-2 rounded-circle bg-gradient-scooter text-white ms-auto"><i class='bx bx-cart'></i>
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
                           <p class="mb-0 text-secondary">Total Products</p>
                           <h4 class="my-1 text-danger">{{number_format($products->count())}}</h4>
                       </div>
                       <div class="widgets-icons-2 rounded-circle bg-gradient-bloody text-white ms-auto"><i class='bx bx-cookie'></i>
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
                       <p class="mb-0 text-secondary">Total Expenses</p>
                       <h4 class="my-1 text-success">{{number_format($expenses->sum('expensesAmount'))}}</h4>
                   </div>
                   <div class="widgets-icons-2 rounded-circle bg-gradient-ohhappiness text-white ms-auto"><i class='bx bxs-group' ></i>
                   </div>
               </div>
           </div>
       </div>
   </div>
</div><!--end row-->

<div class="row">
    <div class="col-xl-12">
        <div class="card border-top border-0 border-4 border-primary">
            <div class="card-body p-5" style="padding-top: 25px !important;">
                <div class="row">
                    <div class="col-lg-12">
                        <h4 class="text-primary" style="padding-bottom: 15px;">Total Outlets</h4>
                    </div>
                    <div class="col-lg-12">
                        <div class="card-title d-flex align-items-center">
                            <div><i class="bx bx-file me-1 font-22 text-primary"></i>
                                <h5 class="mb-0 text-primary" style="display: inline;">Total Sale</h5>
                            </div>
                        </div>
                        <hr>
                        <div class="table-responsive">
                            <table id="myTable" class="table table-striped table-bordered" style="width:100%">
                                <thead>
                                    <tr>
                                        <th>Sr No.</th>
                                        <th>License</th>
                                        <th>Address</th>
                                        <th>Sale</th>
                                    </tr>
                                </thead>

                            </table>
                        </div>
                    </div>
                    <div class="col-lg-12" style="margin-top: 10px;">
                        <div class="card-title d-flex align-items-center">
                            <div><i class="bx bx-file me-1 font-22 text-primary"></i>
                                <h5 class="mb-0 text-primary" style="display: inline;">Today's Sale</h5>
                            </div>
                        </div>
                        <hr>
                        <div class="table-responsive">
                            <table id="myTable1" class="table table-striped table-bordered" style="width:100%">
                                <thead>
                                    <tr>
                                        <th>Sr No.</th>
                                        <th>License</th>
                                        <th>Address</th>
                                        <th>Sale</th>
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

</div>
</div>
<script type="text/javascript">
    $(document).ready(function(){
        $("#myTable").dataTable({
            "processing": true,
            "serverSide": true,
            "responsive": true,
            "searching": false,
            "lengthChange": false,
            "columnDefs": [{
                "width": "25%",
                "targets": "_all" 
            }],
            ajax:"{{url('home')}}?total_sale=1",
            "columns":[
            
            {
                "mData": "sr",
                "bSortable": false,
            },
            {
                "mData": "licenseKey",
                "bSortable": false,
            },
            
            {
                "mData": "companyAddress",
                "bSortable": false,
            },
            {
                "mData": "sale",
                "bSortable": false,
            },
            ]
            
        });

        $("#myTable1").dataTable({
            "processing": true,
            "serverSide": true,
            "responsive": true,
            "searching": false,
            "lengthChange": false,
            "columnDefs": [{
                "width": "25%",
                "targets": "_all" 
            }],
            ajax:"{{url('home')}}?total_sale=0",
            "columns":[
            
            {
                "mData": "sr",
                "bSortable": false,
            },

            {
                "mData": "licenseKey",
                "bSortable": false,
            },
            
            {
                "mData": "companyAddress",
                "bSortable": false,
            },
            {
                "mData": "sale",
                "bSortable": false,
            },
            ]
            
        });
    });
</script>
@endsection
