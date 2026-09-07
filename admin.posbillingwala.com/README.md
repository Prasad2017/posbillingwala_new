# POS Billingwala — Web admin (`admin.posbillingwala.com`)

Laravel 9 web admin for dealers, customers, licences, catalog, sales, devices, crashes, support, push notifications, and the **Website CMS** that powers [posbillingwala.com](https://posbillingwala.com/).

Parent overview: [../README.md](../README.md) · Marketing site: [../website/README.md](../website/README.md)

## Stack

| Item | Value |
|------|--------|
| Framework | Laravel **9.x** (`laravel/framework: ^9.19`) |
| PHP | **^8.0.2** |
| Extras | Sanctum, Laravel UI, PhpSpreadsheet, Yajra DataTables, Guzzle |
| Database | Shared MySQL with Android apps / PHP API |

## Modules

| Area | Paths (auth required unless noted) |
|------|-------------------------------------|
| Auth | `/login` — Admin / Dealer / Customer portals |
| Dealers | `/dealer/*` CRUD |
| Customers & licences | `/customers/*`, licence add/edit/delete |
| Catalog | categories, products, subcategories, portions, portion-masters |
| Import / export | `/catalog-import-export/*`, `/import-export` |
| Expenses / inventory / invoices | list + edit (+ invoice download) |
| Sales | dashboard, overview, invoices |
| Reports | customers, licenses, dealers, branches, devices |
| Devices | `/devices` |
| Crashes | list, analytics, resolve/status |
| Support + push | tickets, FAQ, `/push-notifications` |
| Settings | profile, password, logo, favicon, users |
| **Website CMS** | `/website/*` — settings, products, pricing, dealers, clients, testimonials, about, privacy, terms, refund, support, contacts |

## Public Website API

Used by the marketing site (`routes/api.php`, prefix `website`):

| Endpoint | Data |
|----------|------|
| `GET /api/website/settings` | Company & legal footer |
| `GET /api/website/products` | Product catalog |
| `GET /api/website/pricing` | Plans |
| `GET /api/website/dealers` | Published dealers |
| `GET /api/website/clients` | Customer showcase |
| `GET /api/website/testimonials` | Quotes |
| `GET /api/website/pages/{slug}` | CMS pages |
| `POST /api/website/contact` | Contact form |

Production browsers usually call same-origin `https://posbillingwala.com/api/website/*` via `website/api/website-proxy.php` → this admin host.

## Local run

```bash
cd admin.posbillingwala.com
composer install
cp .env.example .env   # if needed
php artisan key:generate
php artisan serve --host=127.0.0.1 --port=8000
```

Or from repo root (Windows): `.\scripts\start-local.ps1` — admin on **:8000**, website on **:8080**.

| URL | Purpose |
|-----|---------|
| http://127.0.0.1:8000/login | Web admin login |
| http://127.0.0.1:8000/website | Website CMS |
| https://admin.posbillingwala.com/login | Production admin |

## Production deploy

1. Deploy this folder to the **admin** subdomain (not under `/adminpanel/`).
2. Set `.env` (`APP_DEBUG=false`, production DB).
3. `composer install --no-dev`
4. `php artisan key:generate` (once), then `config:cache` / `route:cache` / `view:cache`
5. Ensure SSL is valid for `admin.posbillingwala.com` (browsers block untrusted admin API fetches)
6. Run website CMS migration if needed: `API/migrations/p23_website_catalog.sql`

**Full checklist:** [../docs/DEPLOY_WEB.md](../docs/DEPLOY_WEB.md)

After CORS / config changes:

```bash
php artisan config:clear
php artisan config:cache
php artisan route:clear
```

## Related apps

| Client | Role |
|--------|------|
| `../Admin/` | Android admin (lighter mobile console) |
| `../Dealer/` | Field dealer app |
| `../Owner/` | Shop owner multi-branch app |
| `../WithTable/` | POS billing app |
| `../website/` | Public marketing site (CMS consumer) |
| `../API/` | PHP REST used by Android apps |
