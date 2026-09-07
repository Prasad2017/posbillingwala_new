# 02 All Business Types

Catalog of business types for Universal POS. Each type maps to a **default template** and readiness. **Do not break restaurant/mess billing.**

---

## Readiness

| Label | Meaning |
|-------|---------|
| **Live** | Engines + billing path shipped for this type |
| **Partial** | Template selectable; specialised extras may map to a nearest engine |
| **Planned** | Catalogued; nearest template until its module ships |

## Priority 1 (selectable now)

| Type id | Display | Template | Engines / notes |
|---------|---------|----------|-----------------|
| `restaurant` | Restaurant / Food | `restaurant_default` | Tables, KOT, portions, combos, takeaway, mess — **Live** |
| `bar_restaurant` | Bar + Restaurant | `bar_restaurant_default` | Restaurant + BOT split print — **Live** (doc 06) |
| `mess` | Mess / Tiffin | `mess_focused` | Membership + QR — **Live** (doc 07) |
| `retail` | Retail / General Shop | `retail_default` | Fast + inventory + barcode/camera — **Live** (doc 09) |
| `grocery` | Grocery / Kirana | `grocery_default` | Retail + barcode/inventory — **Live** (doc 09) |
| `weight_fresh` | Weight / Fresh | `weight_fresh_default` | Weight keypad + optional BT scale — **Live** (doc 08) |
| `wholesale` | Wholesale | `wholesale_default` | Qty tiers + cloud sync — **Live** (doc 09) |

## Priority 2 (selectable; modules shipped)

| Type id | Display | Template | Status |
|---------|---------|----------|--------|
| `fashion` | Clothing / Footwear | `fashion_default` | **Live** — variants + cloud sync (doc 10) |
| `jewellery` | Jewellery | `jewellery_default` | **Live** — variants + cloud sync (doc 10) |
| `salon` | Salon / Beauty / Spa | `salon_default` | **Live** — appointments + cloud sync (doc 11) |
| `bakery` | Bakery / Cake Shop | `bakery_default` | **Live** — custom order + deposit + bill photo (doc 12) |

## Priority 3 (selectable → nearest live template)

| Type id | Maps to | Readiness |
|---------|---------|-----------|
| `electronics`, `hardware`, `stationery`, `pet_shop`, `rental` | `retail_default` | Partial |
| `laundry`, `car_wash`, `repair`, `healthcare` | `salon_default` | Partial |
| `custom` | `restaurant_default` (or custom JSON) | Partial |

Selectable in POS Settings + Admin/Dealer/Owner template pickers (`BusinessTemplateChoices` / `BusinessTypeCatalog.selectable()`).

## Aliases (normalize → primary)

Examples: `fish`/`meat`/`veg`/`fruit` → `weight_fresh`; `clothing`/`footwear` → `fashion`; `beauty`/`spa` → `salon`; `kirana` → `retail`; `cafe`/`hotel` → `restaurant`.

## Code

| Class | Role |
|-------|------|
| `BusinessTypes` | Ids + alias normalize |
| `BusinessTypeInfo` / `BusinessTypeReadiness` | Catalog row metadata |
| `BusinessTypeCatalog` | Full list + `selectable()` |
| `BusinessTemplateRegistry` | Templates for priority 1–2 |
| `BusinessTemplateEngine.applyBusinessType` | Settings applies type → template |

## UI

- **POS Settings → Business Template** — Priority 1–3 with `[Live|Partial]`
- **Owner / Admin / Dealer** — same full list (P3 maps labeled)

## Safe rules

1. Default remains `restaurant` / `restaurant_default`  
2. Changing type gates FeatureEngine visibility; wire strings stay `fast_billing` / `table_wise` / `take_away` / `mess`  
3. No invoiceType renames  

## Next

Live schema + PHP deploy is **done**. Run smoke 1–26 in `23_TESTING_AND_MIGRATION.md`.
Optional only: proprietary scale vendor SDK.
Web admin template UI: Add/Edit customer & licence (`BusinessTemplateSupport`).
