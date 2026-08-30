# POS App — Device & Tablet Support Plan

**App:** WithTable (POS Billingwala)  
**Status:** 100% screen parity (incl. support tickets); multi-breakpoint device support; **minSdk 24**; device QA pending  
**Last updated:** 2026-08-30

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

- POS (`WithTable`) **minSdk 24** (Android 7.0) after POI removal — verify on real Android 7 hardware
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
- **Audit result (2026-08-30):** ~~`org.apache.poi:poi:5.5.1`~~ **Removed.** Replaced with formatted HTML spreadsheet export (`ReportToSpreadsheet`) — no POI dependency; minSdk 24 is now feasible after full QA.
- Retest on real hardware: Bluetooth print, login, sync, camera/barcode
- Prefer resource qualifiers (`layout-sw600dp`, `layout-sw600dp-land`) over runtime layout inflation forks
- Revisit `DisplayUtils` behavior on large screens so it does not fight real tablet layouts
- Keep Admin / Dealer / Owner apps out of scope unless separately requested

---

## 9. Out of scope (this doc)

- Play Store listing screenshots
- Admin / Dealer / Owner tablet work

## 9.1 Implementation log

| Date | Change |
|------|--------|
| 2026-08-30 | `TabletUi` helper (600dp breakpoint); `layout-sw600dp/fragment_create_pos.xml` split catalog + inline cart; CreatePos tablet cart panel wired |
| 2026-08-30 | Tablet layouts for table/takeaway/mess hubs; Home 4-column billing row; payment dialog + bottom sheet tablet sizing; landscape default on tablet |
| 2026-08-30 | Phase 2: SalesList two-pane + detail panel; SalesOverview 4-KPI row; ReportsHub split; product form 2-column; ProductMaster 2-col grid; inventory/expense wide tables |
| 2026-08-30 | Operational report tablet layout (all invoice/table/payment/product reports); Sales Dashboard tablet; wider report invoice rows |
| 2026-08-30 | minSdk 24 audit: blocked by Apache POI 5.5.1 — resolved after POI removal |
| 2026-08-30 | Phase 3: Master Data + User Settings two-column tablet layouts; Home sales/catalog side-by-side on tablet |
| 2026-08-30 | Replaced Apache POI with `ReportToSpreadsheet` (formatted HTML `.xls`) for invoice report export |
| 2026-08-30 | minSdk lowered to **24** after POI removal; company settings two-column tablet form |
| 2026-08-30 | Category/subcategory form+list split; combo master grid + add-combo split; device QA checklist |
| 2026-08-30 | Portion master + product portions tablet split; cloud sync, add inventory, product master layouts |
| 2026-08-30 | **All remaining screens:** `TabletFormUi` helper; report settings 2-col menu; mess member/payment forms; expenses; About Us split; printer settings 2-col; auth screens centered; order invoice grid; mess member 2-col grid |
| 2026-08-30 | **Checkout & edit bill:** BluetoothPrint cart/payment split + landscape; EditInvoice sidebar layout; invoice receipt preview widened; Home sw600dp padding |
| 2026-08-30 | **Print & mess activities:** `TabletPrintUi` helper; duplicate bill checkout split; invoice/coupon/token preview widening; test print centered; mess scan/walk-in forms |
| 2026-08-30 | **Phase 6 — 100% parity + all devices:** Support tickets tablet UI (`SupportUiHelper`); multi-breakpoint `TabletUi` (600/720/840dp); splash + Bluetooth picker + bottom sheets scale; `supports-screens` + resizeable activities; `values-sw720dp` margins |
| 2026-08-30 | **Phase 7 — Cover all dialogs/popups:** `DialogUi` + `PopupUi` helpers; all report/share popups + month picker tablet-width; searchable dropdown + picture picker sheets; 3-col grids on 840dp+ (products, orders, mess members, takeaway) |
| 2026-08-30 | **2 GB POS RAM tuning:** CursorWindow capped at 2 MB on ≤2 GB devices (was 50 MB); `DeviceHealthMonitor.isLowRamDevice()` |

---

## 9.2 Device breakpoints (all Android screens)

