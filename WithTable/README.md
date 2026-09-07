# POS Billingwala — POS app (`WithTable`)

Offline-first Android POS for restaurants and shops: dine-in tables, takeaway, mess + QR tokens, combos, catalog, reports, and Bluetooth receipt printing.

Parent overview: [../README.md](../README.md)

## App identity

| Field | Value |
|-------|--------|
| Module | `WithTable/` |
| Package | `com.pos_billingwala` |
| versionName / versionCode | **2.0.58** / **74** |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |
| API base | `https://posbillingwala.com/androidApp/` |
| Media base | `https://posbillingwala.com/storage/app/` |

Override URLs via `BuildConfig.API_BASE_URL` / `MEDIA_BASE_URL` in `app/build.gradle`.

---

## Screen map (navigation)

```
SplashScreen
  └─ Login → LoginMPin → MainActivity
       └─ Home (hub)
            ├─ Fast / Table / Takeaway / Mess  → billing
            ├─ Catalog shortcuts → Product / Combo / Subcategory
            ├─ Sales cards → Sales Overview / Dashboard
            ├─ Sync / Fetch / Printer status
            └─ Settings (gear) → UserSetting
                 ├─ Invoice Details → OrderInvoice
                 ├─ Reports → ReportsHub → report screens
                 ├─ Master Data → MasterData → catalog screens
                 ├─ Shop / Template / Hours / Printer / Scale
                 ├─ Inventory / Expenses / Support / About
                 └─ Sync / PIN / Staff / Language / Share / Logout
```

Visibility of many rows is **template-gated** (`DynamicUiEngine` / `TabRegistry` / `SettingsRegistry` / business template).

---

## 1. Auth & shell

| Screen | Class | What it does |
|--------|--------|----------------|
| Splash | `SplashScreen` | Launch → route to Login or Main |
| Login | `Login` | Licence key + credentials; issues session |
| Register / trial | `Register` | Trial / registration flow |
| MPIN | `LoginMPin` | App PIN unlock → Bearer token (`api_tokens`) |
| Shell | `MainActivity` | Fragment host; back stack; opens Home / CreatePos / Mess / Settings / Cloud Sync |

---

## 2. Home

| Screen | Class | What it does |
|--------|--------|----------------|
| **Home** | `Home` | Dashboard: sales KPIs, billing mode tiles, catalog shortcuts, sync status, notifications |

**From Home**

| Control | Opens |
|---------|--------|
| Settings (gear) | `UserSetting` |
| Fast billing | `CreatePos` (cart order = FAST) — restaurant module |
| Table billing | `InvoiceCompanyTable` — dine-in |
| Takeaway | `InvoiceTakeAway` |
| Mess | `InvoiceMess` hub (`MessModule.openHub`) |
| Subcategory / Product / Combo cards | `AddSubcategory` / `ProductMaster` / `ComboMaster` |
| Total sales card | `SalesOverview` |
| Today sales card | `SalesDashboard` |
| Fetch data | Cloud → local replace (`NetworkDataFetcher`) |
| Synchronize | Cloud sync status (`CloudSyncNav`) |
| Printer status row | `CompanyPrinterSetting` |

---

## 3. Billing screens

| Screen | Class | What it does |
|--------|--------|----------------|
| Table floor | `InvoiceCompanyTable` | Dine-in tables; open / resume table bill → `CreatePos` |
| Takeaway list | `InvoiceTakeAway` | Takeaway orders → `CreatePos` |
| Mess hub | `InvoiceMess` | Member search + mess actions (QR, tokens, sessions, members) |
| Cart / POS | `CreatePos` | Universal cart: products, portions, combos, discount, payment, save bill, print |
| Edit bill | `EditInvoice` | Amend an existing invoice |
| Invoice product lines | `InvoiceProductDetails` | Line-item detail for a bill |

**Mess sub-screens** (from `InvoiceMess` / mess module)

