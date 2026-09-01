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

    public function seedDefaults(): void
    {
        $this->seedSettings();
        $this->seedProducts();
        $this->seedPricing();
        $this->seedDealers();
        $this->seedClients();
        $this->seedTestimonials();
        $this->seedLegalPages();
    }

    private function seedSettings(): void
    {
        $defaults = [
            'legal_company_name' => 'CANA Tech Solutions Private Limited',
            'brand_tagline' => 'Smart Billing. Trusted Support. Better Business.',
            'gstin' => '',
            'office_address' => 'S No 47, Pune - Satara Rd, opp. City Pride Multiplex, near Bhapkar petrol pump, Adinath Society, Taware Colony, Bibwewadi, Pune, Maharashtra 411009',
            'support_phone' => '8983149299',
            'support_whatsapp' => '8983149299',
            'support_email' => 'support@posbillingwala.com',
            'sales_email' => 'hello@posbillingwala.com',
            'business_hours' => 'Mon–Sat, 10:00 AM – 7:00 PM IST',
            'play_store_url' => 'https://play.google.com/store/apps/details?id=com.pos_billingwala',
            'apk_download_url' => '',
            'app_latest_version' => '',
        ];

        foreach ($defaults as $key => $value) {
            if (!WebsiteSetting::find($key)) {
                WebsiteSetting::setValue($key, $value);
            }
        }
    }

    private function seedProducts(): void
    {
        if (WebsiteProduct::count() > 0) {
            return;
        }

        $items = [
            // Software
            ['POS Billing Software (Android)', 'software', 'Complete Android POS app with GST billing, inventory, offline mode, Bluetooth printing and sales reports. Ideal for restaurants, retail and grocery.', '📱', 10],
            ['Mobile & Tablet Billing App', 'software', 'Counter-ready billing on Android phone or tablet. Fast checkout, product search, customer records and daily sales summary.', '📲', 20],
            ['Multi-Device Licence Pack', 'software', 'Run POS Billingwala on multiple devices for growing businesses. Includes sync, reports and dealer support.', '▣', 30],

            // Hardware
            ['58mm Bluetooth Thermal Printer', 'hardware', 'Compact 58mm Bluetooth printer for mobile billing. Fast receipt printing, easy pairing and ideal for small counters.', '🖨️', 40],
            ['80mm Bluetooth Thermal Printer', 'hardware', 'Standard 80mm thermal printer for restaurant and retail counters. Reliable daily printing with POS Billingwala.', '🖨️', 50],
            ['USB Thermal Printer', 'hardware', 'Wired USB thermal printer for fixed billing counters. Stable connection and high-speed receipt printing.', '🖨️', 55],
            ['POS Billing Machine', 'hardware', 'All-in-one counter POS terminal compatible with POS Billingwala workflows. Built for busy billing counters.', '🖥️', 60],
            ['Cash Drawer', 'hardware', 'Electronic cash drawer for retail and restaurant counters. Opens with printer signal for secure cash handling.', '💰', 70],
            ['Barcode Scanner', 'hardware', 'USB / wireless barcode scanner for fast product billing. Speeds up retail checkout and stock entry.', '▤', 75],

            // Consumables
            ['57mm Billing Rolls', 'consumables', 'Premium thermal billing rolls for 57mm / 58mm printers. Smooth printing, fade-resistant and long shelf life.', '🧾', 80],
            ['80mm Billing Rolls', 'consumables', 'Standard 80mm thermal rolls for restaurant and retail printers. Available in economical multi-roll packs.', '🧾', 90],
            ['Barcode Labels & Stickers', 'consumables', 'Self-adhesive barcode labels for inventory tagging, retail products and stock management.', '🏷️', 100],
            ['Printer Cleaning Kit', 'consumables', 'Head cleaning kit for thermal printers. Keeps print quality sharp and extends printer life.', '🧹', 110],

            // Accessories
            ['Printer Power Adapter', 'accessories', 'Compatible power adapter for supported thermal printers. Reliable power supply for daily counter use.', '🔌', 120],
            ['USB & Bluetooth Cable', 'accessories', 'High-quality cables for printer and device connectivity. Durable build for busy billing environments.', '🔗', 130],
            ['Tablet Stand & Mount', 'accessories', 'Adjustable stand for Android tablet billing. Keeps screen visible and secure at the counter.', '📐', 140],
            ['Counter Accessories Kit', 'accessories', 'Essential billing counter add-ons — stands, cables and small accessories for a complete setup.', '🧰', 150],
        ];

        foreach ($items as [$name, $category, $description, $icon, $order]) {
            WebsiteProduct::create([
                'name' => $name,
                'category' => $category,
                'description' => $description,
                'icon' => $icon,
                'sort_order' => $order,
                'is_published' => 1,
                'created_at' => now(),
                'updated_at' => now(),
            ]);
        }
    }

    private function seedPricing(): void
    {
        if (WebsitePricingPlan::count() > 0) {
            return;
        }

        $common = "Offline billing mode\nCustomer management\nPriority phone support\nGST billing & invoices\nSingle device licence\nProduct & stock management\nBluetooth thermal printing\nSales reports\nExtend existing licence\nKeep all your data\nGST billing & reports\nBluetooth printing\nPhone support";
        $sixMonths = "Standard 6-month plan\n6 months validity\n" . $common;
        $oneYear = "Everything in 1-year plan\n1 year validity — best value\n" . $common;

        $plans = [
            [
                'subscription', '6 Months', 3500, 'GST included',
                $sixMonths,
                10, 0,
            ],
            [
                'subscription', '1 Year', 6000, 'GST included',
                $oneYear,
                20, 1,
            ],
            [
                'renewal', '6 Months', 3000, 'GST included',
                $sixMonths,
                30, 0,
            ],
            [
                'renewal', '1 Year', 5500, 'GST included',
                $oneYear,
                40, 0,
            ],
        ];

        foreach ($plans as [$type, $validity, $price, $gst, $desc, $order, $featured]) {
            WebsitePricingPlan::create([
                'plan_type' => $type,
                'validity_label' => $validity,
                'price' => $price,
                'gst_note' => $gst,
                'description' => $desc,
                'sort_order' => $order,
                'is_published' => 1,
                'is_featured' => $featured ? 1 : 0,
                'created_at' => now(),
                'updated_at' => now(),
            ]);
        }
    }

    private function seedDealers(): void
    {
        $dealers = [
            [
                'area' => 'Pune',
                'dealer_name' => 'Pune Office',
                'contact_person' => 'Santosh Dixit',
                'role_title' => 'Sales & Marketing Manager',
                'mobile' => '8983149299',
                'whatsapp' => '8983149299',
                'address' => 'S No 47, Pune - Satara Rd, opp. City Pride Multiplex, near Bhapkar petrol pump, Adinath Society, Taware Colony, Bibwewadi, Pune, Maharashtra 411009',
                'map_url' => '',
                'dealer_type' => 'head_office',
                'sort_order' => 10,
            ],
            [
                'area' => 'Pandharpur',
                'dealer_name' => 'Authorized POS Billingwala Dealer',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Pandharpur, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 20,
            ],
            [
                'area' => 'Satara',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Satara',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Satara, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 30,
            ],
            [
                'area' => 'Solapur',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Solapur',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Solapur, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 40,
            ],
            [
                'area' => 'Kolhapur',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Kolhapur',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Kolhapur, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 50,
            ],
            [
                'area' => 'Sangli',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Sangli',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Sangli, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 60,
            ],
            [
                'area' => 'Ahmednagar',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Ahmednagar',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Ahmednagar, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 70,
            ],
            [
                'area' => 'Nashik',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Nashik',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Nashik, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 80,
            ],
            [
                'area' => 'Mumbai',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Mumbai',
                'contact_person' => '',
                'role_title' => 'Authorized Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Mumbai, Maharashtra, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 90,
            ],
            [
                'area' => 'Karnataka',
                'dealer_name' => 'Authorized POS Billingwala Dealer — Karnataka',
                'contact_person' => '',
                'role_title' => 'Regional Dealer',
                'mobile' => '',
                'whatsapp' => '',
                'address' => 'Karnataka, India',
                'map_url' => '',
                'dealer_type' => 'authorized_dealer',
                'sort_order' => 100,
            ],
        ];

        foreach ($dealers as $row) {
            if (WebsiteDealer::where('area', $row['area'])->exists()) {
                continue;
            }
            WebsiteDealer::create(array_merge($row, [
                'is_published' => 1,
                'created_at' => now(),
                'updated_at' => now(),
            ]));
        }
    }

    private function seedClients(): void
    {
        if (WebsiteClient::count() > 0) {
            return;
        }

        $clients = [
            ['business_name' => 'Hotel Shree', 'subtitle' => 'Owner · Pune', 'city' => 'Pune', 'business_category' => 'Restaurant', 'description' => 'Daily billing, KOT printing, and table-wise sales with POS Billingwala.', 'sort_order' => 10],
            ['business_name' => 'Balaji Kirana Store', 'subtitle' => 'Retail · Pandharpur', 'city' => 'Pandharpur', 'business_category' => 'Retail', 'description' => 'Fast barcode billing and thermal receipt printing for daily customers.', 'sort_order' => 20],
            ['business_name' => 'Mess Prasad', 'subtitle' => 'Mess · Solapur', 'city' => 'Solapur', 'business_category' => 'Mess', 'description' => 'Mess token billing and member payment tracking.', 'sort_order' => 30],
            ['business_name' => 'Shree Grocery Mart', 'subtitle' => 'Grocery · Satara', 'city' => 'Satara', 'business_category' => 'Grocery', 'description' => 'Quick item billing, stock tracking and GST invoices for daily grocery sales.', 'sort_order' => 40],
            ['business_name' => 'Fashion Point', 'subtitle' => 'Clothing · Kolhapur', 'city' => 'Kolhapur', 'business_category' => 'Clothing', 'description' => 'Retail billing with customer records and size-wise product management.', 'sort_order' => 50],
            ['business_name' => 'Cafe Delight', 'subtitle' => 'Café · Mumbai', 'city' => 'Mumbai', 'business_category' => 'Restaurant', 'description' => 'Fast takeaway and counter billing with Bluetooth receipt printing.', 'sort_order' => 60],
        ];

        $now = now();
        foreach ($clients as $client) {
            WebsiteClient::create(array_merge($client, [
                'logo_path' => '',
                'photo_path' => '',
                'cta_url' => '',
                'is_published' => 1,
                'created_at' => $now,
                'updated_at' => $now,
            ]));
        }
    }

    private function seedTestimonials(): void
    {
        if (WebsiteTestimonial::count() > 0) {
            return;
        }

        $items = [
            ['author_name' => 'Rajesh Patil', 'business_name' => 'Hotel Shree, Pune', 'quote' => 'Offline billing never stops even when internet goes down. Local dealer support is excellent.', 'rating' => 5, 'sort_order' => 10],
            ['author_name' => 'Suresh Kulkarni', 'business_name' => 'Balaji Kirana Store, Pandharpur', 'quote' => 'Billing is fast and printing works smoothly. Staff learned the app in one day.', 'rating' => 5, 'sort_order' => 20],
            ['author_name' => 'Priya Deshmukh', 'business_name' => 'Mess Prasad, Solapur', 'quote' => 'Perfect for our mess billing. Renewal and support from the dealer is very helpful.', 'rating' => 5, 'sort_order' => 30],
        ];

        $now = now();
        foreach ($items as $item) {
            WebsiteTestimonial::create(array_merge($item, [
                'photo_path' => '',
                'is_published' => 1,
                'created_at' => $now,
                'updated_at' => $now,
            ]));
        }
    }

    private function seedLegalPages(): void
    {
        if (!WebsitePage::where('slug', 'terms')->exists()) {
            WebsitePage::create([
                'slug' => 'terms',
                'title' => 'Terms & Conditions',
                'body_html' => $this->defaultTermsHtml(),
                'updated_at' => now(),
            ]);
        }

        if (!WebsitePage::where('slug', 'refund-renewal')->exists()) {
            WebsitePage::create([
                'slug' => 'refund-renewal',
                'title' => 'Refund & Renewal Policy',
                'body_html' => $this->defaultRefundHtml(),
                'updated_at' => now(),
            ]);
        }

        if (!WebsitePage::where('slug', 'support')->exists()) {
            WebsitePage::create([
                'slug' => 'support',
                'title' => 'Customer Support',
                'body_html' => $this->defaultSupportHtml(),
                'updated_at' => now(),
            ]);
        }

        if (!WebsitePage::where('slug', 'company')->exists()) {
            WebsitePage::create([
                'slug' => 'company',
                'title' => 'Company Model',
                'body_html' => $this->defaultCompanyHtml(),
                'updated_at' => now(),
            ]);
        }
    }

    private function defaultCompanyHtml(): string
    {
        return '<p>POS Billingwala operates as a <strong>dealer-network based SaaS + hardware + support business</strong> — not only a software download.</p>'
            . '<h2>Organizational structure</h2>'
            . '<pre style="white-space:pre-wrap;font-family:inherit;line-height:1.6;background:#f8fafc;padding:1rem;border-radius:12px;">POS Billingwala → Company / Head Office\n  → Regional Dealers\n    → Area Dealers\n      → Sales &amp; Marketing Team\n      → Customer Support Team\n      → Installation / Technical Team\n        → Customers</pre>'
            . '<h2>Head Office</h2><p>Product development, brand, pricing policy, licence system, legal compliance, and central support escalation.</p>'
            . '<h2>Regional &amp; Area Dealers</h2><p>Local sales, installation, first-line support, and renewal collection in your city.</p>'
            . '<h2>Support &amp; Technical Teams</h2><p>Printer setup, menu creation, remote help, training, and renewal reminders.</p>'
            . '<h2>Revenue model</h2><ul>'
            . '<li><strong>Software</strong> — subscription &amp; renewal (6 months / 1 year)</li>'
            . '<li><strong>Hardware</strong> — POS machine, Bluetooth / thermal printer</li>'
            . '<li><strong>Consumables</strong> — 57mm &amp; 80mm billing rolls, labels</li>'
            . '<li><strong>Services</strong> — installation, training, remote support</li></ul>'
            . '<p><a href="dealers.html">Find your local dealer →</a></p>';
    }

    private function defaultTermsHtml(): string
    {
        return '<p>These Terms &amp; Conditions govern your use of POS Billingwala software, services, and related products sold by CANA Tech Solutions Private Limited.</p>'
            . '<h2>License use</h2>'
            . '<p>Each subscription or renewal grants use of POS Billingwala on licensed device(s) for the purchased validity period. Licence keys are non-transferable unless approved in writing.</p>'
            . '<h2>Software updates</h2>'
            . '<p>We may release updates, fixes, and feature improvements during your active licence period.</p>'
            . '<h2>Support</h2>'
            . '<p>Installation, training, and technical support are provided as described on our Support page and by your authorized dealer.</p>'
            . '<h2>Limitation of liability</h2>'
            . '<p>POS Billingwala is provided on an &ldquo;as available&rdquo; basis. We are not liable for indirect business losses beyond applicable law.</p>'
            . '<h2>Contact</h2>'
            . '<p>Questions about these terms: <a href="mailto:support@posbillingwala.com">support@posbillingwala.com</a>.</p>';
    }

    private function defaultRefundHtml(): string
    {
        return '<h2>Renewal policy</h2>'
            . '<ul>'
            . '<li>Licences are valid for the purchased period (6 months or 1 year).</li>'
            . '<li>Renew before expiry to continue uninterrupted access.</li>'
            . '<li>Contact your local dealer or our support team for renewal.</li>'
            . '<li>Same licence key may be renewed for eligible accounts.</li>'
            . '</ul>'
            . '<h2>Refund policy</h2>'
            . '<ul>'
            . '<li><strong>Software:</strong> Subscription fees are generally non-refundable after licence activation, except where required by law or explicitly agreed in writing.</li>'
            . '<li><strong>Hardware:</strong> Defective hardware may be replaced within the warranty period stated on your invoice.</li>'
            . '<li><strong>Disputes:</strong> Email <a href="mailto:support@posbillingwala.com">support@posbillingwala.com</a> with invoice and licence details.</li>'
            . '</ul>';
    }

    private function defaultSupportHtml(): string
    {
        return '<p><strong>We Don&rsquo;t Just Sell Software &mdash; We Support Your Business.</strong></p>'
            . '<p>Our team and authorized dealers help you from installation through daily operations and renewal.</p>'
            . '<h2>What we help with</h2>'
            . '<ul>'
            . '<li>Installation &amp; setup</li>'
            . '<li>Product / menu creation support</li>'
            . '<li>Data sync &amp; software updates</li>'
            . '<li>Remote support</li>'
            . '<li>Printer setup (Bluetooth / thermal)</li>'
            . '<li>Billing software training</li>'
            . '<li>Renewal support</li>'
            . '<li>Dealer-backed local support</li>'
            . '<li>WhatsApp / phone support</li>'
            . '</ul>';
    }

    public function companySettings()
    {
        $this->adminOnly();
        $this->seedDefaults();

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
        $this->seedDefaults();
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
        $this->seedDefaults();
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
        $this->seedDefaults();
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
        $this->seedDefaults();
        $page = WebsitePage::where('slug', 'terms')->firstOrFail();

        return view('website.legal.edit', ['page' => $page, 'label' => 'Terms & Conditions', 'backUrl' => 'website/terms']);
    }

    public function updateTerms(Request $request)
    {
        return $this->updateLegalPage($request, 'terms', 'website/terms');
    }

    public function refund()
    {
        $this->adminOnly();
        $this->seedDefaults();
        $page = WebsitePage::where('slug', 'refund-renewal')->firstOrFail();

        return view('website.legal.edit', ['page' => $page, 'label' => 'Refund & Renewal Policy', 'backUrl' => 'website/refund']);
    }

    public function updateRefund(Request $request)
    {
        return $this->updateLegalPage($request, 'refund-renewal', 'website/refund');
    }

    public function supportPage()
    {
        $this->adminOnly();
        $this->seedDefaults();
        $page = WebsitePage::where('slug', 'support')->firstOrFail();

        return view('website.legal.edit', ['page' => $page, 'label' => 'Support Page Content', 'backUrl' => 'website/support']);
    }

    public function updateSupportPage(Request $request)
    {
        return $this->updateLegalPage($request, 'support', 'website/support');
    }

    public function companyPage()
    {
        $this->adminOnly();
        $this->seedDefaults();
        $page = WebsitePage::where('slug', 'company')->firstOrFail();

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
