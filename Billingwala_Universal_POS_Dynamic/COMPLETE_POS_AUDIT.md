# COMPLETE POS AUDIT — Billingwala Universal Dynamic

**Date:** 2026-09-07  
**App:** `WithTable` (Android)  
**Scope:** Phase 01 — analysis only baseline; code wiring continued in later phases.

## Verdict

Resolver/registry layer under `Extra/dynamic` and `Extra/dynamicui` is largely present. UI is **partially** configuration-driven: Home quick actions, MasterData tabs, Settings sections, and Reports hub use soft visibility. Billing and product forms still rely primarily on vertical modules (`RestaurantFoodModule`, `WeightFreshModule`, etc.). NavigationRegistry had no UI callers before this execution pass.

Resolution chain:

`BusinessSession` → `BusinessTemplateRegistry` → `FeatureEngine` → `SecurityPermissions` → `UIResolverService` / `POSConfiguration` → visibility helpers.

---

## 1. Screens inventory

### Activities

| Class | Purpose | Dynamic layer |
|-------|---------|---------------|
| SplashScreen, Login, Register, LoginMPin | Auth | No |
| MainActivity | Fragment host + DrawerLayout | Hosts fragments that use dynamic |
| BaseActivity | Base | No |
| EditInvoice | Edit saved invoice | No |
| CompanyPrinterSetting | BT bill/KOT/BOT | Via UniversalPrinterEngine roles |
| TableMasterActivity | Table CRUD | No |
| BluetoothPrint (+ Duplicate/ProductList/Test/Coupon/MessToken/InvoiceDetails) | Print / payment | ConfigApplyGuard print/payment (wired) |
| Mess QR / token activities | Mess workflow | Module-driven |

### Fragments (main)

| Fragment | Dynamic usage |
|----------|---------------|
| Home | QA + widgets + NavigationRegistry ∩ QA |
| CreatePos | BillingUIConfiguration snapshot + field hints |
| MasterData | TabRegistry / TabGuard |
| UserSetting | SettingsRegistry |
| AddProduct / UpdateProduct | DynamicProductFormUi + FieldValidator |
| ReportsHub / ReportSetting | ReportResolver / DynamicUiEngine |
| InvoiceCompanyTable / InvoiceTakeAway / InvoiceMess | Module / feature flags |
| Product/inventory/sales/report detail screens | Mostly static |

**Note:** No BottomNavigationView. Navigation is Home tiles → `MainActivity.loadFragment`.

---

## 2. Dynamic layer

### `Extra/dynamic/`

| Class | Status |
|-------|--------|
| POSConfiguration, PosConfigCache | Wired |
| ConfigApplyGuard | Wired (cart/payment/print) |
| ModuleRegistry, FeatureRegistry | Wired / facade |
| ScreenRegistry | Built; UI does not navigate from it |
| NavigationRegistry | Wired to Home QA intersection |
| TabRegistry, TabGuard | Wired (MasterData) |
| FormRegistry, EntityConfiguration | Partial (product form apply) |
| FieldValidator | Used on product save |
| BillingUIConfiguration | Snapshot + cart guard |
| DynamicProductFormUi | Partial (layout-limited) |
| SettingsRegistry, DashboardWidgetResolver, ReportResolver | Wired |
| PrinterRole | Used by UniversalPrinterEngine |

### `Extra/dynamicui/`

UIResolverService, UiConfigCache, UiCodes, NavigationResolver, DashboardResolver, BillingUIResolver, ProductFormResolver, SettingsReportsResolvers, VisibilityRule — **resolve pipeline complete**; UI apply incomplete for full field/section trees.

### Vertical modules (live)

RestaurantFood, BarRestaurant, Mess, WeightFresh, RetailGrocery, FashionJewellery, SalonAppointment, CakeBakery + thin Electronics / Repair / Rental facades.

---

## 3. Business types

| Readiness | Types |
|-----------|-------|
| LIVE | restaurant, bar_restaurant, mess, retail, grocery, weight_fresh (+ fish/meat aliases), wholesale, fashion, jewellery, salon, bakery |
| PARTIAL (map to nearest) | electronics/mobile→retail, hardware→retail, repair/laundry→salon, rental→retail, custom→restaurant |

Cafe / Fast Food normalize to restaurant.

---

## 4. Gap matrix (pre / post this execution)

| Area | Pre | Post execution |
|------|-----|----------------|
| Config aggregate + UI resolve | DONE | DONE |
| Home QA / widgets | PARTIAL | PARTIAL (+ NavigationRegistry) |
| Master tabs | PARTIAL | PARTIAL |
| NavigationRegistry UI use | MISSING | PARTIAL (Home) |
| Bottom nav / drawer rewrite | MISSING | Deferred (not in app architecture) |
| Product form fields | PARTIAL | PARTIAL (layout-limited) |
| FieldValidator | MISSING | PARTIAL (name/price) |
| Billing field/section apply | PARTIAL | PARTIAL → deepened (prompts + KOT) |
| Billing workflow / payment config objects | MISSING | Deferred |
| ConfigApplyGuard payment/print | PARTIAL | DONE |
| Reports hub | PARTIAL | Uses ReportResolver |
| Printer role routing | PARTIAL | addressForRole helper |
| Offline config cache | PARTIAL | Unchanged (PosConfigCache) |
| Dynamic lists/columns/API remap | MISSING | Deferred |
| Electronics/Repair/Rental UX | PARTIAL | Deferred (facades only) |

---

## 5. Files of record

- Plan: `DYNAMIC_POS_IMPLEMENTATION_PLAN.md`
- Tests: `COMPLETE_POS_TEST_REPORT.md`
- Issues: `KNOWN_ISSUES.md`
- Code: `WithTable/app/src/main/java/com/pos_billingwala/Extra/dynamic*`, `Extra/dynamicui/*`