| Screen | Class | What it does |
|--------|--------|----------------|
| Members | `MessMemberList` | Member roster |
| Add / update member | `AddMessMember` / `UpdateMessMember` | CRUD member |
| Member payment | `AddMemberPayment` / `UpdateMessPayment` | Record / edit payments |
| Payment history | `MessMemberPaymentHistory` | History per member |
| Walk-in QR token | `MessWalkInTokenActivity` | Generate walk-in token |
| Scan / verify | `MessTokenScanActivity` | Scan member QR |
| QR management | `MessQrManagementActivity` | Manage printed QR tokens |
| Today’s meal tokens | `MessMealTokenTodayActivity` | Today’s mess meal tokens |
| Meal sessions | `MessMealSessionsActivity` | Session setup |
| Token print | `MessTokenBluetoothPrint` | Bluetooth print mess QR / token |

---

## 4. Print screens

| Screen | Class | What it does |
|--------|--------|----------------|
| Receipt print | `BluetoothPrint` | Main bill print (Woosim/SPP) |
| Invoice details print | `InvoiceDetailsBluetoothPrint` | Reprint / detail print |
| Duplicate print | `DuplicateBluetoothPrint` | Duplicate copy |
| Product list print | `ProductListBluetoothPrint` | Product / KOT-style list |
| Coupon print | `CouponBluetoothPrint` | Coupon slip |
| Test print | `TestInvoiceBluetoothPrint` | Printer test from settings |
| Device picker | `Print.DeviceListActivity` | Paired Bluetooth devices |

Print failure never clears a saved bill.

---

## 5. Settings (`UserSetting`)

Hub opened from Home gear. Groups below match on-screen sections.

### Operations

| Row | Opens | Class |
|-----|--------|--------|
| Invoice Details | Bill list / reprints | `OrderInvoice` |
| Reports | Reports hub | `ReportsHub` |
| Master Data | Catalog hub | `MasterData` |

### Shop & devices

| Row | Opens | Class / action |
|-----|--------|----------------|
| Shop Details | Store profile | `CompanyDetailSetting` |
| Business Template | Template picker | dialog / `BusinessTemplateEngine` |
| Business Hours | Open/close times | time pickers on Settings |
| Printer Details | Bluetooth printer | `CompanyPrinterSetting` |
| Scale (template) | Weight scale | scale device picker |
| Inventory | Stock hub | `Inventory` → `AddInventory` |
| Expenses | Expense list | `Expenses` → `AddExpenses` |

### Support & data

| Row | Opens | Class / action |
|-----|--------|----------------|
| Support | Support hub | `SupportHub` → `CreateSupportTicket` / `MySupportTickets` / `SupportTicketDetails` |
| About | App info | `AboutUs` |
| Fetch Data | Cloud → local | `NetworkDataFetcher` |
| Update App | Play Store | in-app update / market |
| Synchronize | Upload pending | `CloudSyncStatus` via `CloudSyncNav` |

### Account

| Row | Opens | Class / action |
|-----|--------|----------------|
| Change PIN | MPIN change | `LoginMPin` flow |
| Staff role | Owner / Cashier / Waiter | staff roster (`StaffRole`) |
| Language | EN / HI / MR | `AppLanguage` |
| Rate Us | Play Store | market intent |
| Share App | Share sheet | `ShareApp` |
| Logout | Clear session | → `Login` |

---

## 6. Master Data (`MasterData`)

From Settings → Master Data (permission-gated).

| Row | Opens | Class |
|-----|--------|--------|
| Category | Food type / category | `AddCategory` |
| Subcategory | Subcategory CRUD | `AddSubcategory` |
| Portions | Portion Master | `AddPortionMaster` (+ product portions via `ManageProductPortions`) |
| Products | Product list | `ProductMaster` → `AddProduct` / `UpdateProduct` |
| Catalog CSV | Import / export | `ImportExportEngine` menu |
| Combos | Combo master | `ComboMaster` → `AddCombo` / `UpdateCombo` |
| Table Master | Floor layout | `TableMasterActivity` |
| Appointments | Salon calendar | `SalonAppointmentModule` dialog (template) |
| Deposits | Bakery deposits | `CakeBakeryModule` ledger (template) |

---

## 7. Reports (`ReportsHub`)

From Settings → Reports (permission-gated). Legacy `ReportSetting` still exists but hub is primary.

