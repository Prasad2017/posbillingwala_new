# POS App — Device & Tablet Support Plan

**App:** WithTable (POS Billingwala)  
**Status:** Planning only (no implementation yet)  
**Last updated:** 2026-08-29

---

## 1. Target hardware profile

| Requirement | Target | Notes |
|-------------|--------|--------|
| Minimum OS | **Android 7** (API 24) | Better balance than Android 6; closer to current minSdk 26 |
| Minimum RAM | **2 GB** | Enough for lean POS; expect limited multitasking |
| Tablet support | **From 7 inch** | Use ~600dp smallest width (`sw600dp`) as tablet breakpoint |
| Phones | Keep supported | Phone layouts stay; tablets get optimized layouts |

### Why Android 7 (not 6)

- Closer to current **minSdk 26** (Android 8) → less compatibility work than API 23
- Most POS features (Bluetooth printers, permissions, networking, local DB) work on 7
- Android 6 adds old-OS edge cases and library/Play friction for little market gain

### Why 2 GB RAM is enough

- Day-to-day billing can run fine if lists are paged, images sized down, and DB queries stay efficient
- Risk areas on 2 GB: huge in-memory catalogs, large bitmaps, leaks, heavy background work
- Tablets often feel better than phones at 2 GB because counter UI is simpler (fewer stacked screens)

### Current app baseline (today)

- POS (`WithTable`) **minSdk 26** (Android 8.0)
- Layouts are mostly **phone-style** single layouts
- No dedicated tablet resource set (`layout-sw600dp`, etc.) yet
- `DisplayUtils` adjusts font scale on large screens — that is **display scaling only**, not tablet UI

---

## 2. What “7 inch start” means in Android

Android does not use physical inches in layout selection. Practical mapping:

| Shop language | Android approach |
|---------------|------------------|
| 7" tablet and above | Treat **≥ ~600dp** smallest width as tablet (`sw600dp`) |
| Counter POS use | Prefer **landscape** on tablet |
| Phone | Existing layouts (`layout/`) |

On a 7" device the app will **run** today, but may look stretched/sparse until tablet layouts exist.

---

## 3. Tablet UI priority plan

Focus on screens staff use **all day on the counter**. Master/settings can stay phone-style longer.

### Phase 1 — Must have (billing day)

These decide if tablet feels like a real POS or a stretched phone.

| Priority | Screen | Why first | Tablet layout idea |
|----------|--------|-----------|--------------------|
| 1 | **CreatePos** (billing / cart) | Highest daily use | Split: categories + products left, cart + pay right |
| 2 | **InvoiceCompanyTable** (table billing) | Core restaurant flow | Wider table grid, or table grid left / order detail right |
| 3 | **InvoiceTakeAway** | High frequency | Same split pattern as CreatePos |
| 4 | **InvoiceMess** (if mess mode used) | Same pattern as takeaway/table | Same split billing pattern |
| 5 | **OrderInvoice / payment dialogs** | Checkout moment | Wider dialogs, bigger pay buttons, less scrolling |
| 6 | **Home** | Entry hub | Larger tiles/grid, landscape-friendly |

**Phase 1 rule:** If only these look good on 7", shops will accept the rest for launch.

### Phase 2 — Should have (daily ops)

Used often, but not every 30 seconds.

| Priority | Screen | Tablet idea |
|----------|--------|-------------|
| 7 | **SalesList / SalesOverview / SalesDashboard** | Two-pane: list left, details right |
| 8 | **ReportsHub + main reports** (sale, product, payment-mode, table) | Wider tables, less horizontal squeeze |
| 9 | **ProductMaster + Add/Update Product** | Form with 2 columns |
| 10 | **Inventory + AddInventory** | Wider list + quick stock edit |
| 11 | **Expenses** | Readable rows, less scroll |

### Phase 3 — Nice later (setup, not counter work)

Fine as scaled phone UI on tablet at first:

- Category / Subcategory / Portion master
- Combo master
- Mess member add/update/payment
- Company settings, printer settings, user settings
- Support tickets, About, Cloud sync
- Rare reports (refund, discount, expense, mess member payment)

