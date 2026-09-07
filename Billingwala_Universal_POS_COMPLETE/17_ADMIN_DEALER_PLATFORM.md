# 17 Admin Dealer Platform

Sibling apps + Laravel web admin — not inside POS billing core.

## Roles
| App | Role |
|-----|------|
| Admin | Dealers, customers, licences, crash inbox, CMS, **business template per licence** |
| Dealer | Licence register/renew, catalog setup, **business template for own customers’ licences** |
| Owner | Multi-branch sales/catalog, **outlet business template** (Owner ops hub) |
| POS WithTable | Offline storefront billing; applies template on Fetch Data |
| admin.posbillingwala.com | Web ops + Website CMS |

## Business template (platform → POS)
Shared table: `company_business_templates` (migration **p31**).  
`userId` column = **licence id**. POS reads via `androidApp/getBusinessTemplate.php`.

| Actor | API (deploy under matching folder) | Auth | UI entry |
|-------|--------------------------------------|------|----------|
| Admin | `API/Admin/getBusinessTemplate.php`, `setBusinessTemplate.php` | Admin Bearer | Customer → Licences → **Business template** |
| Admin | `API/Admin/insertCustomer.php` | Admin Bearer | New customer → **Template** spinner (`admin_register`) |
| Admin | `API/Admin/insertNewLicence.php` | Admin Bearer | New franchise licence → **Template** spinner (`admin_register`) |
| Dealer | `API/Dealer/getBusinessTemplate.php`, `setBusinessTemplate.php` | Dealer Bearer + owns customer | Customer → licence → **Business template** |
| Dealer | `API/Dealer/insertCustomer.php` | Dealer Bearer | New customer → **Template** spinner (`dealer_register`) |
| Dealer | `API/Dealer/insertNewLicence.php` | Dealer Bearer | New franchise licence → **Template** spinner (`dealer_register`) |
| Owner | `API/Owner/getBusinessTemplate.php`, `setBusinessTemplate.php` | Owner + branch access | Settings → Outlet tools |
| Web admin | Eloquent upsert via `App\Support\BusinessTemplateSupport` | Web session | Add/Edit customer & licence → **Business template** |

Shared helper: `API/business_template_ops.php`  
Network status labels: `admin_push` / `dealer_push` / `owner_push` / `admin_register` / `dealer_register` / `web_admin`  
On register: picking a template auto-suggests Fast/Dine/Takeaway/Mess flags (overridable); server fills defaults if all four were `0`.  
On **set template** for an existing licence: optional `syncModules=1` (UI checkbox, default on) updates the same licence module flags.

## Facade
`WithTable/.../Extra/AdminDealerPlatform.java` — role + module summary for docs/debug.
