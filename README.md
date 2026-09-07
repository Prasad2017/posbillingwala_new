# POS Billingwala

Offline-first point-of-sale for restaurants and shops — four Android apps, a shared PHP/MySQL API, a marketing website, and a Laravel web admin.

## What’s in this repo

| Path | Role |
|------|------|
| `WithTable/` | Main POS app — billing, tables, takeaway, mess + QR tokens, combos, reports, Bluetooth print, i18n |
| `Owner/` | Shop owner app — invoices, sales, multi-branch view, full catalog CRUD |
| `Dealer/` | Dealer app — customer & licence registration / renew, catalog setup |
| `Admin/` | Admin Android app — dealers, customers, licences, catalog, monitoring |
| `website/` | Marketing site (deploy to domain root) |
| `admin.posbillingwala.com/` | Laravel web admin + Website CMS API |
| `API/` | PHP REST API (POS root + `Owner/` / `Dealer/` / `Admin/`) |
| `API/migrations/` | Additive SQL upgrades (safe to re-run) |
| `API/schema/` | Install helper + schema-only reference |
| `docs/` | Deploy, licence / combo / store-details API notes, audit, implementation plan |
| `rgusomuk_posbilling.sql` | Full database dump (schema + data) |
| `releases/` | Local release APK copies (**gitignored**) |

Each Android app is a **standalone Gradle project** — open its folder in Android Studio (no root multi-module wrapper).

Per-app docs: [WithTable/README.md](WithTable/README.md) · [Owner/README.md](Owner/README.md) · [Dealer/README.md](Dealer/README.md) · [Admin/README.md](Admin/README.md) · [admin.posbillingwala.com/README.md](admin.posbillingwala.com/README.md) · [website/README.md](website/README.md)

## Features (current)

- **Offline-first billing** — bills save to SQLite first; sync via WorkManager + receivers when online
- **Universal multi-business POS** — business templates + feature flags (restaurant, mess, bar, retail, fashion, salon, bakery, weight/fresh); pack docs in `Billingwala_Universal_POS_COMPLETE/`
- **Catalog** — Food type → Category → optional Subcategory → Product → Portions linked to **Portion Master** (Half, Full, Kg, etc.)
- **Combo items** — separate combo master (not a product); fixed component list + manual sell price; bill on POS with invoice component snapshots
- **Store details** — structured shop name / address / phone lines for receipts (legacy company fields kept for sync compatibility)
- **Order modes** — dine-in tables, takeaway, mess membership + walk-in mess QR tokens (generate, print, scan/verify)
- **Print** — Bluetooth (Woosim/SPP); print failures do not wipe saved bills; optional BOT split for bar
- **Licensing** — server-authoritative expiry; 7-day / 50-bill trial; validity tiers (6m / 1y / 3y / 5y / lifetime); same-key renew
- **Auth** — login/MPIN issues Bearer tokens (`api_tokens`); guarded write endpoints
- **Multi-branch** — organization/branch scope; Owner store-wise comparison
- **i18n (POS)** — English / Hindi / Marathi (Settings → language; per-app locale)
- **Observability** — Firebase Crashlytics + Performance (POS); Analytics + Messaging across apps
- **Web admin** — dealers, customers, licences, catalog, CSV/Excel import, crashes, support, push, Website CMS

Live Universal schema: MySQL `p27`–`p32` (appointments, deposits, price tiers, variants, business templates, staff). Smoke checklist: `Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md`.

## Stack

| Layer | Tech |
|-------|------|
| Android | Java 17, Activities/Fragments, ViewBinding, SQLite, Retrofit, WorkManager, Firebase |
| API | PHP REST (`API/db_connection.php`, prepared helpers, auth tokens) |
| Web | Static marketing site (`website/`) + Laravel 9 admin (`admin.posbillingwala.com/`) |
| Database | MySQL — shared by all apps and web admin |

## App versions

