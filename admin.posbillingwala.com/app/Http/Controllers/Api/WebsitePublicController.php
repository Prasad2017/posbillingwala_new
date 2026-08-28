<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\WebsiteClient;
use App\Models\WebsiteContactMessage;
use App\Models\WebsitePage;
use App\Models\WebsiteTestimonial;
use App\Services\AdminTables;
use App\Services\WebsiteMedia;
use Illuminate\Http\Request;

class WebsitePublicController extends Controller
{
    public function __construct()
    {
        AdminTables::ensureWebsite();
    }

    public function clients()
    {
        $items = WebsiteClient::where('is_published', 1)
            ->orderBy('sort_order')
            ->orderByDesc('id')
            ->get()
            ->map(function (WebsiteClient $client) {
                return [
                    'id' => $client->id,
                    'business_name' => $client->business_name,
                    'subtitle' => $client->subtitle,
                    'description' => $client->description,
                    'logo_url' => WebsiteMedia::url($client->logo_path),
                    'photo_url' => WebsiteMedia::url($client->photo_path),
                    'cta_url' => $client->cta_url ?: null,
                ];
            });

        return response()->json([
            'success' => true,
            'clients' => $items,
        ]);
    }

    public function testimonials()
    {
        $items = WebsiteTestimonial::where('is_published', 1)
            ->orderBy('sort_order')
            ->orderByDesc('id')
            ->get()
            ->map(function (WebsiteTestimonial $item) {
                return [
                    'id' => $item->id,
                    'author_name' => $item->author_name,
                    'business_name' => $item->business_name,
                    'quote' => $item->quote,
                    'rating' => (int) $item->rating,
                    'photo_url' => WebsiteMedia::url($item->photo_path),
                ];
            });

        return response()->json([
            'success' => true,
            'testimonials' => $items,
        ]);
    }

    public function page(string $slug)
    {
        $page = WebsitePage::where('slug', $slug)->first();
        if (!$page) {
            return response()->json(['success' => false, 'message' => 'Page not found'], 404);
        }

        return response()->json([
            'success' => true,
            'page' => [
                'slug' => $page->slug,
                'title' => $page->title,
                'body_html' => $page->body_html,
                'updated_at' => optional($page->updated_at)->toIso8601String(),
            ],
        ]);
    }

    public function submitContact(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'email' => 'required|email|max:255',
            'subject' => 'nullable|string|max:255',
            'message' => 'required|string|max:5000',
        ]);

        WebsiteContactMessage::create([
            'name' => $validated['name'],
            'email' => $validated['email'],
            'subject' => $validated['subject'] ?? '',
            'message' => $validated['message'],
            'status' => 'New',
            'source_ip' => (string) $request->ip(),
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Thank you — we received your message and will reply soon.',
        ]);
    }
}
