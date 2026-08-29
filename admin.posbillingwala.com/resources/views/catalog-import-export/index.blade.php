@extends('layouts.app')
@section('page_title', 'Catalog Import / Export')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Catalog Import / Export',
            'subtitle' => 'Validate, preview, and confirm — same flow as the Android admin app.',
            'actionUrl' => url('import-export'),
            'actionLabel' => 'Import / Export Hub',
            'actionIcon' => 'bx-grid-alt',
        ])

        <div class="row g-3">
            <div class="col-xl-8">
                <div class="card pb-form-card">
                    <div class="card-body">
                        <div class="d-flex flex-wrap gap-2 mb-3">
                            @foreach($types as $typeKey => $typeName)
                                <a href="{{ route('catalog-import-export.index', ['type' => $typeKey]) }}"
                                   class="btn btn-sm {{ $importType === $typeKey ? 'btn-primary' : 'btn-outline-primary' }}">
                                    {{ $typeName }}
                                </a>
                            @endforeach
                        </div>

                        <h6 class="mb-3">{{ $typeLabel }} — Import</h6>
                        <p class="text-secondary mb-3">Upload an Excel <strong>.xlsx</strong> file using the Billingwala catalog template. The server validates every row before import.</p>

                        <form method="POST" action="{{ route('catalog-import-export.validate') }}" enctype="multipart/form-data" class="row g-3">
                            @csrf
                            <input type="hidden" name="import_type" value="{{ $importType }}">
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
                                <label class="form-label">Import File (.xlsx)</label>
                                <input type="file" name="import_file" class="form-control" accept=".xlsx" required>
                                <small class="text-secondary">Max 10 MB.</small>
                            </div>
                            <div class="col-12 d-flex flex-wrap gap-2">
                                <button type="submit" class="btn btn-primary">
                                    <i class='bx bx-check-shield'></i> Validate &amp; Preview
                                </button>
                            </div>
                        </form>

                        <hr class="my-4">

                        <h6 class="mb-3">{{ $typeLabel }} — Export &amp; Template</h6>
                        <form method="POST" action="{{ route('catalog-import-export.export') }}" class="row g-3 mb-3">
                            @csrf
                            <input type="hidden" name="import_type" value="{{ $importType }}">
                            <div class="col-md-8">
                                <select name="user_id" class="form-select pb-select-search" required data-placeholder="Choose customer">
                                    <option value="">Choose customer</option>
                                    @foreach($users as $user)
                                    <option value="{{ $user->id }}">{{ $user->name }} — {{ $user->shopName }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-4">
                                <button type="submit" class="btn btn-success w-100">
                                    <i class='bx bx-export'></i> Export {{ $typeLabel }}
                                </button>
                            </div>
                        </form>

                        <form method="POST" action="{{ route('catalog-import-export.template') }}" class="row g-3">
                            @csrf
                            <input type="hidden" name="import_type" value="{{ $importType }}">
                            <div class="col-md-8">
                                <select name="user_id" class="form-select pb-select-search" required data-placeholder="Choose customer">
                                    <option value="">Choose customer</option>
                                    @foreach($users as $user)
                                    <option value="{{ $user->id }}">{{ $user->name }} — {{ $user->shopName }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-4">
                                <button type="submit" class="btn btn-outline-primary w-100">
                                    <i class='bx bx-download'></i> Demo Template
                                </button>
                            </div>
                        </form>

                        <div class="mt-3">
                            <a href="{{ route('catalog-import-export.history', ['import_type' => $importType]) }}" class="btn btn-link px-0">
                                View import history
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
