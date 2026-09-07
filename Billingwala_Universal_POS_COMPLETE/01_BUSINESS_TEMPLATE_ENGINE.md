# 01 Business Template Engine

Upgrade the existing Billingwala project safely. Configuration-driven templates select which POS capabilities are available. **Do not break current restaurant billing.**

---

## Goal

One shop → one **business template**. Templates declare feature flags. Runtime visibility = **template ∩ licence** (`FeatureEngine`). Default template = today’s restaurant/mess POS.

## JSON contract (v1)

```json
{
  "version": 1,
  "id": "restaurant_default",
  "businessType": "restaurant",
  "displayName": "Restaurant / Food",
  "features": [
    "fast_billing",
    "dine_in",
    "take_away",
    "mess",
    "total_sale_data",
    "today_sale_data",
    "portions",
    "tables",
    "kot",
    "combos",
    "inventory",
    "gst"
  ]
}
```

Optional alternate shape: `"featureMap": { "fast_billing": true, ... }` (also parsed).

## Prefs (`SharedPreferences` `"user"`)

| Key | Purpose |
|-----|---------|
| `businessType` | e.g. `restaurant` |
| `businessTemplateId` | e.g. `restaurant_default` |
| `businessTemplateJson` | Optional custom overlay (cleared when applying a built-in) |
| `businessTemplateVersion` | JSON version when custom overlay saved |

Empty prefs → `BusinessSession.ensureDefaults()` writes restaurant defaults.

## Classes (`WithTable/.../Extra/`)

| Class | Role |
|-------|------|
| `BusinessTemplateEngine` | Facade: resolve / listBuiltIns / applyBuiltIn / applyJson / summaries |
| `BusinessTemplateJson` | Serialize / parse v1 JSON |
| `BusinessConfigStore` | Persist / clear custom JSON |
| `BusinessTemplateRegistry` | Built-ins; resolve prefers custom JSON → id → type |
| `BusinessSession` | Type + template id prefs |
| `FeatureEngine` | Template ∩ licence at runtime |

## Built-in templates

| Id | Type | Notes |
|----|------|-------|
| `restaurant_default` | restaurant | **Production default** — matches live Home modules |
| `bar_restaurant_default` | bar_restaurant | Restaurant + BOT split print |
| `mess_focused` | mess | Fast + Mess (hides dine-in/takeaway tiles via template) |
| `retail_default` | retail | Fast + barcode/camera + inventory |
| `grocery_default` | grocery | Retail-style + barcode focus |
| `weight_fresh_default` | weight_fresh | Weight keypad + optional BT scale |
| `wholesale_default` | wholesale | Qty price tiers (cloud sync) |
| `fashion_default` / `jewellery_default` | fashion / jewellery | Variants (cloud sync) |
| `salon_default` | salon | Appointments (cloud sync) |
| `bakery_default` | bakery | Custom order + deposit + bill photo |

## UI

**POS Settings → Store → Business Template**

- Shows current template + feature list (`•` active / `○` licence off)
- **Change template** picks a built-in
- Changing template updates Home module visibility; does not rewrite cart/invoice paths

**Owner Settings → Outlet business template**

- Pick outlet (licence) → pick built-in → save to `company_business_templates`
- POS Fetch Data downloads via `getBusinessTemplate.php` (pending local template not overwritten)

## Cloud

| Path | Role |
|------|------|
| `API/insertBusinessTemplate.php` / `getBusinessTemplate.php` | POS upload / download |
| `API/Owner/setBusinessTemplate.php` / `getBusinessTemplate.php` | Owner push / read |
| `API/migrations/p31_company_business_template.sql` | Table |

## Safe rules

1. Existing installs stay on `restaurant_default` until the user changes it  
2. Licence modules still gate Fast / Dine-In / Takeaway / Mess / sale cards  
3. Additive SQLite / MySQL only — no DROP, no wire-string renames  
4. Do not rename `invoiceType` / `cartOrderStatus` values  

## Done in this phase

- Spec + JSON contract  
- Engine / store / parse  
- Settings readout + built-in picker  
- Registry resolves custom JSON when present  
- Cloud sync: POS `insertBusinessTemplate.php` / `getBusinessTemplate.php` + Owner get/set  

## Next

Owner appointment calendar UI; keep restaurant default identical when template = `restaurant_default`.
