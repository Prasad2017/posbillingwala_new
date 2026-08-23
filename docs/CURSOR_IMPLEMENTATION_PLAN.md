# POS Billingwala — Implementation Plan

**Based on:** `docs/CURSOR_CODEBASE_AUDIT.md`  
**Rule:** One feature at a time. Build must succeed before next feature. P0 → P1 → P2 → P3.  
**Constraint:** Additive, backward-compatible, no DROP TABLE migrations, no data loss.

---

## Phase 0 — P0 Hardening (no catalog rewrite)

| ID | Feature | Goal |
|----|---------|------|
| P0-1 | Sync success handling | Only mark local rows synced when API `status == "1"` |
| P0-2 | Dual receiver fix | Stop double upload from manifest + Home registration |
| P0-3 | Invoice pull workers | Fix async early-success in InvoiceWorker / InvoiceProductWorker |
| P0-4 | Invoice number single-source | Printed number always equals saved number |
| P0-5 | Data cleanup protection | Replace / guard `resetTables()` DROP path; never wipe unsynced bills silently |
| P0-6 | Crash monitoring | Add Firebase Crashlytics (+ basic non-fatal logging hooks) |

---

## Phase 1 — P1 Billing / Sync / Print / DB reliability

| ID | Feature | Goal |
|----|---------|------|
| P1-1 | Off-UI bill save | `saveInvoice` + cart DB writes off main thread |
| P1-2 | Off-UI sync queries | Upload constructors / receivers not block UI |
| P1-3 | Printer crash isolation | Failed print never crashes app; bill still saved |
| P1-4 | Local unique sync keys | Unique index / upsert on `invoiceNetworkStatus` locally |
| P1-5 | OkHttp log level | BODY logging only in debug builds |
| P1-6 | Safe `onUpgrade` | Additive column migrations only; never DROP |

---

## Phase 2 — P2 Performance / reporting

| ID | Feature | Goal |
|----|---------|------|
| P2-1 | Fast product search | Indexed / efficient search for billing |
| P2-2 | Report paging | Large invoice reports without ANR |
| P2-3 | Print bitmap off UI | Layout→bitmap on background |
| P2-4 | Previous-day sync UX | Explicit previous-day sync without full wipe |

---

## Phase 3 — Catalog target architecture (additive)

Food Type / Category → Optional Subcategory → Product → Portion

| ID | Feature | Goal |
|----|---------|------|
| P3-1 | Food type table + migration | Additive; map existing categories |
| P3-2 | Optional subcategory | Nullable parent; existing products keep working |
| P3-3 | Product portions | 0..N portions per product; configurable prices |
| P3-4 | Bill line snapshots | Store product + portion name/price on invoice lines |
| P3-5 | Billing UI for portions | CreatePos / cart / print use portions |
| P3-6 | Sync + API for portions | Server tables + endpoints; offline-first |
| P3-7 | Beverage as Food Type | Seed/support Beverage type without breaking data |

---

## Phase 4 — Licence / trial / multi-branch

| ID | Feature | Goal |
|----|---------|------|
| P4-1 | Server-authoritative expiry | Enforce `expiryDate` on check (not only status flag) |
| P4-2 | 7-day trial + max 50 bills | Server counters; never hard-code in APK |
| P4-3 | Validity tiers | 6m / 1y / 3y / 5y / lifetime in Dealer/Admin |
| P4-4 | Same-key upgrade | Renew without new key / device rebind |
| P4-5 | Multi-branch / franchise polish | Owner store-wise already partial; harden |

---

## Phase 5 — Observability & Owner/Admin API hygiene

| ID | Feature | Goal |
|----|---------|------|
| P5-1 | ANR monitoring | Firebase Performance / ANR reporting |
| P5-2 | API prepared statements | Incremental PHP hardening |
| P5-3 | Auth tokens | Session/token for Owner/Dealer/Admin/POS |
| P5-4 | Align API hosts | Confirm POS + Owner share same DB/host strategy |
| P5-5 | Remove secrets from repo dump | Sanitize SQL dump; rotate exposed keys if needed |

---

## Phase 6 — Catalog UI polish

| ID | Feature | Goal |
|----|---------|------|
| P6-1 | CreatePos subcategory chips | Filter products by subcategory after category pick |
| P6-2 | Product subcategory picker | Optional subcategory on Add/Update Product |
| P6-3 | Subcategory master CRUD | Add/edit subcategories under categories in POS |
| P6-4 | Portion management UI | Add/edit portions on product from Product Master |

---

## Release checklist (post Phase 6)

