# Billingwala Universal POS Dynamic

Implementation package for Cursor.

## Status (2026-09-07)

Phases **01–13 executed** on WithTable (wiring pass — not a full UI rewrite). Close-out items from `KNOWN_ISSUES` wired where safe.

| Doc | File |
|-----|------|
| Master requirement | `00_MASTER_REQUIREMENT.md` |
| Execution order | `MASTER_CURSOR_EXECUTION_PROMPT.md` |
| Audit | `COMPLETE_POS_AUDIT.md` |
| Plan | `DYNAMIC_POS_IMPLEMENTATION_PLAN.md` |
| Test report | `COMPLETE_POS_TEST_REPORT.md` |
| Known issues | `KNOWN_ISSUES.md` |
| Registries | `WithTable/.../Extra/dynamic/*` |
| UI resolve | `WithTable/.../Extra/dynamicui/*` |

### This execution pass

- Phase 01 docs created/refreshed  
- ConfigApplyGuard payment + print wired on `BluetoothPrint` / CreatePos pay launch  
- Home quick actions ∩ `NavigationRegistry`  
- Reports hub/settings → `ReportResolver`  
- Product form visibility + `FieldValidator` on save gates  
- `UniversalPrinterEngine.addressForRole` for role→MAC routing  
- **Home deepen:** `HomeDynamicQuickActions` chip row ∩ NavigationRegistry; Repair/Rental chips open hubs  
- **P3 verticals:** Electronics / Repair / Rental prompts; Repair & Rental hubs (new bill / appointments|deposits / recent notes)  
- **Printer:** role routing + connection-type picker/persist (`PrinterConnectionType`)  
- **Lists:** `DynamicListRegistry` + ProductMaster barcode search hint  
- **Nav sheet:** long-press Home settings → `DynamicNavigationSheet`  
- **Mess walk-in:** `MessWalkInTokenActivity` in manifest  
- **ScreenRegistry:** soft visibility + `guardOpen` on Home catalog and Settings hubs  

### Still deferred

See `KNOWN_ISSUES.md` (bottom nav rewrite, full billing layout remount, USB/WiFi print stacks, per-column list adapters).

Start fresh audits with `01_AUDIT_PROMPT.md`; reuse `Extra/dynamic` registries for new UI wiring.
