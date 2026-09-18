# Billingwala POS (Flutter)

Point-of-sale app for restaurants, mess, and retail billing. Runs on **Android**, **iOS**, and **Web
** (`pos_billingwala_v2` · v2.0.1).

Licence modules control which billing modes appear: **Fast billing**, **Dine-in**, **Takeaway**, and
**Mess**. Staff roles further gate screens (for example `billing.create`, `table.view`,
`report.view`).

Languages: English, Hindi, Marathi.

---

## Run

Interactive (clean → pub get → Chrome or Android):

```powershell
# Windows PowerShell
cd posbillingwala-flutter
.\run_dev.ps1
```

```bash
# macOS / Linux / Git Bash
cd posbillingwala-flutter
chmod +x run_dev.sh   # once
./run_dev.sh
```

Manual:

```bash
cd posbillingwala-flutter
flutter pub get
flutter run
```

Optional API override:

```bash
flutter run --dart-define=API_BASE_URL=https://posbillingwala.com/androidApp/
```

Web uses a left nav (Home, Billing, Tables, Takeaway, Mess, Masters, Inventory, Reports, Settings).
Phone uses the home dashboard plus Settings.

---

## Screen map

```
Splash
  → Login (staff PIN or licence key)
      → Register trial
      → PB-PIN (owner)
      → Staff login (when user management is on)
          → Home
              → Fast billing → POS → Payment → Bill print
              → Dine-in → Tables → POS / Split bill → Payment
              → Takeaway → Parcel list → POS → Payment
              → Mess → members / tokens / QR / scan / payments
              → Reports (PIN) → dashboards, invoices, product/expense reports
              → Settings → shop, printers, users, inventory, sync, support
```

---

## 1. Auth

| Screen          | Route          | What it does                                                                                                                                        |
|-----------------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Splash**      | `/splash`      | Branding, Play Store update check (Android), then session bootstrap. Web stays here if offline.                                                     |
| **Login**       | `/login`       | Two modes: **Staff** (mobile + PIN) or **Licence** (licence key). Forgot-licence sheet. Device-conflict dialog if another device holds the licence. |
| **Register**    | `/register`    | Trial signup (name, contact, shop, address). Shows licence key, PB-PIN, and report PIN, then logs in.                                               |
| **PB-PIN**      | `/mpin`        | 4-digit owner PIN after a valid licence. Unlocks the shop on this device.                                                                           |
| **Staff login** | `/staff-login` | Extra PIN screen when **user management** is enabled. Licence stays on the device; staff switch here.                                               |

Unauthenticated users land on Login. After licence login, the app requires PB-PIN. Authenticated
users cannot return to auth screens without logout.

---

## 2. Home

**Route:** `/` · **Screen:** Home dashboard

Greeting, shop name, live clock, printer chip (Connected / USB / Network / Offline), business hours,
unread notification badge.

**KPIs**

- Primary sales (today or month) with hide/show
- Today’s sales
- Catalog counts: categories, subcategories, products, combos

**Billing tiles** (licence + permission)

| Tile         | Goes to              | Permission       |
|--------------|----------------------|------------------|
| Fast billing | `/pos`               | `billing.create` |
| Dine-in      | `/tables`            | `table.view`     |
| Takeaway     | `/takeaway`          | `takeaway.view`  |
| Mess         | `/mess` (report PIN) | `mess.view`      |

Header shortcuts: **Notifications**, **Settings**, **Printer details**. Pull-to-refresh syncs
catalog and sales. Opening Fast billing clears the cart and starts a POS session.

---

## 3. Fast billing (POS)

| Screen          | Route             | What it does                                                                                                                                                               |
|-----------------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **POS**         | `/pos`            | Menu by category/subcategory. Search, add items, portions, qty, cart. Send **KOT** (preview or auto-print). Checkout opens Payment. Opening this route resets the session. |
| **KOT preview** | (pushed from POS) | Preview kitchen ticket, then print via routed printers.                                                                                                                    |
| **Payment**     | `/pos/payment`    | Bill summary, discount, GST, payment mode (cash / UPI / card / split), cash tendered. Saves invoice, prints bill, syncs when online. Needs `bill.create`.                  |

