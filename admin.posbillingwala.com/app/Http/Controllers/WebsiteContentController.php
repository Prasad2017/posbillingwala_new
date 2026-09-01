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

    public function seedWebsitePages(): void
    {
        $this->seedDefaults();
    }

    private function seedDefaults(): void
    {
        if (!WebsitePage::where('slug', 'privacy')->exists()) {
            WebsitePage::create([
                'slug' => 'privacy',
                'title' => 'Privacy Policy',
                'body_html' => $this->defaultPrivacyHtml(),
                'updated_at' => '2026-08-26 00:00:00',
            ]);
        } else {
            $privacy = WebsitePage::where('slug', 'privacy')->first();
            if ($privacy && !str_contains((string) $privacy->body_html, 'sawantp500@gmail.com')) {
                $privacy->body_html = $this->defaultPrivacyHtml();
                $privacy->updated_at = '2026-08-26 00:00:00';
                $privacy->save();
            }
        }

        if (!WebsitePage::where('slug', 'about')->exists()) {
            WebsitePage::create([
                'slug' => 'about',
                'title' => 'About Us',
                'body_html' => $this->defaultAboutHtml(),
                'updated_at' => now(),
            ]);
        } else {
            $about = WebsitePage::where('slug', 'about')->first();
            if ($about && !str_contains((string) $about->body_html, 'CANA Tech Solutions')) {
                $about->body_html = $this->defaultAboutHtml();
                $about->updated_at = now();
                $about->save();
            }
        }
    }

    private function defaultAboutHtml(): string
    {
        return '<p class="about-estd"><strong>Estd. 2022</strong></p>'
            . '<p>POS Billingwala is an offline-first billing platform built for restaurants, cafés, sweet shops, and retail counters across India. We started with a simple goal: keep billing fast and reliable even when the internet does not.</p>'
            . '<p>Since 2022, thousands of counters have trusted POS Billingwala for daily sales, thermal printing, table and takeaway billing, inventory tracking, and owner-level reporting — without depending on constant connectivity.</p>'
            . '<h2>Our mission</h2>'
            . '<p>Help every business bill faster, print reliably, and stay in control — from a single shop to multi-branch operations.</p>'
            . '<h2>What we offer</h2>'
            . '<p>Three connected Android apps work together with a secure web admin panel:</p>'
            . '<ul>'
            . '<li><strong>POS app</strong> — counter billing, KOT, mess tokens, combos, Bluetooth thermal printing, and offline sales</li>'
            . '<li><strong>Owner app</strong> — sales dashboards, catalog management, branch insights, and business overview on the go</li>'
            . '<li><strong>Dealer app</strong> — customer onboarding, licence management, and partner sales tools</li>'
            . '</ul>'
            . '<p>Owners and admins can also use the browser-based web panel for deeper reporting, user management, and configuration.</p>'
            . '<h2>Built for India</h2>'
            . '<p>Multilingual receipts (English, Hindi, Marathi), GST-friendly billing, combo and mess workflows, multi-branch sync, and hardware integrations designed for how Indian businesses actually run — morning rush to closing time.</p>'
            . '<h2>Why businesses choose us</h2>'
            . '<ul>'
            . '<li>Works offline — billing never stops when the network drops</li>'
            . '<li>Simple for staff, powerful for owners</li>'
            . '<li>Regular updates, support tickets, and dealer-backed onboarding</li>'
            . '<li>Affordable licensing for shops of every size</li>'
            . '</ul>'
            . '<h2>Developed by</h2>'
            . '<p>POS Billingwala is developed by <strong>CANA Tech Solutions Private Limited</strong>, a technology company focused on practical software for Indian small and medium businesses.</p>'
            . '<h2>Contact</h2>'
            . '<p>Questions, demos, or partnership enquiries? Use the contact form on our website or email <a href="mailto:support@posbillingwala.com">support@posbillingwala.com</a>.</p>';
    }

    private function defaultPrivacyHtml(): string
    {
        return '<p>POS Billingwala (&ldquo;POS Billingwala&rdquo;) respects the privacy rights of its customers and protects the personal information collected. To further this commitment, we have adopted this Privacy Policy (&ldquo;Privacy Policy&rdquo;) to guide how we collect and use the information you provide us.</p>'
            . '<h2>Scope of this Privacy Policy</h2>'
            . '<p>This Privacy Policy applies only to personal information and non-personal information submitted. By installing and using our app, you are accepting the practices described in this Privacy Policy. If you do not agree to this Privacy Policy, please do not install or use. We reserve the right to modify this Privacy Policy at reasonable times, so please review it frequently. Your continued use of the application will signify your acceptance of the changes to this Privacy Policy.</p>'
            . '<h2>Information We Collect</h2>'
            . '<p>When you use POS Billingwala, we may collect and process the following information depending on the features you use:</p>'
            . '<p><strong>Personal Information.</strong> We may collect your name, mobile phone number, address, and other information that you voluntarily provide while using the application or services.</p>'
            . '<p><strong>Device Information.</strong> We may collect device information, device identifiers, operating system information, application information, and other technical information required to maintain and secure the application.</p>'
            . '<p><strong>Location Information.</strong> We may collect approximate or precise location information when required for specific features and when permission is granted.</p>'
            . '<p><strong>Photos and Media.</strong> We may access photos or images that you choose to capture, upload, or provide through the application when required for app functionality.</p>'
            . '<p><strong>Crash, Diagnostic and Analytics Data.</strong> We may collect crash reports, error logs, diagnostic information, application performance information, and usage or analytics information. This information helps us identify crashes, troubleshoot problems, monitor application performance, and improve our services.</p>'
            . '<h2>How We Use Information</h2>'
            . '<p>The information we collect may be used to provide and operate the application, authenticate users, provide requested features, maintain security, troubleshoot errors and crashes, improve application performance, analyze usage, and improve our services.</p>'
            . '<h2>How We Store and Process Information</h2>'
            . '<p>Some information may be stored locally on your device. Depending on the features you use, information may also be securely transmitted to and processed by our servers or trusted third-party service providers that help us provide application functionality, synchronization, analytics, crash reporting, security, or other services.</p>'
            . '<p>We only collect information that is reasonably necessary for the relevant functionality and purposes described in this Privacy Policy.</p>'
            . '<h2>Third-Party Services</h2>'
            . '<p>POS Billingwala may use third-party SDKs, APIs, libraries, analytics services, crash-reporting services, cloud services, messaging services, or other technology providers. These third-party services may process certain information required to provide their respective services. Their processing of information is governed by their applicable privacy policies and terms.</p>'
            . '<h2>Data Security</h2>'
            . '<p>We take reasonable technical and organizational measures to protect the information we process against unauthorized access, loss, misuse, alteration, or disclosure. However, no method of electronic storage or transmission over the internet can be guaranteed to be completely secure.</p>'
            . '<h2>Data Retention and Deletion</h2>'
            . '<p>We retain information for as long as reasonably necessary to provide our services, comply with applicable legal obligations, resolve disputes, maintain security, and enforce our agreements.</p>'
            . '<p>Where applicable, users may request deletion of their personal information by contacting us using the contact details provided in this Privacy Policy. Certain information may be retained where required by law or for legitimate business and security purposes.</p>'
            . '<h2>Children&rsquo;s personal information</h2>'
            . '<p>The Services comply with the Children&rsquo;s Online Privacy Protection Act (&ldquo;COPPA&rdquo;). We do not knowingly collect personal information from children under the age of 13 through the Services.</p>'
            . '<h2>Links to third-party sites or services</h2>'
            . '<p>The Services may contain links to third-party sites, products or services. POS Billingwala is not responsible for the privacy practices or the content of such sites or services. If you are concerned about the privacy policy of a certain third party, we recommend that you read the privacy policy of the site or service to which you link before you submit any personal information.</p>'
            . '<h2>Changes to this Privacy Policy</h2>'
            . '<p>This policy may be updated at any time. We will publish any updated version via the Service. Please check this page from time to time. Your continued use of the App after changes take effect indicates acceptance of the amended policy. If you do not agree, uninstall the App and avoid further use.</p>'
            . '<h2>Questions</h2>'
            . '<p>If you have questions about this Privacy Policy, email <a href="mailto:sawantp500@gmail.com">sawantp500@gmail.com</a>.</p>'
            . '<p><em>This Privacy Policy was last updated: 26 August 2026.</em></p>';
    }

    public function hub()
    {
        $this->adminOnly();
        $this->seedDefaults();
        app(\App\Http\Controllers\WebsiteCatalogController::class)->seedDefaults();

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
        $this->seedDefaults();
        $page = WebsitePage::where('slug', 'privacy')->firstOrFail();

        return view('website.privacy', compact('page'));
    }

    public function updatePrivacy(Request $request)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'title' => 'required|string|max:255',
            'body_html' => 'required|string',
        ]);

        $page = WebsitePage::where('slug', 'privacy')->firstOrFail();
        $page->title = $validated['title'];
        $page->body_html = $validated['body_html'];
        $page->updated_at = now();
        $page->save();

        return redirect('website/privacy')->with('success', 'Privacy policy updated successfully');
    }

    public function about()
    {
        $this->adminOnly();
        $this->seedDefaults();
        $page = WebsitePage::where('slug', 'about')->firstOrFail();

        return view('website.about', compact('page'));
    }

    public function updateAbout(Request $request)
    {
        $this->adminOnly();
        $validated = $request->validate([
            'title' => 'required|string|max:255',
            'body_html' => 'required|string',
        ]);

        $page = WebsitePage::where('slug', 'about')->firstOrFail();
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