| App | Module | Package | versionName / versionCode |
|-----|--------|---------|---------------------------|
| POS | `WithTable` | `com.pos_billingwala` | **2.0.59** / 75 |
| Owner | `Owner` | `com.posbillingwala.owner` | **1.0.7** / 8 |
| Dealer | `Dealer` | `com.posbillingwala.dealer` | **1.0.11** / 13 |
| Admin | `Admin` | `com.posbillingwala.admin` | **1.1** / 2 |

Default API host pattern: `https://posbillingwala.com/androidApp/` (+ `Owner/` / `Dealer/` / `Admin/`). Override via `BuildConfig.API_BASE_URL` in each app’s `build.gradle`. POS also uses `BuildConfig.MEDIA_BASE_URL` for product images.

## Prerequisites

| Tool | Use |
|------|-----|
| Android Studio (Ladybug+) | Build/run the four Android apps |
| JDK 17 | Matches `compileSdk 37` / Java 17 in Gradle |
| PHP 7.4+ with mysqli | Host `API/` on Apache/nginx |
| PHP 8.0+ + Composer | Laravel web admin (`admin.posbillingwala.com/`) |
| MySQL 5.7+ / MariaDB | Shared database for all clients |

Copy `API/db_local.example.php` → `API/db_local.php` locally. Firebase config (`google-services.json`) is gitignored — add your own for Crashlytics/Performance builds.

## Quick start — website & web admin

1. Upload `website/` to the hosting document root.
2. Upload/configure `admin.posbillingwala.com/` (Laravel web admin on subdomain).
3. Configure `admin.posbillingwala.com/.env` (production DB, `APP_DEBUG=false`).
4. Run `composer install --no-dev` inside the admin folder.
5. Run `php artisan key:generate`, then cache config/routes/views.
6. Ensure DB has catalog + licence + website CMS tables (see Database below).

**Full checklist:** [docs/DEPLOY_WEB.md](docs/DEPLOY_WEB.md)

| URL | Purpose |
|-----|---------|
| `https://posbillingwala.com/` | Marketing website |
| `https://posbillingwala.com/login` | Redirects to web admin login |
| `https://admin.posbillingwala.com/login` | Web admin (Admin / Dealer / Customer) |

## Quick start — database

### Fresh / full restore

```bash
mysql -u USER -p DATABASE < rgusomuk_posbilling.sql
```

Or phpMyAdmin → Import → `rgusomuk_posbilling.sql`.

### Existing DB upgrade (keep data)

Prefer the single upgrade script (food types, subcategories, portions, portion master, bill snapshots, API tokens, licensing, multi-branch, mess tokens, **combos**, **structured store details**, website CMS schema):

```bash
mysql -u USER -p DATABASE < API/migrations/server_upgrade_all.sql
```

| File | Use |
|------|-----|
| `API/migrations/server_upgrade_all.sql` | **Recommended** one-shot upgrade (safe to re-run) |
| `API/schema/posbill_install.sql` | Fresh install helper |
| `API/schema/schema_reference.sql` | Schema-only reference (no production rows) |
| `API/migrations/p23_website_catalog.sql` | Website CMS schema + sample data |

Individual reference migrations (optional; most included in `server_upgrade_all.sql`): `p3_1`–`p3_7`, `p3_5_portion_master`, `p5_3_api_tokens`, `p6_production_licensing`, `p7_multi_branch_scope`, `p8_mess_token_qr`, `p9_combo_items`, `p10_store_details_structured`, `p23_website_catalog`.

Full steps: [docs/DEPLOY_DB.md](docs/DEPLOY_DB.md)

## Quick start — API

1. Copy credentials template on the server:

   ```text
   API/db_local.example.php  →  API/db_local.php
   ```

2. Set `$dbHost`, `$dbUser`, `$dbPass`, `$dbName` (or env vars `DB_HOST`, `DB_USER`, `DB_PASS`, `DB_NAME`).

3. **Never commit** `API/db_local.php` or signing keys (`API/license_signing_private.pem`).

4. Deploy the `API/` folder so app base URLs resolve to these endpoints.

5. Run `server_upgrade_all.sql` (or at least combo + store-details migrations) **before** POS clients sync combos or structured store fields.

