# POS Billingwala — Codebase Audit

**Audit date:** 2026-08-23  
**Scope:** Existing LIVE production ecosystem (read-only inspection; no production functionality changed)  
**Primary POS app:** `WithTable` — `com.pos_billingwala` — versionName **2.0.51** / versionCode **67**  
**Auditor note:** This document describes the **current** production system. Target architecture (Food Type → Subcategory → Product → Portion, Crashlytics, 7-day/50-bill trial, etc.) is identified as **gaps**, not as already implemented.

---

## 1. Workspace inventory

| Path | Role |
|------|------|
| `WithTable/` | Native Android POS (Java) — customer-facing live billing app |
| `Owner/` | Owner Android app — invoices, products, sale visibility (`com.posbillingwala.owner` 1.0.6) |
| `Dealer/` | Dealer Android app — customer + license registration (`com.posbillingwala.dealer` 1.0.10) |
| `Admin/` | Admin Android app — dealers + customers (`com.posbillingwala.admin` 1.0) |
| `API/` | PHP REST backend (POS root + Owner/Dealer/Admin subfolders) |
| `API/schema/schema_reference.sql` | Schema-only reference (no production data; P5-5) |
| `API/migrations/` | Additive SQL migrations |

There is **no** Repository / ViewModel / Room layer. Architecture is classic Activity/Fragment → SQLite helper + Retrofit.

---

## 2. Current architecture

### 2.1 Pattern

```
SplashScreen → Login / LoginMPin → MainActivity (hosts Fragments)
                      ↓
              SharedPreferences ("user")  ← session, licence dates, flags
                      ↓
         POSBillingWalaDatabase (SQLiteOpenHelper)  ← offline store of record
                      ↓
              Retrofit (Api / ApiInterface)  ← cloud sync
                      ↓
         WorkManager (online→offline pull) + BroadcastReceivers (offline→online push)
                      ↓
         Bluetooth print stack (Woosim / SPP)
```

### 2.2 Architecture characteristics

| Aspect | Current state |
|--------|----------------|
| Language | Java 17 |
| UI | Activities + Fragments + ViewBinding |
| Local DB | SQLite via `SQLiteOpenHelper` (`DATABASE_VERSION = 9`) |
| Network | Retrofit 3 + OkHttp 5 + Gson |
| Background | WorkManager (download chain); connectivity receivers (upload) |
| DI / Repository | None |
| Crash monitoring | Firebase Analytics only — **no Crashlytics** |
| ANR monitoring | None |
| Offline-first billing | Yes — bills always write to SQLite first |
| Catalog hierarchy | **Category → Product only** (no Food Type / Subcategory / Portion entities) |

### 2.3 Package map (WithTable)

| Package | Responsibility |
|---------|----------------|
| `Activity` | Splash, Login, LoginMPin, MainActivity, Bluetooth print screens, printer settings |
| `Fragment` | Home, billing (CreatePos, tables, takeaway), masters, reports, mess, settings |
| `Adapter` | Cart, products, categories, invoices, print layouts, tables |
| `Database` | `POSBillingWalaDatabase` — schema + all CRUD + sync flag updates |
| `Retrofit` | `Api`, `ApiInterface` |
| `NetworkToOffline` | Pull/push sync, receivers, WorkManager workers |
| `WorkerClass` | Local UI workers (cart/products/categories) |
| `Print` | Bluetooth service, Woosim managers, device list |
| `Model` | Gson response POJOs |
| `Extra` | `Common` SharedPreferences helpers |
| `Utils` | Display, Excel export, request codes |
| `Interface` | Click listeners |

### 2.4 Activities

| Activity | Role |
|----------|------|
| `SplashScreen` | Launcher; Play in-app update; route Login vs MPIN |
| `Login` | Licence key + device id login |
| `LoginMPin` | MPIN unlock / device bind |
| `MainActivity` | Host + static session fields |
| `BluetoothPrint` | Checkout, save invoice, print bill/KOT |
| `DuplicateBluetoothPrint` | Reprint |
| `InvoiceDetailsBluetoothPrint` | Invoice detail print |
| `CouponBluetoothPrint` | Coupon print |
| `ProductListBluetoothPrint` | Product list print |
| `CompanyPrinterSetting` | Printer / invoice prefix settings |
| `Print.DeviceListActivity` | Bluetooth device picker |