Same POS and Payment screens are reused for dine-in (`/tables/billing`, `/tables/payment`) and
takeaway (`/takeaway/billing`, `/takeaway/payment`) without resetting the table/parcel session.

---

## 4. Dine-in

| Screen            | Route                                  | What it does                                                                                                       |
|-------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| **Tables**        | `/tables`                              | Floor plan by dining area. Occupied / vacant / billed. Open a table to order, move, or settle. Needs `table.view`. |
| **Table billing** | `/tables/billing`                      | POS cart attached to the open table session. KOT per table.                                                        |
| **Table payment** | `/tables/payment`                      | Settle the table bill.                                                                                             |
| **Split bill**    | `/tables/split-bill?table=&sessionId=` | Split an open bill **equally**, **by item**, or **by amount**.                                                     |

---

## 5. Takeaway

| Screen             | Route                      | What it does                                                                                               |
|--------------------|----------------------------|------------------------------------------------------------------------------------------------------------|
| **Takeaway**       | `/takeaway`                | Open parcels waiting for billing. New parcel with optional customer name and phone. Needs `takeaway.view`. |
| **Parcel billing** | `/takeaway/billing?cart=1` | POS for that parcel. `cart=1` opens the cart immediately.                                                  |
| **Parcel payment** | `/takeaway/payment`        | Collect payment and print.                                                                                 |

---

## 6. Mess

Needs `mess.view`. Home opens Mess behind the **report PIN**.

| Screen                | Route                     | What it does                                                                                   |
|-----------------------|---------------------------|------------------------------------------------------------------------------------------------|
| **Mess**              | `/mess`                   | Three tabs: **Members** (add/edit, coupons, QR), **Tokens** (walk-in + verify), **Common QR**. |
| **Meal sessions**     | `/mess/meal-sessions`     | Breakfast / lunch / dinner windows for token issue.                                            |
| **Today’s tokens**    | `/mess/meal-tokens-today` | Tokens issued today; reprint / status.                                                         |
| **Member payments**   | `/mess/payments`          | Collect or list payments for a member (pass member in `extra`).                                |
| **Scan token**        | `/mess/scan`              | Camera scan of a member or walk-in QR.                                                         |
| **Coupon / token QR** | (pushed from Mess)        | Show, save, or share a member coupon / token QR.                                               |

---

## 7. Masters (catalog)

**Hub:** `/masters` — Categories, Subcategories, Portions, Products, Combos, Table master. Needs
`product.view`.

| Screen               | Route                            | What it does                                             |
|----------------------|----------------------------------|----------------------------------------------------------|
| **Catalog tabs**     | `/masters/catalog?tab=combos`    | Products / combos list (home shortcut).                  |
| **Categories**       | `/masters/categories`            | Food groups (Veg, Non-veg, …). Add / edit / delete.      |
| **Subcategories**    | `/masters/subcategories`         | Starter, Main course, Beverage, …                        |
| **Portion masters**  | `/masters/portion-masters`       | Half, Full, and other sizes.                             |
| **Products**         | `/masters/products`              | Menu list with search and status.                        |
| **Product form**     | `/masters/products/form?id=`     | Create or edit name, price, GST, image, category, stock. |
| **Product portions** | `/masters/products/portions?id=` | Map portion sizes and prices to a product.               |
| **Combos**           | `/masters/combos`                | Combo meals.                                             |
| **Combo form**       | `/masters/combos/form?id=`       | Combo items, price, and validity.                        |
| **Table master**     | `/masters/tables`                | Dining areas, table types, tables and seats.             |

---

## 8. Inventory and expenses

