# POS Billingwala

Offline-first point-of-sale system for restaurants and shops — Android apps plus a shared PHP/MySQL backend.

## What’s in this repo

| Path | Role |
|------|------|
| `WithTable/` | Main POS app (billing, tables, takeaway, mess, reports, Bluetooth print) |
| `Owner/` | Shop owner app — invoices, products, sales visibility |
| `Dealer/` | Dealer app — customer & licence registration |
| `Admin/` | Admin app — dealers & customers |
| `posbillingwala.com/` | Production website + Laravel web admin |
| `API/` | PHP REST API (POS root + Owner / Dealer / Admin folders) |
| `docs/` | Audit, implementation plan, DB deploy, licence API notes |
| `spllmgkn_posbill_complete.sql` | Full database dump (schema + data) |
| `releases/` | Local release APK copies (not committed) |

## Stack

- **Android:** Java, Activities/Fragments, ViewBinding, SQLite, Retrofit, WorkManager
- **Backend:** PHP REST endpoints
- **Web:** Static marketing site + Laravel 9 admin panel (`posbillingwala.com/adminpanel`)
- **Database:** MySQL (shared by all apps via `API/db_connection.php`)

## Quick start — website & web admin

Deploy the public site and Laravel admin to your domain:

1. Upload `posbillingwala.com/` to hosting document root.
2. Configure `adminpanel/.env` (production DB, `APP_DEBUG=false`).
3. Run `composer install --no-dev` inside `adminpanel/`.
4. Run `php artisan key:generate`, then cache config/routes/views.
5. Ensure DB has food type / subcategory / portion tables (see DB deploy below).

**Full checklist:** [docs/DEPLOY_WEB.md](docs/DEPLOY_WEB.md)

| URL | Purpose |
|-----|---------|
| `https://posbillingwala.com/` | Marketing website |
| `http://posbillingwala.com/login` | Web admin login (Admin / Dealer / Customer links) |
| `https://posbillingwala.com/adminpanel/` | Legacy admin URL (redirects to `/login`) |

Web admin features match the Android Admin app: dealers, customers, licences, categories (food type), subcategories, product portions, and CSV/Excel product import.

## Quick start — database

### Fresh / full restore

Import the complete dump:

```bash
mysql -u USER -p DATABASE < spllmgkn_posbill_complete.sql
```

Or use phpMyAdmin → Import → select `spllmgkn_posbill_complete.sql`.

### Existing DB upgrade (keep data)

Prefer the single upgrade script:

```bash
mysql -u USER -p DATABASE < API/migrations/server_upgrade_all.sql
```

Schema-only reference (no production rows): `API/schema/schema_reference.sql`  
Install helper: `API/schema/posbill_install.sql`

Full steps: [docs/DEPLOY_DB.md](docs/DEPLOY_DB.md)

## Quick start — API

1. Copy credentials template on the server:

   ```text
   API/db_local.example.php  →  API/db_local.php
   ```

2. Set `$dbHost`, `$dbUser`, `$dbPass`, `$dbName` (or env vars `DB_HOST`, `DB_USER`, `DB_PASS`, `DB_NAME`).

3. **Never commit** `API/db_local.php` or signing keys (`API/license_signing_private.pem`).

4. Deploy the `API/` folder to your PHP host so app base URLs point at those endpoints.

## Android apps

Open each module in Android Studio:

- `WithTable` — primary POS (`com.pos_billingwala`)
- `Owner` — `com.posbillingwala.owner`
- `Dealer` — `com.posbillingwala.dealer`
- `Admin` — `com.posbillingwala.admin`

Point Retrofit base URLs at your deployed API, then build/run or assemble release APKs.

## Docs

| Doc | Description |
|-----|-------------|
| [docs/DEPLOY_DB.md](docs/DEPLOY_DB.md) | DB credentials, migrations, upgrade notes |
| [docs/DEPLOY_WEB.md](docs/DEPLOY_WEB.md) | Website + web admin production deploy |
| [docs/LICENSE_API_REQUIREMENTS.md](docs/LICENSE_API_REQUIREMENTS.md) | Licensing / trial API behaviour |
| [docs/CURSOR_CODEBASE_AUDIT.md](docs/CURSOR_CODEBASE_AUDIT.md) | Codebase audit |
| [docs/CURSOR_IMPLEMENTATION_PLAN.md](docs/CURSOR_IMPLEMENTATION_PLAN.md) | Implementation plan |

## Security notes

- Production DB passwords and licence signing keys stay off git (see `.gitignore`).
- If a full SQL dump was ever shared, rotate MySQL passwords and treat exposed licences / MPINs / contacts as compromised — see [docs/DEPLOY_DB.md](docs/DEPLOY_DB.md).

## Repository

https://github.com/Prasad2017/posbillingwala_new
