<?php

namespace Database\Seeders;

use App\Models\WebsiteClient;
use App\Models\WebsiteDealer;
use App\Models\WebsitePage;
use App\Models\WebsitePricingPlan;
use App\Models\WebsiteProduct;
use App\Models\WebsiteSetting;
use App\Models\WebsiteTestimonial;
use App\Services\AdminTables;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class WebsiteMockDataSeeder extends Seeder
{
    public function run(bool $fresh = false): void
    {
        AdminTables::ensureWebsite();

        if ($fresh) {
            $this->truncateWebsiteTables();
        }

        $this->seedSettings();
        $this->seedProducts();
        $this->seedPricing();
        $this->seedDealers();
        $this->seedClients();
        $this->seedTestimonials();
        $this->seedPages();
    }

    private function truncateWebsiteTables(): void
    {
        Schema::disableForeignKeyConstraints();

        foreach ([
            'website_products',
            'website_pricing_plans',
            'website_dealers',
            'website_clients',
            'website_testimonials',
            'website_settings',
            'website_pages',
        ] as $table) {
            if (Schema::hasTable($table)) {
                DB::table($table)->truncate();
            }
        }

        Schema::enableForeignKeyConstraints();
    }

    private function seedSettings(): void
    {
        $defaults = [
            'legal_company_name' => 'CANA Tech Solutions Private Limited',
            'brand_tagline' => 'Smart Billing. Trusted Support. Better Business.',
            'gstin' => '27XXXXX1234X1ZX',
            'office_address' => 'S No 47, Pune - Satara Rd, opp. City Pride Multiplex, near Bhapkar petrol pump, Adinath Society, Taware Colony, Bibwewadi, Pune, Maharashtra 411009',
            'support_phone' => '8983149299',
            'support_whatsapp' => '8983149299',
            'support_email' => 'support@posbillingwala.com',
            'sales_email' => 'hello@posbillingwala.com',
            'business_hours' => 'Mon–Sat, 10:00 AM – 7:00 PM IST',
            'play_store_url' => 'https://play.google.com/store/apps/details?id=com.pos_billingwala',
            'apk_download_url' => '',
            'app_latest_version' => 'Latest',
        ];

        foreach ($defaults as $key => $value) {
            WebsiteSetting::setValue($key, $value);
        }
    }

    private function seedProducts(): void
    {
        if (WebsiteProduct::count() > 0) {
            return;
        }

        $items = [
            ['POS Billing Software (Android)', 'software', 'Complete Android POS app with GST billing, inventory, offline mode, Bluetooth printing and sales reports. Ideal for restaurants, retail and grocery.', '📱', 10],
            ['Mobile & Tablet Billing App', 'software', 'Counter-ready billing on Android phone or tablet. Fast checkout, product search, customer records and daily sales summary.', '📲', 20],
            ['Multi-Device Licence Pack', 'software', 'Run POS Billingwala on multiple devices for growing businesses. Includes sync, reports and dealer support.', '▣', 30],
            ['58mm Bluetooth Thermal Printer', 'hardware', 'Compact 58mm Bluetooth printer for mobile billing. Fast receipt printing, easy pairing and ideal for small counters.', '🖨️', 40],
            ['80mm Bluetooth Thermal Printer', 'hardware', 'Standard 80mm thermal printer for restaurant and retail counters. Reliable daily printing with POS Billingwala.', '🖨️', 50],
            ['USB Thermal Printer', 'hardware', 'Wired USB thermal printer for fixed billing counters. Stable connection and high-speed receipt printing.', '🖨️', 55],
            ['POS Billing Machine', 'hardware', 'All-in-one counter POS terminal compatible with POS Billingwala workflows. Built for busy billing counters.', '🖥️', 60],
            ['Cash Drawer', 'hardware', 'Electronic cash drawer for retail and restaurant counters. Opens with printer signal for secure cash handling.', '💰', 70],
            ['Barcode Scanner', 'hardware', 'USB / wireless barcode scanner for fast product billing. Speeds up retail checkout and stock entry.', '▤', 75],
            ['57mm Billing Rolls', 'consumables', 'Premium thermal billing rolls for 57mm / 58mm printers. Smooth printing, fade-resistant and long shelf life.', '🧾', 80],
            ['80mm Billing Rolls', 'consumables', 'Standard 80mm thermal rolls for restaurant and retail printers. Available in economical multi-roll packs.', '🧾', 90],
            ['Barcode Labels & Stickers', 'consumables', 'Self-adhesive barcode labels for inventory tagging, retail products and stock management.', '🏷️', 100],
            ['Printer Cleaning Kit', 'consumables', 'Head cleaning kit for thermal printers. Keeps print quality sharp and extends printer life.', '🧹', 110],
            ['Printer Power Adapter', 'accessories', 'Compatible power adapter for supported thermal printers. Reliable power supply for daily counter use.', '🔌', 120],
            ['USB & Bluetooth Cable', 'accessories', 'High-quality cables for printer and device connectivity. Durable build for busy billing environments.', '🔗', 130],
            ['Tablet Stand & Mount', 'accessories', 'Adjustable stand for Android tablet billing. Keeps screen visible and secure at the counter.', '📐', 140],
            ['Counter Accessories Kit', 'accessories', 'Essential billing counter add-ons — stands, cables and small accessories for a complete setup.', '🧰', 150],
        ];

        $now = now();
        foreach ($items as [$name, $category, $description, $icon, $order]) {
            WebsiteProduct::create([
                'name' => $name,
                'category' => $category,
                'description' => $description,
                'icon' => $icon,
                'sort_order' => $order,
                'is_published' => 1,
                'created_at' => $now,
                'updated_at' => $now,
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
            ['subscription', '6 Months', 3500, 'GST included', $sixMonths, 10, 0],
            ['subscription', '1 Year', 6000, 'GST included', $oneYear, 20, 1],
            ['renewal', '6 Months', 3000, 'GST included', $sixMonths, 30, 0],
            ['renewal', '1 Year', 5500, 'GST included', $oneYear, 40, 0],
        ];

        $now = now();
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
                'created_at' => $now,
                'updated_at' => $now,
            ]);
        }
    }

    private function seedDealers(): void
    {
        $dealers = [
            ['area' => 'Pune', 'dealer_name' => 'Pune Office', 'contact_person' => 'Santosh Dixit', 'role_title' => 'Sales & Marketing Manager', 'mobile' => '8983149299', 'whatsapp' => '8983149299', 'address' => 'S No 47, Pune - Satara Rd, opp. City Pride Multiplex, near Bhapkar petrol pump, Adinath Society, Taware Colony, Bibwewadi, Pune, Maharashtra 411009', 'map_url' => '', 'dealer_type' => 'head_office', 'sort_order' => 10],
            ['area' => 'Pandharpur', 'dealer_name' => 'Authorized POS Billingwala Dealer', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Pandharpur, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 20],
            ['area' => 'Satara', 'dealer_name' => 'Authorized POS Billingwala Dealer — Satara', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Satara, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 30],
            ['area' => 'Solapur', 'dealer_name' => 'Authorized POS Billingwala Dealer — Solapur', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Solapur, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 40],
            ['area' => 'Kolhapur', 'dealer_name' => 'Authorized POS Billingwala Dealer — Kolhapur', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Kolhapur, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 50],
            ['area' => 'Sangli', 'dealer_name' => 'Authorized POS Billingwala Dealer — Sangli', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Sangli, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 60],
            ['area' => 'Ahmednagar', 'dealer_name' => 'Authorized POS Billingwala Dealer — Ahmednagar', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Ahmednagar, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 70],
            ['area' => 'Nashik', 'dealer_name' => 'Authorized POS Billingwala Dealer — Nashik', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Nashik, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 80],
            ['area' => 'Mumbai', 'dealer_name' => 'Authorized POS Billingwala Dealer — Mumbai', 'contact_person' => '', 'role_title' => 'Authorized Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Mumbai, Maharashtra, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 90],
            ['area' => 'Karnataka', 'dealer_name' => 'Authorized POS Billingwala Dealer — Karnataka', 'contact_person' => '', 'role_title' => 'Regional Dealer', 'mobile' => '', 'whatsapp' => '', 'address' => 'Karnataka, India', 'map_url' => '', 'dealer_type' => 'authorized_dealer', 'sort_order' => 100],
        ];

        $now = now();
        foreach ($dealers as $row) {
            if (WebsiteDealer::where('area', $row['area'])->exists()) {
                continue;
            }
            WebsiteDealer::create(array_merge($row, [
                'is_published' => 1,
                'created_at' => $now,
                'updated_at' => $now,
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

    private function seedPages(): void
    {
        app(\App\Http\Controllers\WebsiteCatalogController::class)->seedDefaults();
        app(\App\Http\Controllers\WebsiteContentController::class)->seedWebsitePages();
    }
}
