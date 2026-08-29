@extends('layouts.app')
@section('page_title', 'Product Import')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Product Import',
            'subtitle' => 'Upload CSV or Excel — columns: Product, Category, Unit, Price, CGST, SGST.',
            'actionUrl' => url('import-export'),
            'actionLabel' => 'Import / Export Hub',
            'actionIcon' => 'bx-grid-alt',
        ])

        <div class="row g-3">
            <div class="col-xl-8">
                <div class="card pb-form-card">
                    <div class="card-body">
                        <div class="d-flex align-items-center gap-2 mb-3">
                            <span class="pb-form-icon"><i class='bx bx-cloud-upload'></i></span>
                            <h6 class="mb-0">Bulk Product Import</h6>
                        </div>
                        <p class="text-secondary mb-3">Upload a <strong>CSV</strong> or <strong>Excel (.xlsx / .xls)</strong> file. The first row can be a header row.</p>
                        <p class="mb-4">
                            <a href="http://www.posbillingwala.com/androidApp/DemoExcel/CustomerProductList.xlsx" target="_blank" rel="noopener noreferrer" class="btn btn-outline-primary btn-sm">
                                <i class='bx bx-download'></i> Download Excel Template
                            </a>
                        </p>
                        <form method="POST" action="{{ url('product-import/upload') }}" enctype="multipart/form-data" class="row g-3">
                            @csrf
                            <div class="col-md-6">
                                <label class="form-label">Select Customer</label>
                                <select name="user_id" class="form-select pb-select-search" required data-placeholder="Choose customer">
                                    <option value="">Choose customer</option>
                                    @foreach($users as $user)
                                    <option value="{{ $user->id }}" @if(old('user_id') == $user->id) selected @endif>{{ $user->name }} — {{ $user->shopName }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Import File (CSV or Excel)</label>
                                <input type="file" name="import_file" class="form-control" accept=".csv,.txt,.xlsx,.xls" required>
                                <small class="text-secondary">Max 10 MB.</small>
                            </div>
                            <div class="col-12">
                                <button type="submit" class="btn btn-primary">
                                    <i class='bx bx-import'></i> Import Products
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