### 2.5 Key Fragments (billing / ops)

| Fragment | Role |
|----------|------|
| `Home` | Dashboard; licence days; fetch-from-cloud; receiver registration |
| `CreatePos` | Product pick + cart (fast / takeaway / table) |
| `InvoiceTakeAway` | Open takeaway carts |
| `InvoiceCompanyTable` | Dine-in table grid |
| `OrderInvoice` | Local invoice history (not create) |
| `ProductMaster` / `AddProduct` / `UpdateProduct` / `AddCategory` | Catalog |
| `CompanyDetailSetting` | Shop/company |
| `Inventory` / `AddInventory` | Stock |
| `Expenses` / `AddExpenses` | Expenses |
| Mess set (`InvoiceMess`, `MessMemberList`, …) | Mess module |
| Report set (`InvoiceReport`, `SaleReport`, …) | Reports |
| `UserSetting` | Sync upload/download, logout, MPIN |

### 2.6 Services / Receivers / Workers

**Services**

- `BluetoothPrintService` — Bluetooth SPP connect/write threads  
- No dedicated sync `Service`; uploads run from receivers / helper classes

**Receivers**

| Receiver | Trigger | Behavior |
|----------|---------|----------|
| `OfflineToNetworkReceiver` | `CONNECTIVITY_CHANGE` (manifest + Home) | Push unsynced rows |
| `LicenceKeyReceiver` | Same | Re-check licence expiry |

**WorkManager workers (online → offline)**

`CategoryWorker` → `ProductWorker` → `CompanyWorker` → `CompanyPrinterWorker` → `InvoiceWorker` → `InvoiceProductWorker` → `MessMemberWorker` → `MessInvoiceWorker` → `MessMemberPaymentWorker` → `InventoryWorker` → `ExpensesWorker`

**Local UI workers**

`CartProductWorker`, `HomeProductsWorker`, `HomeAllProductsWorker`, `ProductCategoryWorker`

### 2.7 Repositories

**None.** Fragments/Activities call `POSBillingWalaDatabase` and Retrofit directly.

---

## 3. Database schema

### 3.1 Local SQLite (`pos_billingwala_db`, version 9)

| Table | Purpose |
|-------|---------|
| `product_category` | Categories |
| `product` | Products (single price + unit) |
| `cart_product` | Open carts by table / order type |
| `invoice` | Saved bills |
| `invoice_final_product` | Bill line items (name/price snapshot) |
| `company_printer_setting` | Printer + invoice UI flags + BT MACs |
| `company` | Shop details, GST, tables, logos |
| `inventory` | Stock movements |
| `expenses` | Expenses |
| `member` | Mess members |
| `member_payment` | Mess payments |
| `mess_invoice` | Mess attendance/invoices |

#### Local column highlights

**`product_category`:** `categoryId`, `categoryName`, `categoryDeletedStatus`, `categoryNetworkStatus`, `categoryStatus`

