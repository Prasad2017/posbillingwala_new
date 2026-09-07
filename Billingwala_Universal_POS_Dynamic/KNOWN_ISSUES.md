# KNOWN ISSUES — Dynamic POS

**Date:** 2026-09-07 (close-out refresh)

## Open (deferred by design)

1. **No BottomNavigationView / Drawer menu driven by NavigationRegistry**  
   App uses Home tiles + hubs. Registry drives Home QA intersection, secondary chips (`HomeDynamicQuickActions`), and a soft **More screens** sheet (long-press Home settings → `DynamicNavigationSheet`). Full bottom-nav rewrite deferred.

2. **CreatePos billing section remount is partial**  
   Cart-lifetime snapshot gates KOT chrome, barcode, and module prompts. Full BillingUIResolver section trees still do not remount shared catalog/cart XML blocks.

3. **USB / WiFi / Network print stacks**  
   Connection type is stored + pickable in `CompanyPrinterSetting` (`PrinterConnectionType` + DB `printerConnectionType`). Live print path remains Bluetooth; Wi‑Fi/Network store an identifier in the address field. USB is reserved.

4. **Dynamic list adapters still static**  
   `DynamicListRegistry` exposes product/inventory column plans and ProductMaster barcode search hint. Recycler adapters are not rewritten per-column.

5. **Bottom nav / drawer not ScreenRegistry-driven**  
   Soft gates cover Home catalog + Settings hubs via `ScreenRegistry.apply` / `guardOpen`.

## Closed in this close-out

- Repair / Rental hubs (`RepairModule.showHub` / `RentalModule.showHub`) + Home chips  
- Printer connection-type UI + persist  
- `DynamicListRegistry` + ProductMaster search hint  
- `DynamicNavigationSheet` (long-press settings)  
- Product form business extras (serial/warranty/flavour) via `DynamicProductFormUi`

## Mitigations in place

- ConfigApplyGuard blocks template apply during cart / payment / print  
- PosConfigCache for offline config snapshot  
- Print failure must not delete saved invoices (existing rule)  
- Soft visibility via FeatureEngine ∩ SecurityPermissions

## Do not “fix” by deleting

Historical invoices, products, and shop data must remain when modules/features are disabled.
