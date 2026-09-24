# Billingwala POS (Web)

Online-only React POS for restaurants, mess, and retail billing. **Browser only** — no offline queue, no Bluetooth/USB printers.

Companion to the Flutter mobile apps (`posbillingwala-flutter`). This package replaces Flutter web.

## Stack

- Vite + React 19 + TypeScript
- React Router 7
- Zustand (session / cart)
- TanStack Query
- Axios (`application/x-www-form-urlencoded` → existing PHP API)

## Run

```bash
cd posbillingwala-web
cp .env.example .env   # if needed
npm install
npm run dev
```

Dev server: `http://localhost:5174`

```bash
npm run build
npm run preview
```

## Env

| Variable | Default |
|----------|---------|
| `VITE_API_BASE_URL` | `https://posbillingwala.com/androidApp/` |
| `VITE_MEDIA_BASE_URL` | `https://posbillingwala.com/storage/app/` |

## Auth flow

1. `/login` — licence key  
2. `/mpin` — PB-PIN → Bearer token  
3. `/staff-login` — when user management is enabled  
4. App shell (Home, Billing, Tables, Takeaway, Mess, Masters, Inventory, Reports, Settings)

Soft **Lock** clears the token and returns to MPIN. **Logout** clears the licence session.

## Online-only policy

- Requires network (`navigator.onLine` + API calls)
- Offline banner blocks billing UX
- Invoices are saved directly via `insertInvoice.php` / line endpoints — no IndexedDB sync queue

## Print

- Browser receipt / KOT preview + `window.print()`
- Optional cloud print jobs via `createPrintJob.php` (see Settings → Print jobs)

## Licence modules

Nav tiles respect login flags `fastBilling`, `dineIn`, `takeAway`, `mess` and staff permissions (`billing.create`, `table.view`, …).

## Deploy

Build static assets (`dist/`) and host on e.g. `pos.posbillingwala.com` or a path on the main domain. Point SPA fallback to `index.html`. CORS is already enabled on the PHP API.

## Screen map (parity target)

See Flutter web screen map in `posbillingwala-flutter/README.md` — this app mirrors those routes under the same path names.
