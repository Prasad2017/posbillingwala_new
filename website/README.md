# POS Billingwala — Marketing website

Static marketing site for [posbillingwala.com](https://posbillingwala.com/). Deploy the contents of this folder to your domain document root.

## Structure

```text
website/
  index.html          Home
  products.html       Products catalog (CMS)
  software.html       Software features
  pricing.html        Subscription & renewal plans (CMS)
  dealers.html        Area-wise dealer finder (CMS)
  customers.html      Trusted customers (CMS)
  support.html        Customer support
  download.html       App download
  contact.html        Contact form
  company.html        Company model (CMS)
  about.html          About Us (CMS)
  privacy.html        Privacy policy (CMS)
  terms.html          Terms & conditions (CMS)
  refund.html         Refund & renewal policy (CMS)
  assets/
    css/site.css
    js/site-layout.js   Shared nav & footer
    js/site-icons.js    SVG icon system
    js/site-tabs.js     Tab & filter pill components
    js/site.js          Nav scroll & animations
    js/site-content.js  CMS data from admin API
    js/site-contact.js  Contact form
```

## Website menu

`HOME | PRODUCTS | SOFTWARE | PRICING | DEALERS | CUSTOMERS | SUPPORT | DOWNLOAD | CONTACT`

## Deploy

1. Upload everything under `website/` to the hosting document root (e.g. `public_html/`).
2. Admin panel runs on **subdomain** `https://admin.posbillingwala.com` (there is no `/adminpanel/` folder on the main site).
3. Ensure `mod_rewrite` is enabled on Apache.

| URL | Purpose |
|-----|---------|
| `/` | Home |
| `/products.html` | Product catalog |
| `/pricing.html` | Software plans |
| `/dealers.html` | Find local dealer |
| `/contact.html` | Contact / demo form |
| `/login` | Redirects to `admin.posbillingwala.com/login` |

## Local preview

```bash
cd website
python -m http.server 8080
```

CMS pages show fallback content offline; live data loads when admin API is reachable.

## Dynamic content (admin panel)

Manage under **Settings → Website Content** in the admin panel (admin login only).

| Admin path | What it controls |
|------------|------------------|
| `/website/settings` | Company name, GSTIN, address, support numbers, tagline |
| `/website/products` | Products page catalog |
| `/website/pricing` | Subscription & renewal pricing table |
| `/website/dealers` | Area-wise dealer network |
| `/website/support` | Support page HTML content |
| `/website/clients` | Trusted customers showcase |
| `/website/testimonials` | Customer quotes |
| `/website/about` | About Us page |
| `/website/privacy` | Privacy policy |
| `/website/terms` | Terms & conditions |
| `/website/refund` | Refund & renewal policy |
| `/website/contacts` | Website contact form enquiries |

Public API base: `https://admin.posbillingwala.com/api/website`

| Endpoint | Data |
|----------|------|
| `.../settings` | Company & legal footer info |
| `.../products` | Product catalog |
| `.../pricing` | Pricing plans |
| `.../dealers` | Published dealers |
| `.../clients` | Customer showcase |
| `.../testimonials` | Testimonials |
| `.../pages/{slug}` | CMS pages (about, privacy, terms, support, refund-renewal) |
| `.../contact` | POST contact form |

Override API base: set `window.PBW_WEBSITE_API` before `site-config.js` loads.

### Website shows no data but admin has data?

The public site loads content from the **admin subdomain API** (not directly from DB). Check in browser:

| Test URL | Expected |
|----------|----------|
| `https://admin.posbillingwala.com/api/website/dealers` | JSON `{"success":true,"dealers":[...]}` |
| `https://admin.posbillingwala.com/api/website/pages/privacy` | JSON with privacy HTML |

**Fix checklist:**

1. Deploy latest `website/` files (`site-config.js`, `site-content.js`, HTML pages — no `/adminpanel/` references)
2. Deploy latest `admin.posbillingwala.com` Laravel code (API routes under `/api/website/...`)
3. Run `API/migrations/p23_website_catalog.sql` on server DB if tables empty
4. SSH on admin server: `php artisan route:clear && php artisan config:clear`
5. Hard refresh website (Ctrl+F5)

## Database migration (production server)

Website CMS tables are also in:

- `API/migrations/p23_website_catalog.sql` — **schema + sample data** (run once on admin DB)
- Included in `API/migrations/server_upgrade_all.sql` (p23 schema only; run `p23_website_catalog.sql` for sample data)

**Sample data in p23 includes:** company settings, 8 products, 4 pricing plans, 10 area dealers (Pune head office + Santosh Dixit contact), CMS pages (terms, refund, support, company), 3 demo customers.

Or auto-create + seed: open **Admin → Website Content** once (Laravel).

**Note:** Main POS tables (`users`, `licences`, `invoice`, etc.) are **not** changed.

## Business model doc

See [docs/POS_BILLINGWALA_BUSINESS_AND_WEBSITE_MODEL.md](../docs/POS_BILLINGWALA_BUSINESS_AND_WEBSITE_MODEL.md).

## Related

- Web admin: `admin.posbillingwala.com/` (Laravel)
- Full deploy checklist: [docs/DEPLOY_WEB.md](../docs/DEPLOY_WEB.md)