**`product`:** `productId`, `userId`, `categoryId`, `categoryName`, `productCode`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productWithGSTPrice`, `productDeletedStatus`, `productNetworkStatus`, `productStatus`

**`invoice`:** `invoiceId`, `userId`, `noOfTable`, `invoiceNumber`, `customer*`, `invoiceDate`, `subTotal`, `totalGSTAmount`, `discount`, `discountType`, `totalAmount`, `paymentMode`, `invoiceOrderStatus`, `invoiceType`, `invoiceNetworkStatus`, `invoiceStatus`

**`invoice_final_product`:** `invoiceProductId`, `invoiceNumber`, `productName`, `productPrice`, `productUnit`, `productCGST`, `productSGST`, `productQuantity`, `productStatus`, `invoiceProductNetworkStatus`, `invoiceProductStatus`

#### Sync flag convention (critical)

| Column | Meaning |
|--------|---------|
| `*NetworkStatus` (VARCHAR) | Client random 10-char idempotency key sent to server |
| `*Status` (TINYINT) | `0` = not synced, `1` = synced |

### 3.2 MySQL central (`spllmgkn_posbill`)

| Table | Notes |
|-------|-------|
| `users` | Shop owners / dealers; `shopName`, `reportPin`, `dealerId` |
| `licenses` | Device binding, expiry, feature flags, franchise |
| `categories` | Per-user catalog |
| `products` | Flat product; `productUnit` text only |
| `companys` | Shop profile keyed by `licenseId` |
| `company_printer_setting` | Printer config |
| `invoice` | Bills keyed by `licenseId` |
| `invoice_final_product` | Line items by `invoiceNumber` |
| `inventory`, `expenses` | Ops data |
| `mess_*` | Mess module |
| `units` | Unit name list |
| Laravel leftovers | `migrations`, `failed_jobs`, `password_resets`, `personal_access_tokens` |

#### `licenses` columns

`id`, `userId`, `licenseKey`, `licenseValidity`, `licenseType`, `android_device_name`, `android_device_id`, `mpin`, `licenseStatus`, `expiryDate`, `paymentStatus`, `amount`, `userType` (`owner`/`franchise`), `userName`, `fastBilling`, `takeAway`, `dineIn`, `mess`, `total_sale_data`, `today_sale_data`, timestamps

### 3.3 Schema gaps vs target architecture

| Target | Present? |
|--------|----------|
| Food Type / Category as first-class type | No (flat `categories` only) |
| Optional Subcategory | **No** |
| Product → Portion (Half/Full/Kg) | **No** (`productUnit` is a single text field; one price per product) |
| Bill line portion snapshot | Partial — stores `productName`/`productPrice`/`productUnit` only; no `portionId`/`portionName` |
| Daily business day entity | Implicit via date string on invoices |
| Trial bill counter (max 50) | **No** |

---

## 4. API structure

### 4.1 Base URLs (P5-4 aligned)

| Client | Base URL |
|--------|----------|
| WithTable POS | `https://www.posbillingwala.com/androidApp/` |
| Owner | `https://www.posbillingwala.com/androidApp/Owner/` |
| Dealer | `https://www.posbillingwala.com/androidApp/Dealer/` |
| Admin | `https://www.posbillingwala.com/androidApp/Admin/` |

All four share one MySQL via `API/db_connection.php`. Media: `https://www.posbillingwala.com/storage/app/`.
URLs come from `BuildConfig.API_BASE_URL` (POS also `MEDIA_BASE_URL`).

### 4.2 POS endpoints (`API/`)

**Auth / licence:** `Login.php`, `LoginMpin.php`, `updateMPin.php`, `updateAndroidKey.php`, `check_licence_expire.php`, `updateLicenceKey.php`, `LogOut.php`

**Catalog / shop:** `getCategoryList`, `insertCategory`, `getProductList`, `insertProduct`, `getCompanyList`, `insertCompanyDetail`, `getCompanyPrinterSetting`, `insertCompanyPrinterSetting`

**Billing / ops:** `getInvoiceList`, `insertInvoice`, `getInvoiceProductList`, `insertInvoiceProduct`, `getInventoryList`, `insertInventory`, `getExpensesList`, `insertExpenses`

**Mess:** `getMessMemberList`, `insertMessMember`, `getMessMemberPaymentList`, `insertMessPayment`, `getMessInvoiceList`, `insertMessInvoice`

### 4.3 Owner / Dealer / Admin

- **Owner:** login by contact, invoice reads, product/category CRUD, sale-data toggles, store-wise views  
- **Dealer:** customer registration, new/renew licence, product/category for customers  
- **Admin:** dealer list + same customer/license/product surface as Dealer  

### 4.4 API security posture (current)

| Issue | Status |
|-------|--------|
| Prepared statements | Not used (string-concat SQL) |
| Auth tokens / JWT | None — `userId` / `licenseId` sufficient |
| CORS | `Access-Control-Allow-Origin: *` on some scripts |
| DB credentials | `API/db_local.php` (gitignored) or env vars — see `docs/DEPLOY_DB.md` |
| Invoice dedup | `insertInvoice.php` upserts by `licenseId` + `invoiceNetworkStatus` (good) |
| Expiry check | `check_licence_expire.php` uses `licenseStatus='active'` + device match — **not** live `expiryDate >= today` |