Do these only after Phase 1–2 feel solid.

---

## 4. Recommended build order

1. **CreatePos** tablet split layout  
2. Reuse that pattern for **TakeAway / Mess / Table order** screens  
3. **Home** tile layout for landscape  
4. **Payment / invoice confirm** dialogs  
5. **Sales + top reports** two-pane  
6. **Product + inventory** forms  

Do **not** tablet-optimize all 50+ fragments at once.

### Timeline mindset

| Phase | Meaning |
|-------|---------|
| Phase 1 | Real product launch for tablet |
| Phase 2 | Comfortable daily use |
| Phase 3 | Polish |

---

## 5. Design rules for 7" tablets

- Breakpoint: **≥600dp** width / smallest width → tablet
- Default POS orientation on tablet: **landscape**
- Touch targets: larger (fast finger use at counter)
- Prefer **side-by-side** over more scrolling
- Keep **same Java/Kotlin logic**; add alternate XML layouts where needed
- Phone layouts stay as-is; add `sw600dp` (and/or land) variants for Phase 1 screens first
- Avoid loading full catalogs into memory; page lists and downsize images for 2 GB devices

---

## 6. Phase 1 wireframe checklist

### 6.1 CreatePos (billing)

| Zone | Content |
|------|---------|
| Left (~55–65%) | Category chips/tabs + product grid/list + search |
| Right (~35–45%) | Cart lines, qty controls, totals, discount, **Pay / Save** |
| Top bar | Table/customer/order context, back, optional sync/printer status |
| Must avoid | Stacking cart under products with long scroll on landscape 7" |

### 6.2 InvoiceCompanyTable

| Zone | Content |
|------|---------|
| Primary | Table grid with clear occupied/free states |
| Optional split | Selected table summary / open order actions on the right |
| Actions | New order, continue order, settle — large tap targets |

### 6.3 InvoiceTakeAway / InvoiceMess

| Zone | Content |
|------|---------|
| Same pattern as CreatePos | Products left, cart/order right |
| Header | Mode label (Take Away / Mess), member or token if applicable |

### 6.4 OrderInvoice / payment dialogs

| Zone | Content |
|------|---------|
| Layout | Wider bottom sheet or centered dialog (not full phone-narrow) |
| Content | Amount due, payment modes, tender/change, print toggle |
| Actions | Confirm / Cancel large and reachable with thumb in landscape |

### 6.5 Home

| Zone | Content |
|------|---------|
| Layout | Responsive tile grid (2–3 columns on 7" landscape) |
| Tiles | Billing modes, sales, reports, products, settings |
| Must avoid | Single long vertical list with tiny tiles and large empty margins |

---

## 7. Phase 1 success criteria

On a **7" Android 7 / ~2 GB** tablet, staff should complete without “zoomed phone” feel:

1. Open app → **Home**
2. Start **bill / table / takeaway** (and mess if enabled)
3. Add products + edit qty
4. Take **payment** + **print**

---

## 8. Implementation notes (when coding starts)

- Lowering minSdk from 26 → 24 requires dependency audit (libraries may force higher minSdk)
- Retest on real hardware: Bluetooth print, login, sync, camera/barcode
- Prefer resource qualifiers (`layout-sw600dp`, `layout-sw600dp-land`) over runtime layout inflation forks
- Revisit `DisplayUtils` behavior on large screens so it does not fight real tablet layouts
- Keep Admin / Dealer / Owner apps out of scope unless separately requested

---

## 9. Out of scope (this doc)

- Code changes
- Exact XML/Compose designs
- Play Store listing screenshots
- Admin / Dealer / Owner tablet work

---

## 10. Next steps (when ready to build)

1. Confirm minSdk decision: **24 (Android 7)** vs stay on **26** and only add tablet UI  
2. Implement Phase 1 layouts for CreatePos → other billing modes → Home → payment  
3. Device QA matrix: phone + 7" tablet, Android 7 and a current Android version, 2 GB where possible  
4. Then Phase 2 sales/reports/product/inventory