| Screen           | Route                                             | What it does                                                                            |
|------------------|---------------------------------------------------|-----------------------------------------------------------------------------------------|
| **Inventory**    | `/inventory` · `?tab=expenses`                    | **Stock** tab: ledger of purchases and waste. **Expenses** tab. Needs `inventory.view`. |
| **Add purchase** | `/inventory/add`                                  | Stock in (purchase). GST and supplier fields.                                           |
| **Add waste**    | `/inventory/add?mode=waste` or `/inventory/waste` | Stock out / wastage.                                                                    |
| **Expenses**     | `/expenses`                                       | Shop expense list. Needs `expense.view`.                                                |
| **Add expense**  | `/expenses/add`                                   | Amount, category, note, date.                                                           |

---

## 9. Reports

**Hub:** `/reports` — opened with **report PIN** (default `9082` if unset). Needs `report.view`.

Licence flags hide dine-in / takeaway / mess / fast-billing reports the shop did not buy. User-wise
sales appears only when user management is on.

### Sales and analytics

| Screen              | Route                 | What it does               |
|---------------------|-----------------------|----------------------------|
| **Sales dashboard** | `/reports/dashboard`  | Charts for this branch.    |
| **Sales overview**  | `/reports/overview`   | Period totals vs previous. |
| **Sales list**      | `/reports/sales-list` | Bills and line items.      |
| **User-wise sales** | `/reports/staff-wise` | Sales by staff user.       |

### Operational reports

| Screen                | Route                        | What it does                          |
|-----------------------|------------------------------|---------------------------------------|
| **Invoice report**    | `/reports/invoices`          | All invoices with filters.            |
| **Sale-wise**         | `/reports/sale`              | Fast-billing invoices.                |
| **Table invoices**    | `/reports/table`             | Dine-in invoices.                     |
| **Table list**        | `/reports/table-list?table=` | Bills for one table.                  |
| **Takeaway invoices** | `/reports/takeaway`          | Parcel invoices.                      |
| **Payment mode**      | `/reports/payment-mode`      | Cash / UPI / card split.              |
| **Discount-wise**     | `/reports/discount`          | Discounted bills.                     |
| **Refund-wise**       | `/reports/refund`            | Refunded bills.                       |
| **Product-wise**      | `/reports/products`          | Items sold. `?type=combo` for combos. |
| **Expense-wise**      | `/reports/expense`           | Expenses in a date range.             |
| **Mess invoices**     | `/reports/mess`              | Mess billing.                         |
| **Mess members**      | `/reports/mess-members`      | Member report.                        |
| **Mess payments**     | `/reports/mess-payments`     | Member payment report.                |

### Invoice actions

| Screen                 | Route                              | What it does                                         |
|------------------------|------------------------------------|------------------------------------------------------|
| **Invoice detail**     | `/reports/invoice/:invoiceId`      | Full bill, reprint, edit entry.                      |
| **Edit invoice**       | `/reports/invoice/:invoiceId/edit` | Change lines / payment on a saved bill.              |
| **Bill print preview** | `/print/bill/:invoiceId`           | Preview and print. `?duplicate=1` marks a duplicate. |

The hub also has **Delete all invoices** (blocked if unsynced bills are pending).

---

## 10. Settings

**Hub:** `/settings` — groups for billing, store, cloud, and account. Tiles respect staff
permissions.

### Store

| Screen              | Route                      | What it does                                                                                      |
|---------------------|----------------------------|---------------------------------------------------------------------------------------------------|
| **Shop details**    | `/settings/company`        | Shop profile, GST, address, logo (cloud company). Needs `settings.view`.                          |
| **Printer details** | `/settings/devices`        | Bill / KOT Bluetooth, USB, or network; paper size; bill format; test print. Needs `printer.view`. |
| **Business hours**  | `/settings/business-hours` | Open / close times shown on Home.                                                                 |
| **Users**           | `/settings/users`          | Staff list (user management). Needs `user.view`.                                                  |
| **Add user**        | `/settings/users/add`      | Create staff + role.                                                                              |
| **User detail**     | `/settings/users/:id`      | Profile, role, deactivate, reset PIN.                                                             |
| **Edit user**       | `/settings/users/:id/edit` | Update staff.                                                                                     |
| **Salary**          | `/settings/salary`         | Monthly salary and payments.                                                                      |
| **POS devices**     | `/settings/pos-devices`    | Authorized devices for this store. Needs `device.view`.                                           |