---

## 5. Authentication

1. User enters **licence key** → `Login.php` with `android_device_id`  
2. Device bind / conflict handled via `updateAndroidKey.php` / `LoginMpin.php`  
3. Session stored in SharedPreferences (`userId`, `LicenceKey`, dates, feature flags, MPIN flow)  
4. Subsequent opens use **MPIN** (`LoginMPin`) when `firstLogin` set  
5. Periodic re-validation via `LicenceKeyReceiver` → `check_licence_expire.php`  
6. Logout clears device binding server-side (`LogOut.php`) and local prefs  

**One licence per device:** enforced by `android_device_id` on `licenses` (null = unbound; mismatch = already logged in elsewhere).

---

## 6. Billing flow

```
Home
 ├─ Fast billing → CreatePos (FS###, cartOrderStatus=fast_billing)
 ├─ Take Away   → InvoiceTakeAway → CreatePos (TA###, take_away)
 └─ Dine-in     → InvoiceCompanyTable → CreatePos (table #, table_wise)
        ↓
   Cart (cart_product) — qty/price edits via CartAdapter
        ↓
   BluetoothPrint (checkout UI)
        ↓
   saveInvoice():
     - optional inventory rows (status 0)
     - generate invoiceNumber (today COUNT + prefix)
     - insert invoice (invoiceStatus=0, random invoiceNetworkStatus)
     - insert invoice_final_product lines (snapshots of name/price/unit/GST/qty)
     - mark cart completed
        ↓
   Print (optional) + later cloud sync
```

**Rules observed**

- Billing does **not** wait on server success to complete locally (offline-first — good).  
- Historical bills keep product name/price/unit snapshots (good baseline for portion snapshots later).  
- Invoice number uses **daily count** + printer `invoicePrefix` (supports daily numbering pattern).  

**Known billing bugs / risks**

- `getInvoiceNumber()` may be called again during print layout → printed number can differ from saved number.  
- Heavy SQLite + bitmap work on UI thread during checkout.  

---

## 7. Offline flow

1. All masters, carts, bills, inventory, expenses, mess data live in SQLite.  
2. Unsynced rows keep `*Status = 0`.  
3. App remains usable without network for billing/printing (if printer paired).  
4. Licence validation and cloud fetch require network.  
5. Splash may trigger upload via `OfflineNetworkData` before Play update.  

---

## 8. Sync flow

### 8.1 Online → Offline (pull)

1. User confirms fetch (`Home` / `UserSetting`).  
2. **`resetTables()` DROP TABLE IF EXISTS all local tables**, then `onCreate`.  
3. `NetworkDataFetcher.fetchAllData()` runs WorkManager chain.  
4. Workers insert cloud rows as already synced (`*Status = 1`).  

**Risks:** Destructive wipe of local-only unsynced data; `InvoiceWorker`/`InvoiceProductWorker` use async `enqueue` then immediately `return Result.success()`; `setLockingEnabled(false)` during fetch.

### 8.2 Offline → Online (push)

Triggered by:

- `OfflineToNetworkReceiver` (manifest + Home — **double registration risk**)  
- `UserSynchronizeData` (manual)  
- `OfflineNetworkData` (splash / settings)

Pattern: query `*Status = 0` → Retrofit `save*` → on HTTP success mark `*Status = 1` (often even when business `status != "1"`).

Server `insertInvoice.php` **does** upsert by `invoiceNetworkStatus` (mitigates duplicates if same key retries). Client still risks:

- Marking failed business responses as synced → silent data loss on server  
- Concurrent upload storms from dual receivers  
- Race with manual sync  

---

## 9. Printing flow

**Bluetooth only** (SPP UUID classic). No USB or Wi‑Fi/network printer implementation found.

```
CompanyPrinterSetting stores bluetoothAddress / bluetoothKOTAddress / feed lines / 2" vs 3"
        ↓
DeviceListActivity (paired + discovery)
        ↓
WoosimPrnMng / KOTWoosimPrnMng + BluetoothPrintService
        ↓
Layout (NestedScrollView) → Bitmap → dither → write bytes
        ↓
Optional ESC/POS cut; paper feed from settings
```

