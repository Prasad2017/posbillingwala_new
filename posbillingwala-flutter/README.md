# POS Billingwala Flutter (`pos_billingwala_v2`)

Offline-first POS for restaurants and messes — **Android**, **iOS**, and **Web**.  
Version **2.0.1+76** · Dart SDK `^3.12.2`

Local sales, catalog, tables, takeaway, and mess data live in **Drift (SQLite)** and sync to PHP APIs under `https://posbillingwala.com/androidApp/`.

---

## Stack

| Layer | Tech |
|-------|------|
| UI / state | Flutter + Riverpod |
| Navigation | go_router (`lib/app/router.dart`) |
| Local DB | Drift + SQLite |
| HTTP | Dio → PHP `androidApp/` scripts |
| Session | SharedPreferences |
| Print | Bluetooth / USB / network ESC-POS |
| Push | Firebase Messaging + local notifications |

Feature code lives under `lib/features/*`. Shared code: `lib/core`, `lib/app`, `lib/language`.

---

## Run

```bash
cd posbillingwala-flutter
flutter pub get
flutter run
```

Optional build defines:

```bash
flutter run --dart-define=API_BASE_URL=https://posbillingwala.com/androidApp/ \
            --dart-define=MEDIA_BASE_URL=https://posbillingwala.com/storage/app/
```

---

## Auth flow

```
/splash → AuthController.bootstrap()
  ├─ no session              → /login
  ├─ session, no auth token  → /mpin
  └─ session + token         → / (home)

Login paths:
  A) Licence key  → Login.php → /mpin → LoginMpin.php → /
  B) Staff mobile+PIN → staffLogin.php → /
  C) Trial register → registerTrial.php → licence + MPIN → /
```

Staff routes are gated by permissions (e.g. `billing.create`, `table.view`, `mess.view`, `report.view`). See `staffRoutePermissionRedirect` in `router.dart`.

---

## Feature modules

| Module | Role |
|--------|------|
| **auth** | Licence, MPIN, trial register, session, device bind |
| **home** | Dashboard KPIs, banners, module tiles |
| **pos** | Catalog cart, payment checkout, KOT |
| **tables** | Floor plan, dining sessions, split bill |
| **takeaway** | Open parcels → POS billing |
| **mess** | Members, coupons, QR tokens, payments, meal sessions |
| **masters** | Categories, products, portions, combos, tables |
| **inventory** | Stock purchase/waste + expense sync |
| **expense** | Expense list / add UI |
| **reports** | Sales, operational, mess, expense, product, staff |
| **settings** | Company, devices, hours, PIN, about, share |
| **print** | Printers, routing, queue, bill/mess test previews |
| **staff** | Staff CRUD, permissions, salary |
| **sync** | Full fetch / upload |
| **notifications** | In-app inbox + meal-token queue |
| **support** | Support tickets |
| **payment_display** | Customer-facing payment QR (Android) |
| **company** | Company / printer-setting API DTOs |

---

## Screens & data

### Auth

| Screen | Route | Data shown / submitted | Sources |
|--------|-------|------------------------|---------|
| Splash | `/splash` | Boot / offline retry | `authControllerProvider`, licence scope, in-app update (Android) |
| Login | `/login` | Staff mobile+PIN **or** licence key; language | `Login.php`, `updateAndroidKey.php`, `staffLogin.php` |
| MPIN | `/mpin` | 4-digit PB-PIN | `LoginMpin.php` or local PIN; `SessionStore` |
| Register | `/register` | Name, contact, shop, address → trial | `registerTrial.php` |
| Staff login | `/staff-login` | Mobile + PIN | `staffLogin.php`, `StaffStore` |

### Home

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Home | `/` | Greeting, shop, printer chip, period sales KPIs, catalog counts, Fast / Dine / Takeaway / Mess tiles | `homeDashboardKpisProvider`, `catalogCountsProvider`, `getHomeSalesOverview.php`, Drift invoices + catalog |
| Banner carousel | *(on Home)* | Promo images | `getHomeBannerList.php` (`homeBannersProvider`) |

**Home shortcuts**

