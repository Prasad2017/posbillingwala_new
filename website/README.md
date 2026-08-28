# POS Billingwala — Marketing website

Static marketing site for [posbillingwala.com](https://posbillingwala.com/). Deploy the contents of this folder to your domain document root.

## Structure

```text
website/
  index.html          Home page
  privacy.html        Privacy policy
  .htaccess           Login redirects + PHP handler (cPanel)
  app-ads.txt         AdMob verification (if used)
  assets/
    css/site.css      Main styles
    js/site.js        Nav, scroll, reveal animations
    images/           Icons and template assets
    pos_images/       Hero & client photos
```

## Deploy

1. Upload everything under `website/` to the hosting document root (e.g. `public_html/`).
2. Place the Laravel web admin (`admin.posbillingwala.com/` or `adminpanel/`) alongside it so `/login` redirects work (see `.htaccess`).
3. Ensure `mod_rewrite` is enabled on Apache.

| URL | Purpose |
|-----|---------|
| `/` | Marketing homepage |
| `/privacy.html` | Privacy policy |
| `/login` | Redirects to web admin login |
| `/dealer/login` | Dealer portal login |

## Local preview

Open `index.html` in a browser, or serve the folder with any static server:

```bash
cd website
python -m http.server 8080
```

Then visit `http://localhost:8080`.

## Dynamic content (admin panel)

Clients, testimonials, and privacy policy are loaded from the Laravel admin API. Manage them under **Website Content** in the admin panel (admin login only).

| Admin path | What it controls |
|------------|------------------|
| `/website/privacy` | Privacy policy HTML |
| `/website/clients` | Customer logo, name, story on homepage |
| `/website/testimonials` | Customer quotes on homepage |

Public API (read-only):

| Endpoint | Data |
|----------|------|
| `/adminpanel/api/website/clients` | Published client showcase |
| `/adminpanel/api/website/testimonials` | Published testimonials |
| `/adminpanel/api/website/pages/privacy` | Privacy policy content |

Override API base on the static site by setting `window.PBW_WEBSITE_API` before `site-content.js` loads.

## Related

- Web admin: `admin.posbillingwala.com/` (Laravel)
- Full deploy checklist: [docs/DEPLOY_WEB.md](../docs/DEPLOY_WEB.md)