| Row | Class |
|-----|--------|
| Sales Dashboard | `SalesDashboard` |
| Sales Overview | `SalesOverview` |
| Invoice Report | `InvoiceReport` |
| Sale-wise Report | `SaleReport` |
| Table Report | `InvoiceTableReport` |
| Takeaway Report | `InvoiceTakeAwayReport` |
| Payment Mode Report | `InvoicePaymentModeWiseReport` |
| Discount Report | `InvoiceDiscountReport` |
| Refund Report | `InvoiceRefundReport` |
| Product-wise Report | `InvoiceProductReport` |
| Combo-wise Report | `InvoiceProductReport` (`COMBO` filter) |
| Expense Report | `InvoiceExpenseReport` |
| Mess Member Report | `InvoiceMessMemberReportList` → `InvoiceMessMemberPaymentReport` |
| Mess Report | `InvoiceMessReport` |
| Delete all invoices | Clears local invoices only if none unsynced |

Also: `SalesList` for sale list drill-downs; `InvoiceTableListReport` for table list detail.

---

## 8. Support hub (`SupportHub`)

| Screen | Class | What it does |
|--------|--------|----------------|
| Support hub | `SupportHub` | Create ticket / my tickets / dial support |
| Create ticket | `CreateSupportTicket` | New support ticket (online) |
| My tickets | `MySupportTickets` | Ticket list |
| Ticket details | `SupportTicketDetails` | Thread / status |

---

## 9. Cloud sync (`CloudSyncStatus`)

Opened from Home sync, Settings synchronize, or after login when pending uploads exist. Shows pending invoice / catalog upload state; WorkManager + receivers continue sync in background.

---

## Features (cross-cutting)

- **Offline-first billing** — SQLite first; WorkManager + connectivity receivers sync when online
- **Order modes** — dine-in, takeaway, fast, mess membership + walk-in QR tokens
- **Catalog** — Food type → Category → Subcategory → Product → Portions (Portion Master)
- **Combos** — combo master + components; invoice component snapshots
- **Store details** — structured shop name / address / phone for receipt headers
- **Licensing** — server-authoritative expiry; trial (7-day / 50-bill); renew same key
- **i18n** — English / Hindi / Marathi
- **Universal templates** — restaurant, mess, bar, retail, fashion, salon, bakery, weight/fresh; appointments / deposits / staff / barcode / weight / variants / wholesale when enabled
- **Observability** — Firebase Crashlytics, Performance, Analytics, Messaging

Pack + smoke: [../Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md](../Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md)

## Stack

Java 17, Activities/Fragments, ViewBinding, SQLite, Retrofit 3 + OkHttp 5, WorkManager, Picasso, Material, ZXing, MPAndroidChart, Lottie, Play In-App Update, AdMob, Firebase, Woosim Bluetooth JARs.

## Build

Open `WithTable/` in Android Studio (standalone Gradle project).

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK naming (often under `../releases/`, gitignored):

`POS-Billingwala-{version}-v{code}-unsigned.apk`

Add `google-services.json` for Firebase builds. Sign with your upload keystore before store rollout.

## Smoke test (screen path)

1. **Login** → **MPIN** → **Home**  
2. **Settings → Fetch Data** → catalog synced  
3. **Master Data → Products** → add product + portion → sync  
4. **Master Data → Combos** → create combo → appear on **Home** / cart  
5. **Home → Table / Takeaway / Fast** → **CreatePos** → save + **Bluetooth print**  
6. **Settings → Shop Details** → print header uses structured fields  
7. **Home → Mess** → walk-in token → QR print → scan/verify  
8. **Settings → Language** → EN / HI / MR  
9. Expired / trial licence blocked on **Login**  
10. Template-gated: barcode, weight+scale, BOT printer, staff, salon appointments, bakery deposits, wholesale tiers — see Universal pack testing doc  

## Related

| Path | Notes |
|------|--------|
| `../API/` | POS PHP endpoints at androidApp root |
| `../Billingwala_Universal_POS_COMPLETE/23_TESTING_AND_MIGRATION.md` | Smoke + live deploy checklist |
| `../Billingwala_Universal_POS_Dynamic/` | Dynamic UI / navigation execution pack |
| `../docs/COMBO_API_REQUIREMENTS.md` | Combo API contract |
| `../docs/STORE_DETAILS_API_CHANGES.md` | Structured store fields |
| `../docs/LICENSE_API_REQUIREMENTS.md` | Licence / trial behaviour |
| `../docs/DEPLOY_DB.md` | Migrations (`server_upgrade_all.sql`) |
| `../API/migrations/p27_*.sql` … `p32_*.sql` | Appointments, deposits, tiers, variants, template, staff |