Screens: `BluetoothPrint`, `DuplicateBluetoothPrint`, `InvoiceDetailsBluetoothPrint`, `CouponBluetoothPrint`, `ProductListBluetoothPrint`.

**Risks:** Bitmap conversion on UI thread; busy-wait `.run()` on printer release; printer failures can still throw into UI paths; static Activity leaks in managers.

---

## 10. License flow

```
Dealer/Admin creates user + license (Demo/Regular, validity days)
        ↓
POS Login(licenceKey, android_device_id)
        ↓
Bind device / MPIN
        ↓
Store expiry locally; LicenceKeyReceiver refreshes when online
        ↓
Home.totalLicenceDays() shows remaining days from stored expire date
```

**Current Dealer validity options:** 5 / 30 / 183 / 365 days, Lifetime (mapped ~10958 days).  
**Feature flags:** `fastBilling`, `takeAway`, `dineIn`, `mess`.  
**Franchise:** extra `licenses` rows with `userType=franchise` for same `userId`.

### Target vs current license features

| Target | Current |
|--------|---------|
| 7-day trial | Approx via Demo + short validity (5/30), not fixed 7-day product rule |
| Max 50 trial bills | **Not implemented** |
| 6-month / 1y / 3y / 5y / lifetime | 183≈6m, 365≈1y, Lifetime present; **3y/5y not first-class** |
| Same key upgrade | Dealer `updateCustomerLicenceDetails` path exists |
| One licence per device | Yes via `android_device_id` |
| No hard-coded trial dates in APK | Dates come from server `expiryDate` (good) |
| Signing keys in APK | Not observed for licence crypto (licence is opaque server string) |

**Bypass / soft risks:** Client trusts stored expire date offline; server check is status-flag based, not date arithmetic on every request; logout unbinds device.

---

## 11. Crash / error handling

| Area | Current |
|------|---------|
| Firebase Crashlytics | **Absent** |
| ANR monitoring | **Absent** |
| Analytics | Firebase Analytics present |
| Exception handling | Widespread `printStackTrace` / empty catch |
| Printer errors | Partial try/catch; not guaranteed non-crashing |
| Static leaks | Sync helpers + printer managers hold Activity/Context |

---

## 12. Owner / backend integration

```
Dealer/Admin → users + licenses (+ optional seed categories/products)
      ↓
WithTable POS → licence login → local SQLite + sync invoices up
      ↓
Owner app → shop user login → read invoices JOIN licenses; manage catalog; toggle sale visibility
```

Shared conceptual model: POS `userId` == `licenses.id` (licence id). Owner queries by shop `users.id` through `licenses.userId`.

**Integration risks:** ~~POS vs Owner API host split~~ (P5-4 aligned); ~~no auth tokens~~ (P5-3); SQL injection being hardened (P5-2); ~~production SQL dump in repo~~ (P5-5 removed — rotate credentials if dump was shared).

---

## 13. Production-risk areas (priority ordered)

### P0 — Data loss / duplicate billing / crash / licence bypass

1. **`resetTables()` DROP TABLE** used on “fetch from cloud” — destroys local unsynced bills/carts if user confirms.  
2. **Empty `onUpgrade()`** — version bumps do not migrate; relies on ad-hoc `addColumnIfNotExists` / `upGradeDatabase()` (incomplete vs ALTER list).  
3. **Sync marks failed responses as synced** — bill may never reach server but local thinks synced.  
4. **Double connectivity receivers** — concurrent upload races.  
5. **InvoiceWorker async early success** — incomplete/duplicate local inserts after cloud fetch.  
6. **Printed vs saved invoice number mismatch** (`getInvoiceNumber()` twice).  
7. **Licence check does not enforce `expiryDate` live** — depends on `licenseStatus` maintenance.  
8. **No Crashlytics** — production crashes invisible.  
9. **API SQL injection + no auth** — bill/license tampering possible if endpoints exposed.  