| Control | Goes to |
|---------|---------|
| Notifications | `/notifications` |
| Settings | `/settings` |
| Printer chip | `/settings/devices` |
| Sales cards | `/reports` (needs `report.view`) |
| Categories / Subcategories / Products / Combos | Masters routes |
| Fast Billing | `/pos` |
| Dine In | `/tables` |
| Take Away | `/takeaway` |
| Mess | `/mess` |

Licence flags (`fastBilling`, `dineIn`, `takeAway`, `mess`) plus staff permissions control which tiles appear. If no module flags are set, all billing modules are treated as unlocked (legacy licence).

### POS / payment / KOT

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| POS | `/pos`, `/tables/billing`, `/takeaway/billing` | Categories, products, combos, cart, totals; KOT (dine-in) | Drift: `CartItems`, `CartComboItems`, catalog, `Kots` / `KotItems` |
| Payment | `/pos/payment`, `/tables/payment`, `/takeaway/payment` | Customer, discount, packing, cash/UPI/split → save/print | `Invoices`, `InvoiceItems`, `InvoiceComboItems`; sync `insertInvoice.php` |
| KOT preview | *(pushed)* | KOT lines before print | `kotControllerProvider`, print dispatcher |

### Tables & takeaway

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Tables | `/tables` | Floor status, areas, open/settle/join/hold/split | `PosTables`, `DiningSessions`, `CartItems`; `insertDiningSession.php` |
| Split bill | `/tables/split-bill?table=&sessionId=` | Equal / by items / by amount partial pay | Cart + `DiningSessions.paidAmount` |
| Takeaway | `/takeaway` | Open parcels (`P1`…) + amounts; optional name/phone | `CartItems` by takeaway scope |

### Mess

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Mess hub | `/mess` | Payer mode, stats, member grid → coupon/QR | `MessMembers`, payments, invoices, tokens, meal queue; mess APIs |
| Members | `/mess/members` | PIN-gated member CRUD (+ opening payment) | `MessMembers`, `insertMessMember.php` |
| QR management | `/mess/qr` | Shop common QR generate / status / print | `mess_qr_*.php` |
| Meal sessions | `/mess/meal-sessions` | Session name, times, prefix, active | `mess_meal_session_list/save.php` (cloud cache) |
| Meal tokens today | `/mess/meal-tokens-today` | Today’s tokens, reprint, cancel | `MessMealTokenQueue`; `mess_meal_token_*.php` |
| Payments | `/mess/payments` | History + record payment (`extra`: member) | `MessMemberPayments`; payment APIs |
| Token scan | `/mess/scan` | Camera verify member QR | `MessTokens`, `verifyMessToken.php` |
| Coupon | *(pushed)* | Paper coupon preview/print | `MessInvoices`, `insertMessInvoice.php` |
| Token QR | *(pushed)* | Member QR slip preview/print | `MessTokens`, `insertMessToken.php` |

### Masters

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Masters hub | `/masters` | Nav only | — |
| Catalog | `/masters/catalog?tab=` | Combined products / combos | Catalog Drift + `MastersApi` |
| Categories | `/masters/categories` | Category CRUD | `ProductCategories`, `insertCategory.php` |
| Subcategories | `/masters/subcategories` | Subcategory CRUD | `ProductSubcategories` |
| Products | `/masters/products` | Product list / search | `Products`, `ProductPortions` |
| Product form | `/masters/products/form?id=` | Create/edit product + portions | Products + optional opening stock |
| Product portions | `/masters/products/portions?id=` | Portions for one product | `ProductPortions`, `PortionMasters` |
| Combos | `/masters/combos` | Combo list | `Combos` |
| Combo form | `/masters/combos/form?id=` | Combo + components | `Combos`, `ComboItems` |
| Portion masters | `/masters/portion-masters` | Global portion labels | `PortionMasters` |
| Table master | `/masters/tables` | Areas, types, tables | `DiningAreas`, `TableTypes`, `PosTables` |

