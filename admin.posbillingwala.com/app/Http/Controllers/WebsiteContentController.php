<?php

namespace App\Http\Controllers;

use App\Models\WebsiteClient;
use App\Models\WebsiteContactMessage;
use App\Models\WebsitePage;
use App\Models\WebsiteTestimonial;
use App\Models\WebsiteDealer;
use App\Models\WebsitePricingPlan;
use App\Models\WebsiteProduct;
use App\Services\AdminTables;
use App\Services\WebsiteMedia;
use Auth;
use Illuminate\Http\Request;

class WebsiteContentController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
        AdminTables::ensureWebsite();
    }

    private function adminOnly(): void
    {
        if (Auth::user()->role_id != 1) {
            abort(403);
        }
    }

    private function pageOrCreate(string $slug, string $title): WebsitePage
    {
        return WebsitePage::firstOrCreate(
            ['slug' => $slug],
            ['title' => $title, 'body_html' => '', 'updated_at' => now()]
        );
    }

    public function hub()
    {
        $this->adminOnly();

        return view('website.hub', [
            'clientCount' => WebsiteClient::count(),
            'testimonialCount' => WebsiteTestimonial::count(),
            'contactCount' => WebsiteContactMessage::count(),
            'newContactCount' => WebsiteContactMessage::where('status', 'New')->count(),
            'dealerCount' => WebsiteDealer::count(),
            'pricingCount' => WebsitePricingPlan::count(),
            'productCount' => WebsiteProduct::count(),
        ]);
    }

    public function privacy()
    {
        $this->adminOnly();
        $page = $this->pageOrCreate('privacy', 'Privacy Policy');

        return view('website.privacy', compact('page'));
    }

    public function updatePrivacy(Request $request)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'title' => 'required|string|max:255',
            'body_html' => 'required|string',
        ]);

        $page = $this->pageOrCreate('privacy', 'Privacy Policy');
        $page->title = $validated['title'];
        $page->body_html = $validated['body_html'];
        $page->updated_at = now();
        $page->save();

        return redirect('website/privacy')->with('success', 'Privacy policy updated successfully');
    }

    public function about()
    {
        $this->adminOnly();
        $page = $this->pageOrCreate('about', 'About Us');

        return view('website.about', compact('page'));
    }

    public function updateAbout(Request $request)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'title' => 'required|string|max:255',
            'body_html' => 'required|string',
        ]);

        $page = $this->pageOrCreate('about', 'About Us');
        $page->title = $validated['title'];
        $page->body_html = $validated['body_html'];
        $page->updated_at = now();
        $page->save();

        return redirect('website/about')->with('success', 'About Us page updated successfully');
    }

    public function clients()
    {
        $this->adminOnly();
        $clients = WebsiteClient::orderBy('sort_order')->orderByDesc('id')->get();

        return view('website.clients.all', compact('clients'));
    }

    public function clientAdd()
    {
        $this->adminOnly();

        return view('website.clients.add');
    }

    public function clientStore(Request $request)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'business_name' => 'required|string|max:255',
            'subtitle' => 'nullable|string|max:255',
            'city' => 'nullable|string|max:120',
            'business_category' => 'nullable|string|max:120',
            'description' => 'nullable|string',
            'cta_url' => 'nullable|url|max:500',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'logo' => 'nullable|image|mimes:jpeg,jpg,png,webp,svg|max:2048',
            'photo' => 'nullable|image|mimes:jpeg,jpg,png,webp|max:4096',
            'is_published' => 'nullable|boolean',
        ]);

        $client = new WebsiteClient();
        $client->business_name = $validated['business_name'];
        $client->subtitle = $validated['subtitle'] ?? '';
        $client->city = $validated['city'] ?? '';
        $client->business_category = $validated['business_category'] ?? '';
        $client->description = $validated['description'] ?? '';
        $client->cta_url = $validated['cta_url'] ?? '';
        $client->sort_order = (int) ($validated['sort_order'] ?? 0);
        $client->is_published = $request->boolean('is_published', true);
        $client->created_at = now();
        $client->updated_at = now();
        $client->save();

        if ($request->hasFile('logo')) {
            $client->logo_path = WebsiteMedia::save($request->file('logo'), 'clients', 'client-' . $client->id . '-logo');
            $client->save();
        }
        if ($request->hasFile('photo')) {
            $client->photo_path = WebsiteMedia::save($request->file('photo'), 'clients', 'client-' . $client->id . '-photo');
            $client->save();
        }

        return redirect('website/clients')->with('success', 'Client added successfully');
    }

    public function clientEdit($id)
    {
        $this->adminOnly();
        $client = WebsiteClient::findOrFail($id);

        return view('website.clients.edit', compact('client'));
    }

    public function clientUpdate(Request $request, $id)
    {
        $this->adminOnly();
        $client = WebsiteClient::findOrFail($id);

        $validated = $request->validate([
            'business_name' => 'required|string|max:255',
            'subtitle' => 'nullable|string|max:255',
            'city' => 'nullable|string|max:120',
            'business_category' => 'nullable|string|max:120',
            'description' => 'nullable|string',
            'cta_url' => 'nullable|url|max:500',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'logo' => 'nullable|image|mimes:jpeg,jpg,png,webp,svg|max:2048',
            'photo' => 'nullable|image|mimes:jpeg,jpg,png,webp|max:4096',
            'is_published' => 'nullable|boolean',
        ]);

        $client->business_name = $validated['business_name'];
        $client->subtitle = $validated['subtitle'] ?? '';
        $client->city = $validated['city'] ?? '';
        $client->business_category = $validated['business_category'] ?? '';
        $client->description = $validated['description'] ?? '';
        $client->cta_url = $validated['cta_url'] ?? '';
        $client->sort_order = (int) ($validated['sort_order'] ?? 0);
        $client->is_published = $request->boolean('is_published', true);
        $client->updated_at = now();

        if ($request->hasFile('logo')) {
            WebsiteMedia::delete($client->logo_path);
            $client->logo_path = WebsiteMedia::save($request->file('logo'), 'clients', 'client-' . $client->id . '-logo');
        }
        if ($request->hasFile('photo')) {
            WebsiteMedia::delete($client->photo_path);
            $client->photo_path = WebsiteMedia::save($request->file('photo'), 'clients', 'client-' . $client->id . '-photo');
        }

        $client->save();

        return redirect('website/clients')->with('success', 'Client updated successfully');
    }

    public function clientToggle($id)
    {
        $this->adminOnly();
        $client = WebsiteClient::findOrFail($id);
        $client->is_published = !$client->is_published;
        $client->updated_at = now();
        $client->save();

        return redirect()->back()->with('success', $client->is_published ? 'Client published' : 'Client hidden from website');
    }

    public function clientDelete($id)
    {
        $this->adminOnly();
        $client = WebsiteClient::findOrFail($id);
        WebsiteMedia::delete($client->logo_path);
        WebsiteMedia::delete($client->photo_path);
        $client->delete();

        return redirect('website/clients')->with('success', 'Client deleted successfully');
    }

    public function testimonials()
    {
        $this->adminOnly();
        $testimonials = WebsiteTestimonial::orderBy('sort_order')->orderByDesc('id')->get();

        return view('website.testimonials.all', compact('testimonials'));
    }

    public function testimonialAdd()
    {
        $this->adminOnly();

        return view('website.testimonials.add');
    }

    public function testimonialStore(Request $request)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'author_name' => 'required|string|max:255',
            'business_name' => 'nullable|string|max:255',
            'quote' => 'required|string',
            'rating' => 'nullable|integer|min:1|max:5',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'photo' => 'nullable|image|mimes:jpeg,jpg,png,webp|max:2048',
            'is_published' => 'nullable|boolean',
        ]);

        $item = new WebsiteTestimonial();
        $item->author_name = $validated['author_name'];
        $item->business_name = $validated['business_name'] ?? '';
        $item->quote = $validated['quote'];
        $item->rating = (int) ($validated['rating'] ?? 5);
        $item->sort_order = (int) ($validated['sort_order'] ?? 0);
        $item->is_published = $request->boolean('is_published', true);
        $item->created_at = now();
        $item->updated_at = now();
        $item->save();

        if ($request->hasFile('photo')) {
            $item->photo_path = WebsiteMedia::save($request->file('photo'), 'testimonials', 'testimonial-' . $item->id);
            $item->save();
        }

        return redirect('website/testimonials')->with('success', 'Testimonial added successfully');
    }

    public function testimonialEdit($id)
    {
        $this->adminOnly();
        $testimonial = WebsiteTestimonial::findOrFail($id);

        return view('website.testimonials.edit', compact('testimonial'));
    }

    public function testimonialUpdate(Request $request, $id)
    {
        $this->adminOnly();
        $item = WebsiteTestimonial::findOrFail($id);

        $validated = $request->validate([
            'author_name' => 'required|string|max:255',
            'business_name' => 'nullable|string|max:255',
            'quote' => 'required|string',
            'rating' => 'nullable|integer|min:1|max:5',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'photo' => 'nullable|image|mimes:jpeg,jpg,png,webp|max:2048',
            'is_published' => 'nullable|boolean',
        ]);

        $item->author_name = $validated['author_name'];
        $item->business_name = $validated['business_name'] ?? '';
        $item->quote = $validated['quote'];
        $item->rating = (int) ($validated['rating'] ?? 5);
        $item->sort_order = (int) ($validated['sort_order'] ?? 0);
        $item->is_published = $request->boolean('is_published', true);
        $item->updated_at = now();

        if ($request->hasFile('photo')) {
            WebsiteMedia::delete($item->photo_path);
            $item->photo_path = WebsiteMedia::save($request->file('photo'), 'testimonials', 'testimonial-' . $item->id);
        }

        $item->save();

        return redirect('website/testimonials')->with('success', 'Testimonial updated successfully');
    }

    public function testimonialToggle($id)
    {
        $this->adminOnly();
        $item = WebsiteTestimonial::findOrFail($id);
        $item->is_published = !$item->is_published;
        $item->updated_at = now();
        $item->save();

        return redirect()->back()->with('success', $item->is_published ? 'Testimonial published' : 'Testimonial hidden from website');
    }

    public function testimonialDelete($id)
    {
        $this->adminOnly();
        $item = WebsiteTestimonial::findOrFail($id);
        WebsiteMedia::delete($item->photo_path);
        $item->delete();

        return redirect('website/testimonials')->with('success', 'Testimonial deleted successfully');
    }

    public function contacts()
    {
        $this->adminOnly();
        $contacts = WebsiteContactMessage::orderByDesc('id')->limit(200)->get();

        return view('website.contacts.all', compact('contacts'));
    }

    public function contactShow($id)
    {
        $this->adminOnly();
        $contact = WebsiteContactMessage::findOrFail($id);
        if ($contact->status === 'New') {
            $contact->status = 'Read';
            $contact->updated_at = now();
            $contact->save();
        }

        return view('website.contacts.show', compact('contact'));
    }

    public function contactUpdateStatus(Request $request, $id)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'status' => 'required|in:New,Read,Replied,Closed',
        ]);
        $contact = WebsiteContactMessage::findOrFail($id);
        $contact->status = $validated['status'];
        $contact->updated_at = now();
        $contact->save();

        return redirect('website/contacts/' . $id)->with('success', 'Status updated');
    }
}
