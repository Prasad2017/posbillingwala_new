# DYNAMIC_UI_ANALYSIS — Billingwala POS (WithTable)

**Phase:** Dynamic UI Engine  
**Rule:** Analyze first — do not rewrite UI in this document.  
**App:** Single POS (`WithTable`) — one app, configuration-driven visibility.

---

## Current UI Architecture

| Layer | Mechanism |
|-------|-----------|
| Shell | `MainActivity` hosts a fragment stack (`loadFragment` / `navigateBack`) |
| Drawer | `DrawerLayout` shell only — no NavigationView menu |
| Bottom nav | Dimens exist; not used as primary navigation |
| Dashboard | `Fragment.Home` — billing tiles + sales KPIs + catalog counts |
| Settings | `Fragment.UserSetting` — grouped menu rows |
| Reports | `Fragment.ReportsHub` (+ legacy `ReportSetting`) |
| Billing | `Fragment.CreatePos` + table / takeaway / mess entry screens |
| Master | `Fragment.MasterData` → catalog / table / salon / bakery rows |
| Gates | `FeatureEngine` (template ∩ licence ∩ company) + `SecurityPermissions` (role / PIN) |

Navigation is fragment-centric. Dynamic UI must resolve **what to show** and apply via existing `LicenseModules.setVisible` / `FeatureEngine.setVisible`, not invent a second nav graph.

---

## Existing Screens (inventory)

### Shell / auth
`SplashScreen`, `Login`, `LoginMPin`, `Register`, `MainActivity`, `BaseActivity`

### Dashboard / settings
`Home`, `UserSetting`, `CompanyDetailSetting`, `AboutUs`, `ShareApp`, `SupportHub`, `CreateSupportTicket`, `MySupportTickets`, `SupportTicketDetails`, `CloudSyncStatus`

### Billing
`CreatePos`, `InvoiceCompanyTable`, `InvoiceTakeAway`, `InvoiceMess`, `OrderInvoice`, `InvoiceProductDetails`

### Catalog / products / categories
`MasterData`, `AddCategory`, `AddSubcategory`, `ProductMaster`, `AddProduct`, `UpdateProduct`, `AddPortionMaster`, `ManageProductPortions`, `ComboMaster`, `AddCombo`, `UpdateCombo`, `Inventory`, `AddInventory`, `Expenses`, `AddExpenses`

### Restaurant / tables
`TableMasterActivity`, `InvoiceCompanyTable` + `DineInOpsUi` / `DineInTableHelper` / `DineInKotHelper`

### Mess
`MessMemberList`, `AddMessMember`, `UpdateMessMember`, payment / session / token / QR Activities

### Reports
`ReportsHub`, `ReportSetting`, `SalesDashboard`, `SalesOverview`, `SalesList`, `SaleReport`, invoice / payment / discount / refund / product / expense / mess reports

### Print
Bluetooth print Activities + `CompanyPrinterSetting`

### Vertical UIs (module dialogs, not duplicate apps)
Salon appointments, bakery deposits/custom order, weight keypad/scale — via `Extra/*Module`

---

## Reusable Components (keep)

| Component | Role |
|-----------|------|
| `FeatureEngine` / `FeatureFlags` | Canonical capability gate |
| `BusinessTemplateEngine` / `BusinessSession` | Template + type |
| `SecurityPermissions` / `StaffRole` | Role + PIN |
| `UniversalBillingEngine` | Billing modes — **do not replace** |
| `ProductServiceEngine` | Catalog capabilities |
| `InventoryStockEngine` | Stock |
| `DynamicSettingsReports` | Settings/reports entry map |
| `LicenseModules.setVisible` | View VISIBLE/GONE |
| `EmptyListUi`, `TabletUi`, `ReportUiHelper`, `BottomSheetUi`, `DialogUi` | Presentation helpers |
| `UniversalPosModules` | Diagnostics registry |

---

## Duplicate Screens

| Pair | Notes |
|------|-------|
| `ReportsHub` vs `ReportSetting` | Same report gates; Hub is primary. Prefer Hub for Dynamic UI. |
| Home billing tiles vs Settings “quick” paths | Same features; one resolver for both. |
| Module `isEnabled()` vs `FeatureEngine.isEnabled` | Modules wrap FeatureEngine — UI should prefer Dynamic UI → FeatureEngine. |

**Do not** create per-business Home / CreatePos copies.

---

## Hardcoded UI (migration targets)

| Location | Hardcoding | Recommended |
|----------|------------|-------------|
| `Home.applyModuleVisibility` | Direct FeatureEngine calls | Resolve via `UIResolverService` / quick actions + widgets |
| `Home` combo card | Always shown | Gate on `FeatureFlags.COMBOS` |
| `Home` KPI clicks | Direct `LicenseModules` | Align with FeatureEngine / Dynamic UI |
| `ReportsHub` / `ReportSetting` | Licence-only (`dineIn`/`takeAway`/`mess`) | FeatureEngine + report screen config |
| `MasterData.applyFeatureVisibility` | FeatureEngine (good) | Keep; optionally read from resolved screen list |
| `UserSetting` inventory / scale | FeatureEngine / WeightFreshModule | Settings section config |
| `CreatePos` | Module prompts (weight/variant/salon/bakery) | Billing field/section config (behaviour stays in modules) |
| `RestaurantFoodModule.isFoodHospitalityTemplate` | businessType switch | Allowed inside domain modules; UI must not duplicate |

---

## Recommended Migration (gradual)

```text
Existing Screen
    → Wrap with DynamicUiGuard / FeatureEngine (already partly done)
    → Connect UIConfiguration from UIResolverService
    → Apply visibility/order from config
    → Regression test (restaurant default unchanged)
```

**Phase order:** Analysis → Engine + cache → Home + Reports → Master + Settings → Billing/Product field hints → docs + diagnostics.

**Do not** replace all screens in one commit.

---

## Breaking Change Risks

| Risk | Mitigation |
|------|------------|
| Restaurant tiles disappear | Default template + fallback = prior restaurant Home |
| Reports hide for dine-in shops | FeatureEngine must match prior licence ∩ template |
| Offline blank UI | Disk + memory cache + `UiFallback` default POS |
| Role hides billing for waiter incorrectly | Waiter keeps `BILL`; only settings/master restricted |
| Double-gate (licence + FeatureEngine) | FeatureEngine already includes licence |
| Heavy resolve every frame | `UiConfigCache` memory + SharedPreferences |
| Security by UI hide only | Keep `SecurityPermissions.runAuthorized` + backend |

---

## Priority Resolution (target)

1. System default  
2. Business template  
3. Business configuration (custom JSON)  
4. Enabled features (`FeatureEngine`)  
5. User role  
6. User permission  

**Conflicts:** Visibility is AND across layers (most restrictive wins). Labels/order: higher priority overlay wins when explicitly set.

---

## Out of scope for this analysis

- Rewriting XML layouts  
- Separate apps per business  
- Replacing Universal Billing / Product / Inventory / Printer engines  
