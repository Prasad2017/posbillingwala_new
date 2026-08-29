@extends('layouts.app')
@section('page_title', 'Product Export')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Product Export',
            'subtitle' => 'Download an Excel workbook with products, categories, subcategories, and portions.',
            'actionUrl' => url('import-export'),
            'actionLabel' => 'Import / Export Hub',
            'actionIcon' => 'bx-grid-alt',
        ])

        <div class="row g-3">
            <div class="col-xl-8">
                <div class="card pb-form-card">
                    <div class="card-body">
                        <div class="d-flex align-items-center gap-2 mb-3">
                            <span class="pb-form-icon"><i class='bx bx-export'></i></span>
                            <h6 class="mb-0">Export Customer Catalog</h6>
                        </div>
                        <p class="text-secondary">The exported file includes <strong>Instructions</strong>, <strong>Products</strong>, <strong>Categories</strong>, <strong>Sub Categories</strong>, and <strong>Portions</strong> sheets — ready for re-import after edits.</p>
                        <form method="POST" action="{{ url('product-export/download') }}" class="row g-3">
                            @csrf
                            <div class="col-md-8">
                                <label class="form-label">Select Customer</label>
                                <select name="user_id" class="form-select pb-select-search" required data-placeholder="Choose customer">
                                    <option value="">Choose customer</option>
                                    @foreach($users as $user)
                                    <option value="{{ $user->id }}" @if(old('user_id') == $user->id) selected @endif>{{ $user->name }} — {{ $user->shopName }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-4 d-flex align-items-end">
                                <button type="submit" class="btn btn-primary w-100">
                                    <i class='bx bx-download'></i> Download Excel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            <div class="col-xl-4">
                <div class="card pb-form-card h-100">
                    <div class="card-body">
                        <h6 class="section-title">Export columns</h6>
                        <ul class="pb-check-list mb-0">
                            <li>Product Name &amp; Code</li>
                            <li>Category &amp; Sub Category</li>
                            <li>Portion &amp; Price</li>
                            <li>GST, Unit, Status</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
