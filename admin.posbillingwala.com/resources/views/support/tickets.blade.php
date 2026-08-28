@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h5 class="dash-hello mb-0">My Tickets</h5>
            <a href="{{ url('support/tickets/create') }}" class="btn btn-primary btn-sm">Create ticket</a>
        </div>
        <form class="mb-3" method="get">
            <select name="status" class="form-select pb-select-search" style="max-width:220px" onchange="this.form.submit()">
                <option value="all" @if($status==='all') selected @endif>All</option>
                <option value="open" @if($status==='open') selected @endif>Open</option>
                <option value="closed" @if($status==='closed') selected @endif>Closed</option>
            </select>
        </form>
        @forelse($tickets as $t)
            <a class="hub-row" href="{{ url('support/tickets/'.$t->id) }}">
                <span class="hub-icon kpi-icon blue"><i class='bx bx-envelope'></i></span>
                <div class="flex-grow-1">
                    <h6>{{ $t->subject }}</h6>
                    <p>{{ $t->ticket_no }} · {{ $t->app_name }} · {{ $t->category }}
                        @if(!empty($t->shop_name)) · {{ $t->shop_name }} @endif
                        · {{ $t->created_at }}</p>
                </div>
                <span class="status-badge {{ strtolower($t->status)==='open' ? 'status-trial' : 'status-active' }}">{{ $t->status }}</span>
            </a>
        @empty
            @include('layouts.empty-state', [
                'title' => 'No support tickets',
                'subtitle' => 'Create a ticket when you need help from the POS Billingwala team.',
                'actionUrl' => url('support/tickets/create'),
                'actionLabel' => 'Create Ticket',
            ])
        @endforelse
    </div>
</div>
@endsection