### Key API areas

| Area | Example endpoints |
|------|-------------------|
| POS auth & sync | `Login.php`, `LoginMpin.php`, `insertInvoice.php`, `getProductList.php` |
| Catalog | `getFoodTypeList.php`, `getCategoryList.php`, `getSubcategoryList.php`, `getPortionMasterList.php`, `getPortionList.php` |
| Combos | `insertCombo.php`, `insertComboItem.php`, `getComboList.php`, `getComboItemList.php`, `insertInvoiceComboItem.php` |
| Store / company | `insertCompanyDetail.php`, `getCompanyList.php`, `insertCompanyPrinterSetting.php` |
| Licensing | `check_licence_expire.php`, `registerTrial.php`, `licence_expiry.php` |
| Mess QR tokens | `insertMessToken.php`, `getMessTokenList.php`, `verifyMessToken.php` |
| Owner / Dealer / Admin | `*/Login.php`, catalog CRUD under each subfolder; Bearer token via `auth_guard.php` |

## Android apps

Open each module folder in Android Studio, point Retrofit base URLs at your deployed API, then build/run or assemble release APKs.

```bash
# From each app module directory (WithTable, Owner, Dealer, Admin)
./gradlew assembleDebug
./gradlew assembleRelease
```

Unsigned release copies may live under `releases/` (gitignored). Expected naming:

| File pattern | App |
|--------------|-----|
| `POS-Billingwala-{version}-v{code}-unsigned.apk` | WithTable (POS) |
| `Owner-{version}-v{code}-unsigned.apk` | Owner |
| `Dealer-{version}-v{code}-unsigned.apk` | Dealer |
| `Admin-{version}-v{code}-unsigned.apk` | Admin |

Sign with your upload keystore before store/rollout — do not commit keystores or passwords.

**Smoke test after deploy**

1. Login (licence + MPIN) → Bearer token issued  
2. Sync catalog → food types, categories, subcategories, portion masters, portions  
3. Add product with portion master + price → syncs to server  
4. Master Data → Combo → create combo with components + sell price → appear on Home → bill + print  
5. Store Details → shop name / address / phone lines → print header uses structured fields  
6. Bill with a portion product → print + invoice line snapshots  
7. Mess walk-in token → QR print → scan/verify (member tokens only)  
8. Switch POS language (EN / HI / MR) → UI strings update  
9. Trial / expired licence blocked server-side  

## Docs

| Doc | Description |
|-----|-------------|
| [docs/DEPLOY_DB.md](docs/DEPLOY_DB.md) | DB credentials, migrations, upgrade notes |
| [docs/DEPLOY_WEB.md](docs/DEPLOY_WEB.md) | Website + web admin production deploy |
| [docs/LICENSE_API_REQUIREMENTS.md](docs/LICENSE_API_REQUIREMENTS.md) | Licensing / trial API behaviour |
| [docs/COMBO_API_REQUIREMENTS.md](docs/COMBO_API_REQUIREMENTS.md) | Combo master contract |
| [docs/STORE_DETAILS_API_CHANGES.md](docs/STORE_DETAILS_API_CHANGES.md) | Structured shop / address / phone fields |
| [docs/CURSOR_CODEBASE_AUDIT.md](docs/CURSOR_CODEBASE_AUDIT.md) | Codebase audit |
| [docs/CURSOR_IMPLEMENTATION_PLAN.md](docs/CURSOR_IMPLEMENTATION_PLAN.md) | Implementation plan |
| [docs/POS_BILLINGWALA_BUSINESS_AND_WEBSITE_MODEL.md](docs/POS_BILLINGWALA_BUSINESS_AND_WEBSITE_MODEL.md) | Business + website model |

## Security notes

- Production DB passwords and licence signing keys stay off git (see `.gitignore`).
- If a full SQL dump was ever shared, rotate MySQL passwords and treat exposed licences / MPINs / contacts as compromised — see [docs/DEPLOY_DB.md](docs/DEPLOY_DB.md).

## Repository

https://github.com/Prasad2017/posbillingwala_new
