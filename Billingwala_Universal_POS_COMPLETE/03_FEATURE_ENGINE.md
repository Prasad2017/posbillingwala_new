# 03 Feature Engine

Unify feature resolution as **template ∩ licence ∩ company/printer toggles**. Do not break current restaurant billing, print, or Settings.

---

## Resolution order

```text
FeatureEngine.isEnabled(flag)
  1. Template supports flag?     (BusinessTemplateRegistry)
  2. Licence allows flag?        (LicenseModules / MainActivity session)
  3. Company/printer allows?     (CompanyFeatureToggles)
  → true only if all pass
```

## Flag → source map

| Flag | Template | Licence | Company / printer |
|------|----------|---------|-------------------|
| `fast_billing` | yes | `fastBilling` | — |
| `dine_in` | yes | `dineIn` | — (Home tile) |
| `take_away` | yes | `takeAway` | — |
| `mess` | yes | `mess` | — |
| `total_sale_data` / `today_sale_data` | yes | sale flags | — |
| `tables` | yes | — | `company.tableStatus` |
| `kot` | yes | — | `printer.kotEnable` (blank/missing → **on**) |
| `gst` | yes | — | `company.gstStatus` |
| `portions` / `combos` / `inventory` | yes | — | — |
| `open_price` | optional | — | **product** row only (not shop toggle) |

## Classes

| Class | Role |
|-------|------|
| `FeatureEngine` | Single resolver + `disabledReason()` |
| `CompanyFeatureToggles` | Read GST / tables / KOT from SQLite |
| `AppContexts` | Application context when callers only have a DB |
| `FeatureFlags` | Canonical keys |

## Wired call sites (Phase 03)

| Site | Behaviour |
|------|-----------|
| `Home.applyModuleVisibility` | Licence modules via FeatureEngine (unchanged for restaurant) |
| `DineInTableHelper.isKotEnabled` | Delegates to `FeatureEngine` + `kotEnable` |
| `InvoiceCompanyTable` floor load | `FeatureFlags.TABLES` (template ∩ `tableStatus`) |
| `MasterData` | Hide portions/combos by template; table master = template TABLES ∩ dine-in licence |
| `UserSetting` inventory row | `FeatureFlags.INVENTORY` |
| Business Template dialog | Shows disabled reasons (licence / GST / tables / KOT) |

## Safe rules

1. Bill GST math still reads `company.gstStatus` directly in print/save paths (same source as `FeatureEngine.GST`)
2. Do not gate Home **Dine-In** tile on `tableStatus` — only licence + template `dine_in`
3. Table Master stays visible when dine-in licence is on even if `tableStatus` is off (so shops can set up tables first)
4. KOT default-on when no printer row — matches prior live behaviour
5. No schema / API changes in this phase

## Done

- Spec + company toggle reader  
- FeatureEngine three-layer resolve  
- KOT / tables / MasterData / inventory wiring  
- Settings feature summary reasons  

## Next

**02 All Business Types** — catalog priority + which engines each type needs  
or **04 Universal Billing Engine** — abstract cart/invoice adapter without renaming legacy `invoiceType` values.