### P1 — Billing / sync / printing / database failure

1. UI-thread SQLite on save/cart/sync constructors.  
2. UI-thread bitmap print — ANR on large bills.  
3. Blind inserts on invoice pull (no unique constraint on `invoiceNetworkStatus` locally).  
4. Printer NPE / busy-wait risks in Woosim managers.  
5. Host split POS vs Owner/Dealer.  
6. OkHttp BODY logging in production builds — secrets in logcat.  

### P2 — Performance / reporting

1. Full-table cursor scans for unsynced rows on main thread.  
2. Report fragments may load large invoice sets.  
3. No FTS / indexed fast product search beyond simple queries.  
4. `largeHeap=true` masks memory pressure from bitmaps.  

### P3 — UI / catalog gaps

1. No Subcategory / Portion model (blocks target food hierarchy).  
2. Beverage as separate Food Type not modeled.  
3. UI modernization not blocking production safety.  

---

## 14. Crash / ANR risks

| Risk | Location / pattern |
|------|--------------------|
| ANR on checkout | `BluetoothPrint.saveInvoice` + layout→bitmap on main thread |
| ANR on cart edit | `CartAdapter` / `CreatePos` SQLite on main thread |
| ANR on sync | `OfflineToNetworkReceiver.onReceive` loops cursors on main thread |
| Crash on printer | `KOTWoosimPrnMng` process before null check; static service teardown |
| Crash after rotate | Static dialogs / Activity refs in sync + print |
| Crash on licence cast | `LicenceKeyReceiver` may assume Activity context |
| Busy-wait | `WoosimPrnMng.releaseAllocations` uses `.run()` spin |

---

## 15. Performance risks

- No Room / paging for invoices  
- Product search not optimized for large catalogs  
- WorkManager chain + full wipe is heavy  
- Print path allocates large Bitmaps  
- Multidex + many libraries; minify disabled on release  

---

## 16. Database migration risks

| Risk | Detail |
|------|--------|
| `onUpgrade` empty | Schema evolution unsafe for version bumps |
| `resetTables` DROP | Destructive; violates “never DROP for production migration” spirit when used as sync tool |
| ALTER list incomplete | Many ALTER_* constants exist; `upGradeDatabase()` only applies feed-line columns |
| No UNIQUE indexes | Local `invoiceNetworkStatus` / `invoiceNumber` not unique |
| Server/client column drift | Some Dealer PHP vs dump naming inconsistencies |
| Future Portion schema | Must be **additive** (`CREATE TABLE IF NOT EXISTS`, `ALTER ADD COLUMN`) — never DROP production tables |

---

## 17. High-risk files

| File | Why |
|------|-----|
| `WithTable/.../Database/POSBillingWalaDatabase.java` | All schema, CRUD, DROP reset, sync flags |
| `WithTable/.../Activity/BluetoothPrint.java` | Bill save + print + invoice numbering |
| `WithTable/.../NetworkToOffline/OfflineToNetworkReceiver.java` | Auto upload |
| `WithTable/.../NetworkToOffline/OfflineNetworkData.java` | Upload + false synced |
| `WithTable/.../NetworkToOffline/UserSynchronizeData.java` | Manual upload |
| `WithTable/.../NetworkToOffline/NetworkDataFetcher.java` | Full wipe + pull chain |
| `WithTable/.../NetworkToOffline/WorkerClass/InvoiceWorker.java` | Async worker bug |
| `WithTable/.../NetworkToOffline/WorkerClass/InvoiceProductWorker.java` | Same |
| `WithTable/.../Fragment/Home.java` | resetTables + dual receivers |
| `WithTable/.../Fragment/UserSetting.java` | resetTables + logout + sync |
| `WithTable/.../Activity/Login.java` / `LoginMPin.java` | Auth + device bind |
| `WithTable/.../NetworkToOffline/Receiver/LicenceKeyReceiver.java` | Licence gate |
| `API/insertInvoice.php` | Server bill upsert |
| `API/Login.php` / `check_licence_expire.php` / `updateAndroidKey.php` | Licence security |
| `API/config.php` (+ Owner/Dealer/Admin) | DB credentials |
| `WithTable/.../Print/BluetoothPrintService.java` | Print I/O |
| `WithTable/.../Print/WoosimPrnMng.java` / `KOTWoosimPrnMng.java` | Printer lifecycle |