### Printers (multi-printer)

| Screen              | Route                                    | What it does                                          |
|---------------------|------------------------------------------|-------------------------------------------------------|
| **Printers**        | `/settings/printers`                     | Store printers (Bluetooth / USB / network).           |
| **Add printer**     | `/settings/printers/add`                 | Register a printer.                                   |
| **Edit printer**    | `/settings/printers/edit`                | Update an existing printer (`extra`: `StorePrinter`). |
| **Printer routing** | `/settings/printer-routing`              | Route kitchen / bar / bill jobs to printers.          |
| **Print queue**     | `/settings/print-queue`                  | Pending / failed jobs; retry.                         |
| **Test print**      | `/settings/test-print?mode=invoice\|kot` | Sample invoice or KOT preview.                        |

### Cloud and app

| Screen               | Route                    | What it does                                                                   |
|----------------------|--------------------------|--------------------------------------------------------------------------------|
| **Sync**             | `/sync?mode=sync\|fetch` | `sync` = upload offline bills (mobile). `fetch` = download catalog from cloud. |
| **Fetch result**     | `/sync/fetch-result`     | Counts after a cloud fetch.                                                    |
| **About**            | `/settings/about`        | Version, developer, website.                                                   |
| **Share app**        | `/settings/share`        | Play Store invite (not on web).                                                |
| **Change PB-PIN**    | `/settings/change-pin`   | New 4-digit owner PIN.                                                         |
| **Language**         | (dialog on hub)          | English / हिंदी / मराठी.                                                       |
| **Update / Rate us** | (hub actions)            | Play in-app update / store listing (Android).                                  |
| **Logout**           | (hub action)             | Lock to PB-PIN, or **Switch user** when user management is on.                 |

---

## 11. Support

| Screen             | Route                | What it does                                |
|--------------------|----------------------|---------------------------------------------|
| **Help & Support** | `/support`           | Contact, hours, create ticket, ticket list. |
| **Create ticket**  | `/support/create`    | Subject, message, screenshot.               |
| **Tickets**        | `/support/tickets`   | Open / closed tickets.                      |
| **Ticket detail**  | `/support/:ticketId` | Conversation and status.                    |

---

## 12. Notifications

**Route:** `/notifications`

In-app list (FCM + local). Tapping a row deep-links (for example pending mess tokens). Home badge
shows unread count.

---

## Permissions (staff)

When user management is on, these prefixes redirect to Home if the role lacks the permission:

| Path prefix                           | Permission                       |
|---------------------------------------|----------------------------------|
| `/pos`, `/pos/payment`                | `billing.create` / `bill.create` |
| `/tables`                             | `table.view`                     |
| `/takeaway`                           | `takeaway.view`                  |
| `/mess`                               | `mess.view`                      |
| `/reports`                            | `report.view`                    |
| `/inventory`                          | `inventory.view`                 |
| `/expenses`                           | `expense.view`                   |
| `/masters`                            | `product.view`                   |
| `/settings/users`, `/settings/salary` | `user.view`                      |
| `/settings/printers`                  | `printer.view`                   |
| `/settings/pos-devices`               | `device.view`                    |

---

## Project layout

```
lib/
  app/           router, theme
  core/          API, Drift DB, widgets, logging
  features/      one folder per module (auth, pos, tables, mess, …)
  language/      locale strings
assets/locale/   en.json, hi.json, mr.json
```

Support: [info@posbillingwala.com](mailto:info@posbillingwala.com) · +91 89831 49299 · Mon–Sat 10:
00–19:00 IST
