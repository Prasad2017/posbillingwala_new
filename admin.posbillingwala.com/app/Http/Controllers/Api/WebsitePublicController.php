<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\WebsiteClient;
use App\Models\WebsiteContactMessage;
use App\Models\WebsitePage;
use App\Models\WebsiteTestimonial;
use App\Models\WebsiteDealer;
use App\Models\WebsitePricingPlan;
use App\Models\WebsiteProduct;
use App\Models\WebsiteSetting;
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
                    'city' => $client->city,
                    'business_category' => $client->business_category,
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

    public function dealers()
    {
        $items = WebsiteDealer::where('is_published', 1)
            ->orderBy('sort_order')
            ->orderBy('area')
            ->get()
            ->map(function (WebsiteDealer $dealer) {
                return [
                    'id' => $dealer->id,
                    'area' => $dealer->area,
                    'dealer_name' => $dealer->dealer_name,
                    'contact_person' => $dealer->contact_person,
                    'role_title' => $dealer->role_title,
                    'mobile' => $dealer->mobile,
                    'whatsapp' => $dealer->whatsapp,
                    'address' => $dealer->address,
                    'map_url' => $dealer->map_url ?: null,
                    'dealer_type' => $dealer->dealer_type,
                ];
            });

        return response()->json(['success' => true, 'dealers' => $items]);
    }

    public function pricing()
    {
        $items = WebsitePricingPlan::where('is_published', 1)
            ->orderBy('sort_order')
            ->orderBy('id')
            ->get()
            ->map(function (WebsitePricingPlan $plan) {
                return [
                    'id' => $plan->id,
                    'plan_type' => $plan->plan_type,
                    'validity_label' => $plan->validity_label,
                    'price' => (float) $plan->price,
                    'gst_note' => $plan->gst_note,
                    'description' => $plan->description,
                ];
            });

        return response()->json(['success' => true, 'plans' => $items]);
    }

    public function products()
    {
        $items = WebsiteProduct::where('is_published', 1)
            ->orderBy('sort_order')
            ->orderBy('name')
            ->get()
            ->map(function (WebsiteProduct $product) {
                return [
                    'id' => $product->id,
                    'name' => $product->name,
                    'category' => $product->category,
                    'description' => $product->description,
                    'icon' => $product->icon,
                ];
            });

        return response()->json(['success' => true, 'products' => $items]);
    }

    public function settings()
    {
        return response()->json([
            'success' => true,
            'settings' => WebsiteSetting::allMap(),
        ]);
    }
}