### Inventory & expenses

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Inventory | `/inventory?tab=` | Stock balances + expenses tabs | `InventoryMovements`, `ShopExpenses`, `Products` |
| Add stock / waste | `/inventory/add`, `/inventory/waste` | Product, qty, cost/note | `InventoryMovements`; inventory APIs |
| Expenses | `/expenses` | List + total | `ShopExpenses` |
| Add expense | `/expenses/add` | Name + amount | `insertExpenses.php` |

### Reports

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Reports hub | `/reports` | Report launcher | `report.view` |
| Sales dashboard | `/reports/dashboard` | KPIs, payment mix, trend | `Invoices` aggregates |
| Sales overview | `/reports/overview` | MoM KPI snapshot | Invoices / `getPosSalesReport.php` |
| Sales list | `/reports/sales-list` | Period bills + lines | `Invoices`, `InvoiceItems` |
| Invoices | `/reports/invoices` | Full invoice report + CSV | Filtered `Invoices` |
| Payment mode | `/reports/payment-mode` | Breakdown by payment | Operational report |
| Sale (POS) | `/reports/sale` | Fast-billing invoices | Type filter: POS |
| Table | `/reports/table` | Dine-in invoices | Type filter: table |
| Table list | `/reports/table-list?table=` | Bills for one table | `PosTables` + invoices |
| Takeaway | `/reports/takeaway` | Takeaway invoices | Type filter: takeaway |
| Discount | `/reports/discount` | Discounted bills | Discount > 0 |
| Mess invoices | `/reports/mess` | Mess invoices | Type filter: mess |
| Refund | `/reports/refund` | Refunded bills | Refunded only |
| Product-wise | `/reports/products?type=` | Product/combo ranking | `InvoiceItems` |
| Staff-wise | `/reports/staff-wise` | Sales by staff | Invoice staff fields |
| Expense report | `/reports/expense` | Expenses by period | `ShopExpenses` |
| Mess members | `/reports/mess-members` | Member directory | `MessMembers` |
| Mess payments | `/reports/mess-payments` | All mess payments | `MessMemberPayments` |
| Invoice detail | `/reports/invoice/:id` | Detail / refund / reprint | Invoice + items |
| Edit invoice | `/reports/invoice/:id/edit` | Edit header/lines | Drift + `deleteInvoiceProduct.php` |
| Add products | `/reports/invoice/:id/add-products` | Add lines to bill | Catalog + `InvoiceItems` |

Period filters use `reportPeriodProvider` / `periodInvoicesProvider`. Web may hydrate via `getInvoiceList.php` / `getInvoiceProductList.php`.

### Settings

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Settings hub | `/settings` | Grouped launcher, sync, logout, language | Permissions |
| Company | `/settings/company` | Shop profile, GST, logo | `Companies`; company APIs |
| Devices / printer prefs | `/settings/devices` | Primary bill/KOT printer + receipt text | Prefs + printer setting APIs |
| Business hours | `/settings/business-hours` | Open / close times | Prefs + company API |
| Payment display | `/settings/payment-display` | Customer display pairing (Android) | Prefs |
| About | `/settings/about` | App version, links | Static |
| Share app | `/settings/share` | Play Store invite | `share_plus` |
| Change PIN | `/settings/change-pin` | Change PB-PIN | `updateMPin.php` |

### Print & devices

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Printers | `/settings/printers` | Extra store printers | Store printer APIs |
| Printer form | `/settings/printers/add\|edit` | Add/edit printer | insert/update store printer |
| Printer routing | `/settings/printer-routing` | Food/bev → printer map | Route list / save |
| Print queue | `/settings/print-queue` | Cloud print jobs | Job list / retry |
| POS devices | `/settings/pos-devices` | Registered devices | Device list / revoke |
| Bill print preview | `/print/bill/:invoiceId?duplicate=` | Real bill preview/print | Invoice + items |
| Test print | `/settings/test-print?mode=` | Sample invoice / KOT / mess slips | Print service + slip builder |
| Device picker | *(pushed)* | BT / USB / network discovery | Hardware |

`mode` for test print: `invoice`, `kot`, `mess-qr`, `mess-coupon`, `mess-common-qr`.