All planned phases (P0–P6) are **done**. Before production rollout:

### Server (run once, in order)

1. `API/db_local.php` from `db_local.example.php` with live credentials
2. Run **one** SQL file: `API/migrations/server_upgrade_all.sql`
   (or individual `p3_*` / `p5_3` files — see `docs/DEPLOY_DB.md`)
3. Deploy all `API/*.php` changes (auth tokens, prepared statements, catalog endpoints, licence/trial)
4. Rotate MySQL + Admin password if old dump was ever shared (see `docs/DEPLOY_DB.md`)

### Android (all four apps)

| App | Build | Notes |
|-----|-------|-------|
| WithTable (POS) | `assembleRelease` | Catalog UI (P6), portions, subcategories, sync, Crashlytics |
| Owner | `assembleRelease` | Bearer token, `API_BASE_URL` |
| Dealer | `assembleRelease` | Validity tiers, renew, token |
| Admin | `assembleRelease` | Token, bcrypt login |

**Verify builds:** all four `assembleDebug` and `assembleRelease` succeeded (Aug 2026).

### Release APKs (unsigned)

Built with `assembleRelease`. Copied to `releases/` (gitignored):

| File | App | versionName / versionCode |
|------|-----|---------------------------|
| `POS-Billingwala-2.0.51-v67-unsigned.apk` | WithTable (POS) | 2.0.51 / 67 |
| `Owner-1.0.6-v7-unsigned.apk` | Owner | 1.0.6 / 7 |
| `Dealer-1.0.10-v12-unsigned.apk` | Dealer | 1.0.10 / 12 |
| `Admin-1.0-v1-unsigned.apk` | Admin | 1.0 / 1 |

**Sign before rollout:** Gradle has no `signingConfig` in repo (keystore is gitignored). Sign with your existing upload key, e.g.:

```bash
apksigner sign --ks your-release.keystore --out signed.apk releases/POS-Billingwala-2.0.51-v67-unsigned.apk
```

Or configure `signingConfigs` in each app's `build.gradle` pointing to a local keystore (never commit the keystore).

---

1. Login (licence + MPIN) → token issued
2. Fetch data → categories, subcategories, portions download
3. Add subcategory + portion locally → sync uploads
4. Bill with portion product → print + invoice line snapshots
5. Trial/expired licence blocked server-side

---

## Current work

**Active feature:** Release rollout  
**Status:** Release APKs built; sign + deploy server migrations, then smoke test

### Phase 0 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P0-1 | Done | `OfflineNetworkData.java`, `OfflineToNetworkReceiver.java`, `UserSynchronizeData.java` |
| P0-2 | Done | `AndroidManifest.xml`, `Home.java` |
| P0-3 | Done | `InvoiceWorker.java`, `InvoiceProductWorker.java` |
| P0-4 | Done | `BluetoothPrint.java` |
| P0-5 | Done | `POSBillingWalaDatabase.java`, `Home.java`, `UserSetting.java`, `ReportSetting.java` |
| P0-6 | Done | `build.gradle` (root+app), `PosBillingWalaApp.java`, `AndroidManifest.xml` |

### Phase 1 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P1-1 | Done | `BluetoothPrint.java` — bill DB save on background executor |
| P1-2 | Done | `OfflineSyncExecutor.java`, `OfflineToNetworkReceiver`, `UserSynchronizeData`, `OfflineNetworkData` |
| P1-3 | Done | `BluetoothPrint`, `WoosimPrnMng`, `KOTWoosimPrnMng` — print failure isolation |
| P1-4 | Done | `POSBillingWalaDatabase.java` — upsert + unique indexes on sync keys (DB v10) |
| P1-5 | Done | `Retrofit/Api.java` — OkHttp BODY logging debug-only |
| P1-6 | Done | `POSBillingWalaDatabase.java` — additive schema ensure (DB v11) |

### Phase 2 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P2-1 | Done | `POSBillingWalaDatabase.searchProducts`, indexes; `CreatePos` debounced SQL search |
| P2-2 | Done | Report/order invoice fragments: one-page scroll load; initial load off UI; invoice date/type/payment indexes |
| P2-3 | Done | `BluetoothPrint` — resize/dither/BT write + PDF resize on `printBitmapExecutor`; layout capture stays on UI |
| P2-4 | Done | `PreviousDayInvoiceSync`, date-filter API (`getInvoiceList.php`, `getInvoiceProductList.php`), Settings UX |

