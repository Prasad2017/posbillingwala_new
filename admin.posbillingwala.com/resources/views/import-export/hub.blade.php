@extends('layouts.app')
@section('page_title', 'Import / Export')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Import / Export',
            'subtitle' => 'Bulk catalog tools — validate, preview, and confirm before import.',
        ])

        <div class="row g-3">
            @foreach([
                'products' => ['Products', 'Upload or export full product catalog with portions', 'bx-package'],
                'categories' => ['Categories', 'Import or export category master', 'bx-category'],
                'subcategories' => ['Sub Categories', 'Import or export sub categories linked to categories', 'bx-list-ul'],
                'portions' => ['Portions', 'Import or export portion master names', 'bx-food-menu'],
            ] as $type => [$label, $desc, $icon])
            <div class="col-lg-6">
                <a class="hub-row h-100" href="{{ route('catalog-import-export.index', ['type' => $type]) }}">
                    <span class="hub-icon blue"><i class='bx {{ $icon }}'></i></span>
                    <span class="flex-grow-1">
                        <strong>{{ $label }}</strong>
                        <small class="d-block text-secondary">{{ $desc }}</small>
                    </span>
                    <i class='bx bx-chevron-right'></i>
                </a>
            </div>
            @endforeach
            <div class="col-lg-6">
                <a class="hub-row h-100" href="{{ route('catalog-import-export.history') }}">
                    <span class="hub-icon green"><i class='bx bx-history'></i></span>
                    <span class="flex-grow-1">
                        <strong>Import History</strong>
                        <small class="d-block text-secondary">View recent validate and import sessions</small>
                    </span>
                    <i class='bx bx-chevron-right'></i>
                </a>
            </div>
        </div>
    </div>
</div>
@endsection