### Staff

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Staff list | `/settings/users` | Staff directory | `getStaffList.php` |
| Staff form | `/settings/users/add`, `…/:id/edit` | Profile + permissions | insert/update staff, role defaults |
| Staff detail | `/settings/users/:id` | Profile, PIN, role, deactivate | `getStaff.php` |
| Salary | `/settings/salary` | Monthly salary sheet | salary list / payment APIs |

### Sync / notifications / support

| Screen | Route | Data | Sources |
|--------|-------|------|---------|
| Sync | `/sync?mode=fetch\|sync` | Full fetch or upload progress | `fullSyncControllerProvider` |
| Fetch result | `/sync/fetch-result` | Post-fetch row counts | `FetchLocalCounts` |
| Notifications | `/notifications` | Inbox + pending meal tokens | In-app store, FCM queue |
| Support hub | `/support` | Help entry | Online check |
| Create ticket | `/support/create` | Ticket + screenshot | `createSupportTicket.php` |
| Tickets | `/support/tickets` | Ticket list | `getSupportTickets.php` |
| Ticket detail | `/support/:ticketId` | Thread + reply | details / reply APIs |

---

## Local database (Drift)

| Table | Purpose |
|-------|---------|
| `FoodTypes` | Food-type master |
| `ProductCategories` | Categories |
| `ProductSubcategories` | Subcategories |
| `Products` | Sellable products (price, GST, image) |
| `ProductPortions` | Per-product portion prices |
| `PortionMasters` | Reusable portion names |
| `Combos` / `ComboItems` | Combo headers + components |
| `CartItems` / `CartComboItems` | Scoped cart (POS / table / parcel) |
| `Invoices` / `InvoiceItems` / `InvoiceComboItems` | Local bills |
| `InvoiceProductDeleteQueue` | Pending cloud line deletes |
| `PosTables` / `DiningAreas` / `TableTypes` | Floor masters |
| `DiningSessions` / `OrderRounds` | Open dine-in + rounds |
| `Kots` / `KotItems` | Kitchen tickets |
| `MessMembers` / `MessMemberPayments` | Mess members + payments |
| `MessTokens` / `MessInvoices` | QR tokens + paper coupons |
| `MessMealTokenQueue` | FCM / print queue for meal tokens |
| `Companies` / `CompanyPrinterSettings` | Shop profile + printer settings |
| `InventoryMovements` | Stock in/out |
| `ShopExpenses` | Expenses |

Operational tables are branch-scoped (`organizationId`, `branchId`, `deviceId`).

---

## API (summary)

Base: `ApiConstants.baseUrl` → `https://posbillingwala.com/androidApp/`  
Media: `ApiConstants.mediaBaseUrl` → `https://posbillingwala.com/storage/app/`

| Area | Endpoints (examples) |
|------|----------------------|
| Auth | `Login.php`, `LoginMpin.php`, `registerTrial.php`, `refreshAuthToken.php` |
| Catalog | `get/insert` Category, Subcategory, Product, Portion, Combo |
| Company / home | `getCompanyList`, printer setting, `getHomeSalesOverview`, `getHomeBannerList` |
| Tables | Dining area/type/table + `insertDiningSession` |
| Invoices | `insertInvoice`, products, `getInvoiceList`, `getPosSalesReport` |
| Inventory | `insert/getInventory`, `insert/getExpenses` |
| Mess | Members, payments, invoices, tokens, QR, meal sessions/tokens |
| Staff / devices | Staff CRUD, salary, POS devices, store printers, routes, print jobs |
| Support | Tickets create / list / reply |

Full list: `lib/core/constants/api_constants.dart`.

---

## Web shell navigation

On desktop/web (`WebAppShell`): Home · Billing · Tables · Takeaway · Mess · Masters · Inventory · Reports · Settings.

---

## Project layout

```
posbillingwala-flutter/
├── lib/
│   ├── app/           # router, app bootstrap
│   ├── core/          # DB, network, widgets, utils
│   ├── features/      # feature modules (screens + domain)
│   └── language/      # localization
├── assets/            # images, fonts, locale, payment display
├── android/ ios/ web/
└── pubspec.yaml
```
