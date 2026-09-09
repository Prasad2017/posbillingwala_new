<?php

namespace App\Http\Controllers;

use App\Models\WebsiteClient;
use App\Models\WebsiteDealer;
use App\Models\WebsitePage;
use App\Models\WebsitePricingPlan;
use App\Models\WebsiteProduct;
use App\Models\WebsiteSetting;
use App\Models\WebsiteTestimonial;
use App\Services\AdminTables;
use Auth;
use Illuminate\Http\Request;

class WebsiteCatalogController extends Controller
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

    public function companySettings()
    {
        $this->adminOnly();

        return view('website.settings.company', [
            'settings' => WebsiteSetting::allMap(),
        ]);
    }

    public function updateCompanySettings(Request $request)
    {
        $this->adminOnly();
        $keys = [
            'legal_company_name',
            'brand_tagline',
            'gstin',
            'office_address',
            'support_phone',
            'support_whatsapp',
            'support_email',
            'sales_email',
            'business_hours',
            'play_store_url',
            'apk_download_url',
            'app_latest_version',
        ];

        foreach ($keys as $key) {
            WebsiteSetting::setValue($key, (string) $request->input($key, ''));
        }

        return redirect('website/settings')->with('success', 'Company settings updated successfully');
    }

    public function dealers()
    {
        $this->adminOnly();
        $dealers = WebsiteDealer::orderBy('sort_order')->orderBy('area')->get();

        return view('website.dealers.all', compact('dealers'));
    }

    public function dealerAdd()
    {
        $this->adminOnly();

        return view('website.dealers.add');
    }

    public function dealerStore(Request $request)
    {
        $this->adminOnly();
        $validated = $this->validateDealer($request);

        WebsiteDealer::create(array_merge($validated, [
            'created_at' => now(),
            'updated_at' => now(),
        ]));

        return redirect('website/dealers')->with('success', 'Dealer added successfully');
    }

    public function dealerEdit($id)
    {
        $this->adminOnly();
        $dealer = WebsiteDealer::findOrFail($id);

        return view('website.dealers.edit', compact('dealer'));
    }

    public function dealerUpdate(Request $request, $id)
    {
        $this->adminOnly();
        $dealer = WebsiteDealer::findOrFail($id);
        $validated = $this->validateDealer($request);
        $dealer->fill($validated);
        $dealer->updated_at = now();
        $dealer->save();

        return redirect('website/dealers')->with('success', 'Dealer updated successfully');
    }

    public function dealerToggle($id)
    {
        $this->adminOnly();
        $dealer = WebsiteDealer::findOrFail($id);
        $dealer->is_published = !$dealer->is_published;
        $dealer->updated_at = now();
        $dealer->save();

        return redirect()->back()->with('success', $dealer->is_published ? 'Dealer published' : 'Dealer hidden from website');
    }

    public function dealerDelete($id)
    {
        $this->adminOnly();
        WebsiteDealer::findOrFail($id)->delete();

        return redirect('website/dealers')->with('success', 'Dealer deleted successfully');
    }

    private function validateDealer(Request $request): array
    {
        $validated = $request->validate([
            'area' => 'required|string|max:120',
            'dealer_name' => 'required|string|max:255',
            'contact_person' => 'nullable|string|max:255',
            'role_title' => 'nullable|string|max:255',
            'mobile' => 'nullable|string|max:32',
            'whatsapp' => 'nullable|string|max:32',
            'address' => 'nullable|string',
            'map_url' => 'nullable|url|max:500',
            'dealer_type' => 'required|in:head_office,authorized_dealer',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'is_published' => 'nullable|boolean',
        ]);

        $validated['sort_order'] = (int) ($validated['sort_order'] ?? 0);
        $validated['is_published'] = $request->boolean('is_published', true);

        return $validated;
    }

    public function pricing()
    {
        $this->adminOnly();
        $plans = WebsitePricingPlan::orderBy('sort_order')->orderBy('id')->get();

        return view('website.pricing.all', compact('plans'));
    }

    public function pricingAdd()
    {
        $this->adminOnly();

        return view('website.pricing.add');
    }

    public function pricingStore(Request $request)
    {
        $this->adminOnly();
        $validated = $this->validatePricing($request);

        $plan = WebsitePricingPlan::create(array_merge($validated, [
            'created_at' => now(),
            'updated_at' => now(),
        ]));
        $this->applyFeaturedPricingPlan($plan);

        return redirect('website/pricing')->with('success', 'Pricing plan added successfully');
    }

    public function pricingEdit($id)
    {
        $this->adminOnly();
        $plan = WebsitePricingPlan::findOrFail($id);

        return view('website.pricing.edit', compact('plan'));
    }

    public function pricingUpdate(Request $request, $id)
    {
        $this->adminOnly();
        $plan = WebsitePricingPlan::findOrFail($id);
        $validated = $this->validatePricing($request);
        $plan->fill($validated);
        $plan->updated_at = now();
        $plan->save();
        $this->applyFeaturedPricingPlan($plan);

        return redirect('website/pricing')->with('success', 'Pricing plan updated successfully');
    }

    public function pricingToggle($id)
    {
        $this->adminOnly();
        $plan = WebsitePricingPlan::findOrFail($id);
        $plan->is_published = !$plan->is_published;
        $plan->updated_at = now();
        $plan->save();

        return redirect()->back()->with('success', $plan->is_published ? 'Plan published' : 'Plan hidden from website');
    }

    public function pricingDelete($id)
    {
        $this->adminOnly();
        WebsitePricingPlan::findOrFail($id)->delete();

        return redirect('website/pricing')->with('success', 'Pricing plan deleted successfully');
    }

    private function validatePricing(Request $request): array
    {
        $validated = $request->validate([
            'plan_type' => 'required|in:subscription,renewal',
            'validity_label' => 'required|string|max:64',
            'price' => 'required|numeric|min:0',
            'gst_note' => 'required|string|max:120',
            'description' => 'nullable|string|max:1000',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'is_published' => 'nullable|boolean',
            'is_featured' => 'nullable|boolean',
        ]);

        $validated['sort_order'] = (int) ($validated['sort_order'] ?? 0);
        $validated['is_published'] = $request->boolean('is_published', true);
        $validated['is_featured'] = $request->boolean('is_featured', false);

        return $validated;
    }

    private function applyFeaturedPricingPlan(WebsitePricingPlan $plan): void
    {
        if (!$plan->is_featured) {
            return;
        }

        WebsitePricingPlan::where('id', '!=', $plan->id)->update([
            'is_featured' => 0,
            'updated_at' => now(),
        ]);
    }

    public function products()
    {
        $this->adminOnly();
        $products = WebsiteProduct::orderBy('sort_order')->orderBy('name')->get();

        return view('website.products.all', compact('products'));
    }

    public function productAdd()
    {
        $this->adminOnly();

        return view('website.products.add');
    }

    public function productStore(Request $request)
    {
        $this->adminOnly();
        $validated = $this->validateProduct($request);

        WebsiteProduct::create(array_merge($validated, [
            'created_at' => now(),
            'updated_at' => now(),
        ]));

        return redirect('website/products')->with('success', 'Product added successfully');
    }

    public function productEdit($id)
    {
        $this->adminOnly();
        $product = WebsiteProduct::findOrFail($id);

        return view('website.products.edit', compact('product'));
    }

    public function productUpdate(Request $request, $id)
    {
        $this->adminOnly();
        $product = WebsiteProduct::findOrFail($id);
        $validated = $this->validateProduct($request);
        $product->fill($validated);
        $product->updated_at = now();
        $product->save();

        return redirect('website/products')->with('success', 'Product updated successfully');
    }

    public function productToggle($id)
    {
        $this->adminOnly();
        $product = WebsiteProduct::findOrFail($id);
        $product->is_published = !$product->is_published;
        $product->updated_at = now();
        $product->save();

        return redirect()->back()->with('success', $product->is_published ? 'Product published' : 'Product hidden from website');
    }

    public function productDelete($id)
    {
        $this->adminOnly();
        WebsiteProduct::findOrFail($id)->delete();

        return redirect('website/products')->with('success', 'Product deleted successfully');
    }

    private function validateProduct(Request $request): array
    {
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'category' => 'required|in:software,hardware,consumables,accessories',
            'description' => 'nullable|string',
            'icon' => 'nullable|string|max:16',
            'sort_order' => 'nullable|integer|min:0|max:9999',
            'is_published' => 'nullable|boolean',
        ]);

        $validated['sort_order'] = (int) ($validated['sort_order'] ?? 0);
        $validated['is_published'] = $request->boolean('is_published', true);

        return $validated;
    }

    public function terms()
    {
        $this->adminOnly();
        $page = $this->pageOrCreate('terms', 'Terms & Conditions');

        return view('website.legal.edit', ['page' => $page, 'label' => 'Terms & Conditions', 'backUrl' => 'website/terms']);
    }

    public function updateTerms(Request $request)
    {
        return $this->updateLegalPage($request, 'terms', 'website/terms');
    }

    public function refund()
    {
        $this->adminOnly();
        $page = $this->pageOrCreate('refund-renewal', 'Refund & Renewal Policy');

        return view('website.legal.edit', ['page' => $page, 'label' => 'Refund & Renewal Policy', 'backUrl' => 'website/refund']);
    }

    public function updateRefund(Request $request)
    {
        return $this->updateLegalPage($request, 'refund-renewal', 'website/refund');
    }

    public function supportPage()
    {
        $this->adminOnly();
        $page = $this->pageOrCreate('support', 'Customer Support');

        return view('website.legal.edit', ['page' => $page, 'label' => 'Support Page Content', 'backUrl' => 'website/support']);
    }

    public function updateSupportPage(Request $request)
    {
        return $this->updateLegalPage($request, 'support', 'website/support');
    }

    public function companyPage()
    {
        $this->adminOnly();
        $page = $this->pageOrCreate('company', 'Company Model');

        return view('website.legal.edit', ['page' => $page, 'label' => 'Company Model Page', 'backUrl' => 'website/company']);
    }

    public function updateCompanyPage(Request $request)
    {
        return $this->updateLegalPage($request, 'company', 'website/company');
    }

    private function updateLegalPage(Request $request, string $slug, string $redirectPath)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'title' => 'required|string|max:255',
            'body_html' => 'required|string',
        ]);

        $page = WebsitePage::where('slug', $slug)->firstOrFail();
        $page->title = $validated['title'];
        $page->body_html = $validated['body_html'];
        $page->updated_at = now();
        $page->save();

        return redirect($redirectPath)->with('success', 'Page updated successfully');
    }
}
