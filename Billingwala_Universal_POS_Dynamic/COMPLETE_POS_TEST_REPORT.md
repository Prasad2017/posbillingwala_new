# COMPLETE POS TEST REPORT

**Date:** 2026-09-07  
**Build target:** WithTable debug  
**Scope:** Dynamic package phases 01–13 (wiring pass)

## Method

- Static code review against phase docs  
- Wiring verification (call sites for registries/guards)  
- Device QA checklist below (manual — run on restaurant + retail shops)

## Automated / static checks

| Check | Result |
|-------|--------|
| CreatePos billing snapshot gates prompts/chrome | PASS (KOT, barcode, variant/portion/weight/custom/appt) |
| ConfigApplyGuard payment + print on BluetoothPrint | PASS (wired this pass) |
| Payment flag set when launching payment from CreatePos | PASS |
| ScreenRegistry guardOpen on Home/Settings | PASS |
| MessWalkInTokenActivity in manifest | PASS |
| ReportsHub / ReportSetting → ReportResolver | PASS |
| DynamicProductFormUi on Add/UpdateProduct | PASS |
| FieldValidator on product name/price gate | PASS |
| UniversalPrinterEngine.addressForRole | PASS (helper) |
| PrinterConnectionType UI + DB persist | PASS (CompanyPrinterSetting) |
| RepairModule / RentalModule showHub | PASS (Home chips) |
| DynamicListRegistry + ProductMaster hint | PASS |
| DynamicNavigationSheet (long-press settings) | PASS |
| Template apply blocked while busy | PASS (BusinessTemplateEngine) |
| No new TODO placeholders introduced | PASS |

## Manual device checklist (required before release)

### Navigation / Home
- [ ] Restaurant: Fast / Dine-In / Take Away visible; Mess hidden  
- [ ] Mess shop: Mess tile visible  
- [ ] Retail: Fast visible; table tiles hidden  
- [ ] Weight / salon / bakery / inventory shops show matching secondary Home chips  
- [ ] Stock chip opens Inventory; Appointments opens upcoming dialog  
- [ ] Repair / Rental chips open hubs (new bill / appointments|deposits / recent)  
- [ ] Long-press Home settings → More screens sheet  
- [ ] Template switch blocked while cart open (message / no apply)

### Billing
- [ ] Open CreatePos → add item → pay → print → return; cart clears correctly  
- [ ] Mid-cart: change template from Settings blocked  
- [ ] Barcode scan (grocery) when BARCODE feature on  
- [ ] KOT print (dine-in) does not delete invoice on BT fail  
- [ ] Hold table / resume cart

### Printer
- [ ] Bill / KOT / BOT MACs; disconnected BT shows friendly error  
- [ ] Connection type picker saves bluetooth/wifi/usb/network  
- [ ] Wi‑Fi/Network connect stores identifier (BT stack still used for live print)  
- [ ] Cash drawer / cutter settings unchanged

### Products
- [ ] Add product: empty name blocked; GST section matches GST flag  
- [ ] Serial / warranty / flavour extras when template shows them  
- [ ] Portions section only when PORTIONS enabled  
- [ ] Update product same visibility
- [ ] ProductMaster search hint when barcode field visible

### Settings / Reports
- [ ] UserSetting sections match template  
- [ ] ReportsHub rows match features (tables/takeaway/mess/combo)

### Offline / Multi-shop
- [ ] Airplane mode: create bill, sync later  
- [ ] Switch shop: configs/products do not mix  
- [ ] PosConfigCache snapshot after Home load

## Business matrix (smoke)

| Business | Fast | Tables | Mess | Weight | Variants | Notes |
|----------|------|--------|------|--------|----------|-------|
| Restaurant | Y | Y | N | N | N | Primary |
| Bar+Restaurant | Y | Y | N | N | N | BOT |
| Mess | Y | N | Y | N | N | QR/token |
| Retail/Grocery | Y | N | N | N | N | Barcode |
| Weight fresh | Y | N | N | Y | N | Scale |
| Fashion | Y | N | N | N | Y | Variants |
| Salon | Y | N | N | N | N | Appointments |
| Bakery | Y | N | N | N | N | Custom order |
| Electronics | Y | N | N | N | N | Serial prompt |
| Repair | Y | N | N | N | N | Job note + appt |
| Rental | Y | N | N | N | N | Return + deposit |

## Residual risk

See `KNOWN_ISSUES.md`. Full bottom-nav rewrite and dedicated repair/rental screens intentionally deferred.
