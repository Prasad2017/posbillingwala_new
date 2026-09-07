# 00 Master Readme — Universal POS (safe upgrade)

Upgrade the existing Billingwala project safely. Use configuration-driven architecture, reusable engines, feature flags, role permissions and offline-safe behavior. **Analyze existing code first and do not break current billing.**

---

## Goal

Evolve the live restaurant/mess POS into a **Universal multi-business POS** without rewriting the billing core. Configuration (business template + feature flags) turns capabilities on/off; engines stay shared.

## Pack map (00–24) — complete

| Doc | Topic | Status |
|-----|--------|--------|
| 00 | Master Readme | **Done** |
| 01 | Business Template Engine | **Done** |
| 02 | All Business Types | **Done** |
| 03 | Feature Engine | **Done** |
| 04 | Universal Billing Engine | **Done** |
| 05 | Restaurant Food Module | **Done** |
| 06 | Bar Restaurant Module | **Done** (KOT→BOT split + optional BOT BT) |
| 07 | Mess QR Token | **Done** |
| 08 | Weight Fresh | **Done** (keypad + optional BT scale) |
| 09 | Retail Grocery Wholesale | **Done** (barcode + wholesale tiers + cloud sync) |
| 10 | Fashion Jewellery Variants | **Done** (product_variant + CreatePos picker + cloud sync) |
| 11 | Salon Service Appointment | **Done** (POS calendar + Owner calendar/status + sync) |
| 12 | Cake Bakery Custom Order | **Done** (photo + deposit ledger + cloud sync + bill photo) |
| 13 | Product Service Engine | **Done** |
| 14 | Import Export | **Done** (POS Catalog CSV + Owner Excel) |
| 15 | Inventory Stock Engine | **Done** |
| 16 | Universal Printer | **Done** (bill/KOT + optional BOT BT) |
| 17 | Admin Dealer Platform | **Done** |
| 18 | Crash API Device Logging | **Done** |
| 19 | Dynamic Settings Reports | **Done** |
| 20 | Database API Architecture | **Done** |
| 21 | Offline Sync Architecture | **Done** |
| 22 | Security Permissions | **Done** (device roles + staff_user roster sync + reportPin) |
| 23 | Testing And Migration | **Done** (smoke checklist expanded) |
| 24 | Master Cursor Prompt | **Done** |

## Facades (`WithTable/.../Extra/`)

Use `UniversalPosModules.fullDiagnostics(context)` for a full readout.

| Area | Classes |
|------|---------|
| Config | `BusinessTypes`, `BusinessSession`, `BusinessTemplate*`, `FeatureFlags`, `FeatureEngine` |
| Billing | `BillingMode`, `UniversalBillingEngine` |
| F&B / Mess / Bar | `RestaurantFoodModule`, `MessModule`, `BarRestaurantModule`, `TicketRoute` |
| Verticals | `WeightFreshModule`, `RetailGroceryModule`, `FashionJewelleryModule`, `SalonAppointmentModule`, `CakeBakeryModule` |
| Catalog / stock / print | `ProductServiceEngine`, `ImportExportEngine`, `InventoryStockEngine`, `UniversalPrinterEngine` |
| Platform | `AdminDealerPlatform`, `CrashApiDeviceLogging`, `DynamicSettingsReports`, `DatabaseApiArchitecture`, `OfflineSyncArchitecture`, `SecurityPermissions` |

## Safe constraints (do not break)

- Offline-first invoice save; print failure must not wipe bills
- Legacy wire: `fast_billing` / `table_wise` / `take_away` / `mess`
- Mess QR `POSBILL|v1|…`
- Additive SQLite only
- Restaurant default template = prior live behaviour

## Status — implementation complete

| Layer | Status |
|-------|--------|
| Pack 00–24 code | **Done** |
| Live MySQL `p27`–`p32` + PHP | **Done** (per ops) |
| Priority-3 selectable types | **Done** |
| Laravel web business template | **Done** (`BusinessTemplateSupport`) |
| Fresh `API.zip` | **Done** (repo root; re-upload if not yet) |

**Your next step (not code):** smoke **1–26** on a live licence — `23_TESTING_AND_MIGRATION.md` or the `universal-pos-smoke` canvas.

**Optional only:** proprietary scale vendor SDK if a scale does not speak SPP ASCII.

See `24_MASTER_CURSOR_IMPLEMENTATION_PROMPT.md`.
21