### Phase 3 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P3-1 | Done | DB v12 `food_type` + `product_category.foodTypeId`; seed Food/Beverage; map legacy categories; `FoodTypeResponse`; `API/migrations/p3_1_food_types.sql` |
| P3-2 | Done | DB v13 `product_subcategory` + nullable `product.subcategoryId`; CRUD helpers; `ProductSubcategoryResponse`; `API/migrations/p3_2_product_subcategories.sql` |
| P3-3 | Done | DB v14 `product_portion`; portion CRUD + dedupe index; `getEffectiveProductPrice`; `ProductPortionResponse`; `API/migrations/p3_3_product_portions.sql` |
| P3-4 | Done | DB v15 snapshot columns on `cart_product` + `invoice_product`; `BillLineSnapshot`; cart/invoice save + read paths; `API/migrations/p3_4_bill_line_snapshots.sql` |
| P3-5 | Done | CreatePos portion picker; portion-aware cart lookup; cart/print/report adapters use `getDisplayLineName` / `getResolvedLinePrice`; `BluetoothPrint` totals |
| P3-6 | Done | PHP: `getFoodTypeList`, `getSubcategoryList`, `getPortionList`, inserts + extended category/product/invoice endpoints; Android: `FoodTypeWorker`, `SubcategoryWorker`, `PortionWorker`, upload/download for catalog + bill snapshots |
| P3-7 | Done | Beverage heuristic category mapping; Food/Beverage toggle in `CreatePos`; food type on add/edit category; `p3_7_beverage_category_mapping.sql` |

### Phase 4 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P4-1 | Done | `API/licence_expiry.php`; enforce on `Login.php`, `LoginMpin.php`, `check_licence_expire.php`; align `updateLicenceKey.php` + Android Login/LoginMPin/Home/LicenceKeyReceiver |
| P4-2 | Done | Server trial 7d + max 50 bills; Demo forced to 7 days; `insertInvoice` gate; auth returns trial fields; Android `TrialLimits` soft gate (no hard-coded caps) |
| P4-3 | Done | Validity tiers 6m/1y/3y/5y/lifetime; `licence_normalize_validity_days`; Dealer/Admin `LicenceValidityTiers` + arrays; wired into registration/adapters + insert/update PHP |
| P4-4 | Done | Same-key renew/upgrade via `licence_same_key_upgrade`; never clears key/device/mpin; expiry from max(today, currentExpiry)+days; Dealer/Admin Renew button |
| P4-5 | Done | Franchise branch labels + `getStoreWise` hardening; branch name on new licence; Owner store list polish; `branchCount` on home API |

### Phase 5 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P5-1 | Done | Firebase Performance + `Observability.java`; traces on save/sync; Crashlytics non-fatal on print/save failures; user context in `MainActivity` |
| P5-2 | Done | `API/db_prepared.php`; prepared statements on `licence_expiry.php`, `Login.php`, `LoginMpin.php`, `check_licence_expire.php`, `insertInvoice.php`, `updateAndroidKey.php` |
| P5-3 | Done | `api_tokens` migration + `auth_tokens.php`; tokens on all login endpoints; token guards on `insertInvoice`, Owner profile/product/count, Dealer `insertNewLicence`, Admin `getCustomerList`; all 4 Android apps persist token + Bearer interceptor |
| P5-4 | Done | Shared `API/db_connection.php`; Owner/Dealer/Admin configs include it; POS + media moved to `www.posbillingwala.com`; `BuildConfig.API_BASE_URL` / `MEDIA_BASE_URL` on all apps |
| P5-5 | Done | Removed `spllmgkn_posbill.sql`; added `API/schema/schema_reference.sql`, root `.gitignore`, `db_local.example.php`; credentials via `db_local.php`/env; Admin login uses `password_verify`; `docs/DEPLOY_DB.md` rotation notes |

### Phase 6 completion log

| ID | Status | Changed files |
|----|--------|---------------|
| P6-1 | Done | `CreatePos` subcategory chip filter; `fragment_create_pos.xml`; `HomeCategoryAdapter`; `POSBillingWalaDatabase.getHomeProductList`; `HomeProductsWorker` |
| P6-2 | Done | Optional subcategory spinner on `AddProduct` / `UpdateProduct`; layouts; `updateProduct` + `getProductDetail` subcategoryId |
| P6-3 | Done | `AddSubcategory`, `SubcategoryAdapter`; layouts; `getProductSubcategoryNameList`; nav from `AddCategory`; offline sync via existing `insertSubcategory` path |
| P6-4 | Done | `ManageProductPortions`, `PortionAdapter`; layouts; `getProductPortionNameList`; Portions button on `ProductMaster`; `updateProductPortion` sync flag |
