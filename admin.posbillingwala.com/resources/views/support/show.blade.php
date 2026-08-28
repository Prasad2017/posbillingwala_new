@extends('layouts.app')
@section('content')
<div class="page-wrapper">
    <div class="page-content">
        @include('layouts.flash')
        <a href="{{ url('support/tickets') }}" class="mb-3 d-inline-block">&larr; Back to tickets</a>
        <div class="card mb-3"><div class="card-body">
            <h5 class="dash-hello">{{ $ticket->subject }}</h5>
            <p class="text-secondary mb-0">
                {{ $ticket->ticket_no }} · {{ $ticket->app_name }} · {{ $ticket->category }} ·
                <span class="status-badge status-{{ strtolower($ticket->status)==='open' ? 'trial' : 'active' }}">{{ $ticket->status }}</span>
            </p>
            @if(!empty($ticket->shop_name))
                <p class="text-secondary mb-0 mt-1"><i class='bx bx-store'></i> {{ $ticket->shop_name }}
                    @if(!empty($ticket->device_name)) · {{ $ticket->device_name }} @endif
                </p>
            @endif
            @if($ticket->description)
                <p class="mt-2 mb-0">{{ $ticket->description }}</p>
            @endif
            <div class="table-actions-row mt-3">
                <form method="post" action="{{ url('support/tickets/'.$ticket->id.'/status') }}" class="d-inline">
                    @csrf
                    <input type="hidden" name="status" value="Resolved">
                    <button class="btn btn-sm btn-success" type="submit"><i class='bx bx-check'></i> Mark resolved</button>
                </form>
                <form method="post" action="{{ url('support/tickets/'.$ticket->id.'/status') }}" class="d-inline">
                    @csrf
                    <input type="hidden" name="status" value="Closed">
                    <button class="btn btn-sm btn-outline-secondary" type="submit"><i class='bx bx-x'></i> Close ticket</button>
                </form>
                @if(strtolower($ticket->status) !== 'open')
                <form method="post" action="{{ url('support/tickets/'.$ticket->id.'/status') }}" class="d-inline">
                    @csrf
                    <input type="hidden" name="status" value="Open">
                    <button class="btn btn-sm btn-outline-primary" type="submit"><i class='bx bx-revision'></i> Reopen</button>
                </form>
                @endif
            </div>
        </div></div>
        <div class="card mb-3"><div class="card-body">
            <h6 class="section-title">Conversation</h6>
            @forelse($ticket->messages as $m)
                <div class="border rounded p-3 mb-2 {{ $m->sender === 'You' ? 'bg-light' : '' }}">
                    <strong>{{ $m->sender }}</strong>
                    <small class="text-secondary">{{ $m->created_at }}</small>
                    <p class="mb-0 mt-1">{{ $m->message }}</p>
                </div>
            @empty
                @include('layouts.empty-state', [
                    'title' => 'No replies yet',
                    'subtitle' => 'Send a reply below. The POS app user will see it when online.',
                ])
            @endforelse
        </div></div>
        <div class="card"><div class="card-body">
            <form method="post" action="{{ url('support/tickets/'.$ticket->id.'/reply') }}">
                @csrf
                <textarea name="message" class="form-control pb-field mb-2" rows="3" required placeholder="Write a reply to the customer"></textarea>
                <button class="btn btn-primary">Send reply</button>
            </form>
            <p class="text-secondary small mt-2 mb-0">Customer sees replies in the POS app when they are online.</p>
        </div></div>
    </div>
</div>
@endsection
