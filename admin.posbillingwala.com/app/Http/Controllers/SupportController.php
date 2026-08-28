<?php

namespace App\Http\Controllers;

use App\Models\SupportMessage;
use App\Models\SupportTicket;
use App\Services\AdminTables;
use Auth;
use Illuminate\Http\Request;

class SupportController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    private function adminOnly()
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
        AdminTables::ensure();
    }

    public function hub()
    {
        $this->adminOnly();
        $open = SupportTicket::whereRaw("LOWER(status)='open'")->count();
        $total = SupportTicket::count();
        return view('support.hub', compact('open', 'total'));
    }

    public function tickets(Request $request)
    {
        $this->adminOnly();
        $status = strtolower(trim((string) $request->get('status', 'all')));
        $query = SupportTicket::query()->orderByDesc('id');
        if ($status !== '' && $status !== 'all') {
            $query->whereRaw('LOWER(status)=?', [$status]);
        }
        $tickets = $query->limit(200)->get();
        return view('support.tickets', compact('tickets', 'status'));
    }

    public function create()
    {
        $this->adminOnly();
        return view('support.create');
    }

    public function store(Request $request)
    {
        $this->adminOnly();
        $request->validate([
            'subject' => 'required|string|max:255',
            'description' => 'nullable|string',
            'app_name' => 'nullable|string|max:40',
            'category' => 'nullable|string|max:80',
        ]);
        $ticketNo = 'TKT-' . date('Ymd') . '-' . str_pad((string) random_int(1, 9999), 4, '0', STR_PAD_LEFT);
        $ticket = SupportTicket::create([
            'ticket_no' => $ticketNo,
            'app_name' => $request->get('app_name', 'POS App'),
            'category' => $request->get('category', 'General'),
            'subject' => $request->subject,
            'description' => $request->description,
            'status' => 'Open',
            'created_at' => now(),
            'updated_at' => now(),
        ]);
        if ($request->description) {
            SupportMessage::create([
                'ticket_id' => $ticket->id,
                'sender' => 'You',
                'message' => $request->description,
                'created_at' => now(),
            ]);
        }
        return redirect('support/tickets/' . $ticket->id)->with('success', 'Ticket created');
    }

    public function show($id)
    {
        $this->adminOnly();
        $ticket = SupportTicket::with('messages')->findOrFail($id);
        return view('support.show', compact('ticket'));
    }

    public function reply(Request $request, $id)
    {
        $this->adminOnly();
        $request->validate(['message' => 'required|string']);
        $ticket = SupportTicket::findOrFail($id);
        SupportMessage::create([
            'ticket_id' => $ticket->id,
            'sender' => Auth::user()->name ?: 'Admin',
            'message' => $request->message,
            'created_at' => now(),
        ]);
        $ticket->status = 'Open';
        $ticket->updated_at = now();
        $ticket->save();
        return redirect()->back()->with('success', 'Reply sent');
    }

    public function updateStatus(Request $request, $id)
    {
        $this->adminOnly();
        $request->validate(['status' => 'required|in:Open,Closed,Resolved']);
        $ticket = SupportTicket::findOrFail($id);
        $ticket->status = $request->status;
        $ticket->updated_at = now();
        $ticket->save();
        return redirect()->back()->with('success', 'Ticket marked as ' . $request->status);
    }

    public function faq()
    {
        $this->adminOnly();
        return view('support.faq');
    }
}