---

## 18. Medium-risk files

| File | Why |
|------|-----|
| `Fragment/CreatePos.java` | Cart + billing entry |
| `Adapter/CartAdapter.java` | Qty/price mutations |
| `Fragment/InvoiceTakeAway.java` / `InvoiceCompanyTable.java` | Order routing |
| `Activity/DuplicateBluetoothPrint.java` (+ other print Activities) | Reprint paths |
| `Activity/CompanyPrinterSetting.java` | BT addresses / prefixes |
| `Fragment/ProductMaster.java` / `AddProduct.java` / `UpdateProduct.java` / `AddCategory.java` | Catalog integrity |
| `Fragment/Inventory.java` / `AddInventory.java` | Stock |
| `NetworkToOffline/WorkerClass/*` (non-invoice) | Pull workers |
| `Retrofit/Api.java` / `ApiInterface.java` | Network surface |
| `Activity/MainActivity.java` | Static session state |
| `Activity/SplashScreen.java` | Startup sync/update |
| Dealer/Owner licence registration fragments | Licence issuance |
| `API/insertInvoiceProduct.php` | Line sync |
| `API/Dealer/insertNewLicence.php` / `updateCustomerLicenceDetails.php` | Licence mutations |

---

## 19. Low-risk files

| Area | Examples |
|------|----------|
| Report UI | `InvoiceReport`, `SaleReport`, payment/table/takeaway report fragments |
| Mess UI (non-sync core) | Member list adapters (still touch DB — change carefully) |
| About / cosmetic | `AboutUs`, layout polish, AdMob layouts |
| Adapters display-only | Many report adapters |
| Utils | `DisplayUtils`, calendar helpers |
| SearchableSpinner / CalenderView | UI widgets |
| Owner profile/about screens | Non-billing |

---

## 20. Gap analysis vs stated production targets

| Target capability | Status |
|-------------------|--------|
| Offline billing | Present |
| Online synchronization | Present (needs hardening) |
| Crash monitoring | **Missing** |
| ANR monitoring | **Missing** |
| Fast product search | Basic only |
| Fast billing / printing | Present but UI-thread heavy |
| Daily business day / daily bill numbering | Partial (date + prefix count) |
| Previous-day synchronization | Possible via full list sync; not dedicated |
| Data cleanup protection | **Weak** (`resetTables` DROP) |
| 7-day trial + max 50 trial bills | **Missing** |
| License tiers 6m/1y/3y/5y/lifetime | Partial (183/365/lifetime) |
| One licence per device | Present |
| Multi-branch / franchise | Franchise licences present; branch model limited |
| Owner app + central backend + admin | Present |
| Food Type → Subcategory → Product → Portion | **Missing — major schema + billing change** |
| Portion pricing per product | **Missing** |
| Historical portion snapshots on bills | **Missing** (name/price/unit only) |

---

## 21. Recommended implementation posture (for later phases)

Do **not** implement everything next. Follow project specification phases. Suggested safe order after this audit:

1. **P0 hardening (no schema rewrite):** sync success handling, receiver double-registration, Crashlytics, protect/replace `resetTables` DROP usage, invoice number single-source.  
2. **P1:** move bill save / sync query / print bitmap off UI thread; printer try/catch guarantees.  
3. **Additive catalog evolution:** Food Type / optional Subcategory / Portion tables via non-destructive migrations; bill line snapshot columns; keep old category→product working.  
4. **Licence product rules:** 7-day + 50-bill trial, extended validity tiers — server authoritative.  
5. **P2/P3:** search indexes, reports, UI.

Every feature must still: list affected files, DB/API/migration/offline/sync/regression impact, then implement only that feature, compile, and verify.

---

## 22. Audit constraints honored

- No production functionality modified during this audit  
- No destructive migrations performed  
- No fake APIs or fake sync introduced  
- Document created only: `docs/CURSOR_CODEBASE_AUDIT.md`

---

*End of audit.*
