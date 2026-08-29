@extends('layouts.app')
@section('page_title', 'Import History')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        @include('layouts.page-header', [
            'title' => 'Catalog Import History',
            'subtitle' => 'Recent validate / import sessions for a customer.',
            'actionUrl' => route('catalog-import-export.index'),
            'actionLabel' => 'Back to Import / Export',
            'actionIcon' => 'bx-arrow-back',
        ])

        <div class="card pb-form-card">
            <div class="card-body">
                <form method="GET" action="{{ route('catalog-import-export.history') }}" class="row g-3 mb-4">
                    <div class="col-md-5">
                        <select name="user_id" class="form-select pb-select-search" required data-placeholder="Choose customer">
                            <option value="">Choose customer</option>
                            @foreach($users as $user)
                            <option value="{{ $user->id }}" @if($selectedCustomerId == $user->id) selected @endif>{{ $user->name }} — {{ $user->shopName }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-md-4">
                        <select name="import_type" class="form-select">
                            <option value="">All types</option>
                            @foreach($types as $typeKey => $typeName)
                            <option value="{{ $typeKey }}" @if($importType === $typeKey) selected @endif>{{ $typeName }}</option>
                            @endforeach
                        </select>
                    </div>
                    <div class="col-md-3">
                        <button type="submit" class="btn btn-primary w-100">Load History</button>
                    </div>
                </form>

                @if($selectedCustomerId > 0)
                    <div class="table-responsive">
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Type</th>
                                    <th>File</th>
                                    <th>Status</th>
                                    <th>Rows</th>
                                </tr>
                            </thead>
                            <tbody>
                                @forelse($history as $item)
                                    <tr>
                                        <td>{{ $item['created_at'] ?? '—' }}</td>
                                        <td>{{ ucfirst($item['importType'] ?? '—') }}</td>
                                        <td>{{ $item['fileName'] ?? '—' }}</td>
                                        <td>{{ ucfirst($item['status'] ?? '—') }}</td>
                                        <td>
                                            @if(($item['status'] ?? '') === 'imported')
                                                {{ (int) ($item['createdCount'] ?? 0) + (int) ($item['updatedCount'] ?? 0) }} imported
                                            @else
                                                {{ $item['validRows'] ?? 0 }} valid / {{ $item['totalRows'] ?? 0 }} total
                                            @endif
                                        </td>
                                    </tr>
                                @empty
                                    <tr><td colspan="5" class="text-secondary">No import history found.</td></tr>
                                @endforelse
                            </tbody>
                        </table>
                    </div>
                @endif
            </div>
        </div>
    </div>
</div>
@endsection
