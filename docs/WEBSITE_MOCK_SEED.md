# Seed website mock data

Inserts all demo content for the public website (products, pricing, dealers, customers, testimonials, settings, CMS pages).

## From admin folder (Laravel)

```powershell
cd "admin.posbillingwala.com"
php artisan website:seed-mock
```

Add only missing rows (default — skips tables that already have data).

## Fresh install (replace all website catalog data)

```powershell
cd "admin.posbillingwala.com"
php artisan website:seed-mock --fresh --force
```

## What gets seeded

| Table | Count | Content |
|-------|-------|---------|
| `website_settings` | 12 keys | Company, phone, email, Play Store URL |
| `website_products` | 17 | Software, hardware, consumables, accessories |
| `website_pricing_plans` | 4 | Subscription & renewal (₹3500–₹6000) |
| `website_dealers` | 10 | Pune head office + Maharashtra/Karnataka dealers |
| `website_clients` | 6 | Restaurants, retail, grocery, mess, clothing |
| `website_testimonials` | 3 | Customer reviews |
| `website_pages` | 6+ | About, privacy, terms, support, company, refund |

Contact form messages are **not** deleted.
