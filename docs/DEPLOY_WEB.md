# Website & web admin deploy

Production deployment for **posbillingwala.com** (marketing site) and **adminpanel** (Laravel web admin aligned with the Android Admin app).

## What gets deployed

| Path on server | URL | Purpose |
|----------------|-----|---------|
| `posbillingwala.com/` (root) | `https://posbillingwala.com/` | Marketing website |
| `posbillingwala.com/adminpanel/` | App files (assets stay here) | Laravel web admin |
| Login URL | `http://posbillingwala.com/login` | Clean domain-root login (rewrites into adminpanel) |

The web admin shares the **same MySQL database** as the Android apps and `API/`. See [DEPLOY_DB.md](DEPLOY_DB.md) for database setup and migrations.

---

## Prerequisites

- cPanel or PHP hosting with **PHP 8.0+** (project uses PHP 8.2 handler)
- **MySQL** database (same DB as POS API)
- **Composer** on the server (SSH or cPanel Terminal)
- SSL certificate (recommended; site uses HTTPS links)

Required DB tables for full admin features (included in `API/schema/posbill_install.sql` or `API/migrations/server_upgrade_all.sql`):

- `food_types`
- `product_subcategories`
- `product_portions`

---

## 1. Upload files

Upload the `posbillingwala.com/` folder to your domain document root (e.g. `public_html/` or `posbillingwala.com/`).

Typical structure after upload:

```text
public_html/
  index.html
  privacy.html
  assets/
  adminpanel/
    app/
    config/
    public/          ← Laravel public (or use adminpanel root with .htaccess)
    resources/
    routes/
    composer.json
    .htaccess
```

**Do not upload** (already gitignored; create on server):

- `adminpanel/.env`
- `adminpanel/vendor/` (install via Composer)
- `adminpanel/node_modules/`

---

## 2. Laravel admin — environment

On the server, inside `adminpanel/`:

```bash
cp .env.example .env
```

Edit `.env` for production:

```env
APP_NAME="POS Billingwala"
APP_ENV=production
APP_DEBUG=false
APP_URL=https://posbillingwala.com/adminpanel

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=your_database_name
DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password
```

Generate application key (once):

```bash
php artisan key:generate
```

---

## 3. Install PHP dependencies

Excel product import requires **PhpSpreadsheet**. From `adminpanel/`:

```bash
composer install --no-dev --optimize-autoloader
```

If `composer` is not in PATH, use cPanel **Terminal** or the full path to PHP/Composer provided by your host.

Verify:

```bash
php artisan --version
```

---

## 4. Laravel permissions & cache

```bash
chmod -R 775 storage bootstrap/cache
# On cPanel, assign ownership to the web user if needed (e.g. chown)

php artisan config:cache
php artisan route:cache
php artisan view:cache
```

Ensure `storage/` and `bootstrap/cache/` are writable by the web server.

---

## 5. Web server / document root

### Option A — Admin at `/adminpanel` (current layout)

- Website: domain root serves `index.html`
- Admin: `https://posbillingwala.com/adminpanel/` uses existing `adminpanel/index.php` and `.htaccess`

Confirm `adminpanel/.htaccess` allows URL rewriting (Apache `mod_rewrite`).

### Option B — Point subdomain to Laravel `public/` (optional)

Some hosts point `admin.posbillingwala.com` to `adminpanel/public/` for cleaner URLs. Adjust `APP_URL` accordingly.

---

## 6. Database

1. Import or upgrade DB using [DEPLOY_DB.md](DEPLOY_DB.md).
2. Confirm `users`, `licenses`, `categories`, `products`, `food_types`, `product_subcategories`, `product_portions` exist.

Existing production DB: run **only**:

```bash
mysql -u USER -p DATABASE < API/migrations/server_upgrade_all.sql
```

---

## 7. Logins

| Role | URL | Credentials |
|------|-----|-------------|
| **Admin** | `/login` | Email + password (`users.role_id = 1`) |
| **Dealer** | `/dealer/login` | Aadhar number + password |
| **Customer** | `/customer/login` | Contact number + secret key |

Customer portal secret key is currently hardcoded in `CustomerController` — change before public exposure if needed.

---

## 8. Post-deploy smoke test

### Website

- [ ] `https://posbillingwala.com/` loads with new UI
- [ ] Navigation scrolls to sections (About, Features, Clients, Contact)
- [ ] Google Play badge opens Play Store listing
- [ ] `privacy.html` loads and matches site styling
- [ ] Favicon / app icon displays

### Web admin

- [ ] Admin login works
- [ ] Dashboard shows dealer/customer counts
- [ ] Customer list → action buttons (Edit, License, Categories, Products)
- [ ] Add customer with license (7 Days → Lifetime tiers, POS flags)
- [ ] Categories: food type dropdown saves
- [ ] Subcategories page opens from category actions
- [ ] Products: portions page opens from product actions
- [ ] **Product Import**: upload `.xlsx` template (after `composer install`)
- [ ] Dealer login → dealer dashboard (not redirect loop)

---

## 9. Product import (CSV / Excel)

1. Go to **Product Import** in sidebar.
2. Select customer.
3. Download template: [CustomerProductList.xlsx](https://www.posbillingwala.com/androidApp/DemoExcel/CustomerProductList.xlsx)
4. Upload filled **`.xlsx`** or **`.csv`**.

Column order (row 1 may be header):

| Product | Category | Unit | Price | CGST | SGST |
|---------|----------|------|-------|------|------|

Import creates missing categories and adds/updates products by name (same logic as Android Admin).

---

## 10. Security checklist (production)

- [ ] `APP_DEBUG=false`
- [ ] `.env` not web-accessible (outside public root or blocked by server)
- [ ] Strong admin passwords
- [ ] Rotate customer secret key if exposed
- [ ] HTTPS enforced
- [ ] `API/db_local.php` and `.env` never in git (see root `.gitignore`)

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 500 on admin | Check `storage/logs/laravel.log`; fix permissions on `storage/` |
| Excel import fails | Run `composer install` in `adminpanel/` |
| Blank categories/subcategories | Run DB migration for `food_types`, `product_subcategories`, `product_portions` |
| CSS/JS 404 on website | Confirm `assets/` uploaded; paths are relative to domain root |
| Admin routes 404 | Enable `mod_rewrite`; check `adminpanel/.htaccess` |

---

## Related docs

- [DEPLOY_DB.md](DEPLOY_DB.md) — database credentials and migrations
- [LICENSE_API_REQUIREMENTS.md](LICENSE_API_REQUIREMENTS.md) — licence API behaviour
- [README.md](../README.md) — project overview
