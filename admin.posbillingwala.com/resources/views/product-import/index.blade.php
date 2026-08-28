@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <div class="page-breadcrumb d-none d-sm-flex align-items-center mb-3">
            <div class="breadcrumb-title pe-3">Product Import</div>
        </div>

        <div class="row">
            <div class="col-xl-8">
                <div class="card border-top border-0 border-4 border-primary">
                    <div class="card-body p-5">
                        <div class="card-title d-flex align-items-center">
                            <div><i class="bx bx-import me-1 font-22 text-primary"></i></div>
                            <h5 class="mb-0 text-primary">Bulk Product Import</h5>
                        </div>
                        <hr>
                        <p class="text-secondary">Upload a <strong>CSV</strong> or <strong>Excel (.xlsx / .xls)</strong> file with columns: <strong>Product, Category, Unit, Price, CGST, SGST</strong> (same format as the Android app template).</p>
                        <p class="mb-4">
                            <a href="http://www.posbillingwala.com/androidApp/DemoExcel/CustomerProductList.xlsx" target="_blank" class="btn btn-outline-primary btn-sm">
                                <i class='bx bx-download'></i> Download Excel Template
                            </a>
                        </p>
                        <form method="POST" action="{{ url('product-import/upload') }}" enctype="multipart/form-data" class="row g-3">
                            @csrf
                            <div class="col-md-6">
                                <label class="form-label">Select Customer</label>
                                <select name="user_id" class="form-select" required>
                                    <option value="">Choose customer</option>
                                    @foreach($users as $user)
                                    <option value="{{ $user->id }}" @if(old('user_id') == $user->id) selected @endif>{{ $user->name }} — {{ $user->shopName }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Import File (CSV or Excel)</label>
                                <input type="file" name="import_file" class="form-control" accept=".csv,.txt,.xlsx,.xls" required>
                                <small class="text-secondary">Max 10 MB. First row can be a header row.</small>
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary px-5">Import Products</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