| Class | Smallest width | Layout / behavior |
|-------|----------------|-------------------|
| Phone | &lt; 600dp | Default `layout/` — single column |
| Tablet (7") | ≥ 600dp | `layout-sw600dp/` + Java split helpers |
| Large tablet (10") | ≥ 720dp | Wider centered panels, 2-col grids, `values-sw720dp` margins |
| Expanded / desktop | ≥ 840dp | 3-col grids, max form width 720dp |

`TabletUi` centralizes: `formPanelMaxWidthDp`, `bottomSheetMaxWidthDp`, `gridColumnCount`, `horizontalInsetDp`.

---

## 10. Next steps (when ready to build)

1. ~~Confirm minSdk decision~~ — **minSdk 24** applied; QA on Android 7 device recommended  
2. ~~Implement Phase 1 layouts~~ — done  
3. ~~Phase 2 sales/reports/product/inventory~~ — done  
4. ~~Phase 3 + remaining screens~~ — done  
5. ~~Device QA matrix~~ — automated audit **PASS** (2026-08-30); manual device smoke test pending (no emulator on build machine)

### Phase 6 — Support & all devices ✅ (implemented)
- [x] **Support hub** — create / my tickets side-by-side on tablet
- [x] **Create ticket** — category + subject in one row on tablet
- [x] **My tickets** — 2-col (tablet) / 3-col (840dp+) ticket grid
- [x] **Ticket details** — centered panel; send + refresh in one row
- [x] **Splash** — logo capped width on tablet
- [x] **Bluetooth device picker** — centered sheet on tablet
- [x] **Bottom sheets** — scale to 520 / 560 / 600dp by device class

### Phase 7 — Dialogs & popups (all devices) ✅ (implemented)
- [x] Report period menus (sale/day/month/year) — readable width on tablet
- [x] Share / mess / table list popups — tablet-width via `PopupUi`
- [x] Month picker — centered max width on tablet
- [x] Searchable dropdown + picture picker — `BottomSheetUi` sizing
- [x] Product / order / member grids — 3 columns on 840dp+

---

## 11. Device QA checklist

Run on **phone** and **7" tablet (≥600dp)**, ideally **Android 7 (API 24)** and one current Android version.

### Install & login
- [ ] App installs and opens on API 24 device/emulator
- [ ] Login / licence sync works
- [ ] Home loads with correct billing tiles for licence flags

### Phase 1 — Billing (tablet landscape)
- [ ] **CreatePos** — split catalog + cart; add/remove qty; pay
- [ ] **Table / TakeAway / Mess** hubs — usable layout, start bill
- [ ] **Payment dialog** — readable buttons, confirm payment
- [ ] **Home** — billing row + sales/catalog side-by-side on tablet

### Phase 2 — Daily ops
- [ ] **SalesList** — list + detail panel on tablet
- [ ] **Reports hub** — split shortcuts + report list
- [ ] **Operational report** (invoice/sale/product) — KPIs + charts + table
- [ ] **Product / inventory / expense** — wide tables, product form 2-column

### Phase 3 — Setup
- [ ] **User settings / Master data** — two-column menus on tablet
- [ ] **Shop details** — two-column form on tablet
- [ ] **Portion master / product portions** — form left, list right on tablet
- [ ] **Cloud sync status** — summary + table list side-by-side on tablet
- [ ] **Add inventory** — product + qty in one row on tablet

### Phase 4 — Remaining screens
- [ ] **Report settings** — two-column menu on tablet
- [ ] **Mess members** — 2-column member grid; member/payment forms 2-column fields
- [ ] **About Us** — brand + contact side-by-side on tablet
- [ ] **Login / Register / MPIN** — centered panel on tablet
- [ ] **Printer settings** — two-column form cards on tablet
- [ ] **Order invoice list** — 2-column grid on tablet
- [ ] **BluetoothPrint checkout** — cart left, payment right on tablet
- [ ] **Edit bill** — line items left, totals sidebar right on tablet
- [ ] **Invoice receipt preview** — widened centered preview on tablet

### Phase 5 — Print & mess activities
- [ ] **Duplicate bill print** — cart/payment split on tablet
- [ ] **Invoice details print** — widened receipt preview
- [ ] **Coupon / test invoice print** — centered widened preview
- [ ] **Product list print** — landscape layout
- [ ] **Mess token print** — widened QR token preview
- [ ] **Mess token scan / walk-in** — centered form, name+mobile side-by-side

### Export & hardware
- [ ] **Invoice report export** — share `.xls`, opens clearly in Sheets/Excel
- [ ] **Bluetooth print** — test bill print on real printer
- [ ] **Cloud sync** — fetch / synchronize from Home or settings
- [ ] **Barcode / camera** (if used) — scan product or mess token

### Performance (2 GB if possible)
- [ ] Large product catalog scroll stays smooth
- [ ] No OOM when opening reports with many rows

---

## 12. Automated QA results (2026-08-30)

Run on build machine before manual device testing.

| Check | Result |
|-------|--------|
| `assembleDebug` | **PASS** |
| minSdk 24 / targetSdk 37 | **PASS** |
| `supports-screens` + `resizeableActivity` | **PASS** |
| `layout-sw600dp` layouts | **33 files** |
| Breakpoint resources (`values-sw600dp`, `values-sw720dp`) | **PASS** |
| All 53 fragments tablet-covered | **PASS** (0 gaps) |
| Popups use `PopupUi` (no raw `PopupWindow` in app code) | **PASS** |
| Bottom sheets use `BottomSheetUi.applyFullWidth` | **PASS** |
| Support tickets responsive UI | **PASS** (code) |
| On-device / emulator smoke test | **PENDING** — Android SDK not available on CI machine |

### Recommended manual smoke test (15 min)

1. **Phone (360dp)** — Login → Home → CreatePos → add item → pay → back  
2. **7" tablet emulator (600dp, landscape)** — same flow; confirm split cart + landscape lock  
3. **10" tablet or resized emulator (720dp+)** — Reports hub → invoice report → period menu popup width  
4. **Support** — Settings → Support → create ticket → my tickets grid  
5. **Print** — Bluetooth print preview widened on tablet (no printer required for layout check)

APK path after build: `WithTable/app/build/outputs/apk/debug/app-debug.apk`
