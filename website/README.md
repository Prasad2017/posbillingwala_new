# POS Billingwala Website 2.0

Modern React marketing website for [POS Billingwala](https://posbillingwala.com/) — a complete rebuild of the previous static HTML site with a premium SaaS UI, while preserving the live Admin CMS API.

## Project Overview

- **14 pages** with React Router
- **Live API** for products, pricing, dealers, customers, testimonials, settings, CMS pages, and contact form
- **Framer Motion** animations, Swiper sliders, SEO via React Helmet Async
- Production-ready Vite build with code-splitting and legacy URL redirects

The original `website/` folder is **not deleted**. This React app lives in `posbillingwala-react/` and can replace the marketing document root when you are ready to deploy.

## Technology Stack

| Technology | Purpose |
|---|---|
| React 19 + Vite | App framework & build |
| React Router | Client routing |
| Axios | API client |
| Framer Motion | Page & scroll animations |
| Lucide React | Icons |
| Swiper | Testimonials / screenshots |
| React Helmet Async | SEO meta tags |
| React CountUp | Trust counters |

## Installation

```bash
cd posbillingwala-react
npm install
```

## Environment Variables

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Public website API base (no trailing slash). Leave empty in production to use same-origin `/api/website`. For local/dev use `https://admin.posbillingwala.com/api/website` or `http://127.0.0.1:8000/api/website`. |
| `VITE_SITE_URL` | Canonical site origin for SEO (default `https://posbillingwala.com`) |

## Development

```bash
npm run dev
```

Open `http://127.0.0.1:5173`.

Optional local Admin API:

```bash
cd ../admin.posbillingwala.com
php artisan serve --host=127.0.0.1 --port=8000
```

Then set in `.env`:

```text
VITE_API_BASE_URL=http://127.0.0.1:8000/api/website
```

## Production Build

```bash
npm run build
npm run preview
```

Output is written to `dist/`.

## API Configuration

Public endpoints (same as the previous website):

| Method | Path | Data |
|---|---|---|
| GET | `/settings` | Company, support contacts, Play Store URL, logo |
| GET | `/products` | Product catalog |
| GET | `/pricing` | Subscription & renewal plans |
| GET | `/dealers` | Published dealers |
| GET | `/clients` | Customer showcase |
| GET | `/testimonials` | Reviews |
| GET | `/pages/{slug}` | CMS HTML (about, company, support, privacy, terms, refund-renewal) |
| POST | `/contact` | Contact form (`name`, `email`, `subject`, `message`) |

In production, deploy `public/api/website-proxy.php` with Apache rewrite (included as `public/.htaccess`) so browsers call same-origin `/api/website/*` and the proxy forwards to `admin.posbillingwala.com`.

## Routes

| Route | Page |
|---|---|
| `/` | Home |
| `/products` | Products |
| `/software` | Software |
| `/pricing` | Pricing |
| `/dealers` | Dealers |
| `/customers` | Customers |
| `/support` | Support |
| `/download-app` | Download App |
| `/contact` | Contact |
| `/company` | Company |
| `/about` | About |
| `/privacy-policy` | Privacy |
| `/terms-conditions` | Terms |
| `/refund-policy` | Refund |

Legacy `.html` URLs redirect to the new routes.

## Project Structure

```text
posbillingwala-react/
├── public/           # favicon, robots, sitemap, API proxy, .htaccess
├── src/
│   ├── assets/       # logo, app screenshots, business images
│   ├── components/   # layout, common, products, pricing, dealers…
│   ├── pages/        # 14 page modules
│   ├── services/     # Axios API layer
│   ├── hooks/        # useApi, useScrollPosition, useMediaQuery
│   ├── context/      # Website settings provider
│   ├── styles/       # design system CSS
│   ├── utils/        # constants, helpers, seo
│   ├── App.jsx
│   └── main.jsx
├── .env.example
└── package.json
```

## Deployment Instructions

1. Build: `npm run build`
2. Upload contents of `dist/` to the marketing document root (e.g. `public_html/`)
3. Ensure Apache `mod_rewrite` is enabled (SPA + API proxy)
4. Confirm `api/website-proxy.php` is present and reachable
5. Keep Admin CMS at `https://admin.posbillingwala.com`
6. Prefer empty `VITE_API_BASE_URL` for production builds so the app uses same-origin `/api/website`

### Commands

```bash
npm install
npm run dev
npm run build
npm run preview
```

## Notes

- Dealer finder uses **free-text + area filter** (matching the live API; there is no state/city cascading endpoint).
- Contact form requires a valid **email** (backend validation).
- Trust counters use **real API counts only** — never invented statistics.
- Dynamic content (products, pricing, dealers, customers, testimonials, legal pages) comes exclusively from the Admin API.
