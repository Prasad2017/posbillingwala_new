# DYNAMIC POS IMPLEMENTATION PLAN

**Date:** 2026-09-07  
**Principle:** Reuse Universal Billing / modules; visibility from config; never break live restaurant/retail/mess flows.

## Phase order

| Phase | Doc | Approach |
|-------|-----|----------|
| 01 | Audit | COMPLETE_POS_AUDIT.md (this package) |
| 02 | Configuration | Keep POSConfiguration + registries; no duplicate engines |
| 03 | Navigation / Tabs | Wire NavigationRegistry into Home; MasterData TabRegistry; defer bottom-nav rewrite |
| 04 | Data / Forms | Expand DynamicProductFormUi + FieldValidator on existing XML |
| 05 | Products / Inventory | Soft-gate existing fields; inventory screens stay module-driven |
| 06 | Billing UI | BillingUIConfiguration snapshot for cart lifetime; field hints |
| 07 | Restaurant / Bar / Mess | Preserve existing modules; do not fork |
| 08 | Other businesses | Thin electronics/repair/rental facades; map to templates |
| 09 | Settings | SettingsRegistry on UserSetting |
| 10 | Printer | PrinterRole + addressForRole; markPrintSession guard |
| 11 | Dashboard / Reports | DashboardWidgetResolver + ReportResolver |
| 12 | Offline / Multi-shop | PosConfigCache + ConfigApplyGuard |
| 13 | Testing | COMPLETE_POS_TEST_REPORT.md + KNOWN_ISSUES.md |

## Execution batches (safe)

1. **Guards** — payment/print flags around BluetoothPrint and CreatePos launch.
2. **Reports / Nav facades** — ReportsHub → ReportResolver; Home QA ∩ NavigationRegistry.
3. **Forms / validators** — product name/price via FieldValidator; unit visibility.
4. **Printer** — `UniversalPrinterEngine.addressForRole`.
5. **Defer** — bottom nav rewrite, dynamic list engines, dedicated repair/rental screens, USB/WiFi printers.

## Compatibility rules

- Do not delete historical data when modules disable.
- Do not apply template while cart/payment/print busy.
- Print failure must never wipe saved invoices.
- Prefer FeatureEngine ∩ SecurityPermissions over hardcoded business ifs in UI.
