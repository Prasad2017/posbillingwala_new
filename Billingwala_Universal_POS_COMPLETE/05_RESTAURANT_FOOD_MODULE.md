# 05 Restaurant Food Module

Formalize the **live** restaurant/food POS path as the first business template implementation. **Extract / wrap — do not rewrite** CreatePos or `BluetoothPrint.saveInvoice`.

---

## Catalog hierarchy (live)

```text
Food Type (Food | Beverage)
  → Category
    → optional Subcategory
      → Product
        → Portion(s)  [Half / Full / Kg … via Portion Master]
+ Combo master (separate from product)
```

Seed codes: `food`, `beverage` (`FoodTypeResponse.CODE_*`).

## Order modes (restaurant template)

| Mode | Wire | Entry | Notes |
|------|------|-------|-------|
| Fast | `fast_billing` | Home → CreatePos | Random `FS…` cart key |
| Dine-In | `table_wise` | Home → floor → CreatePos | Sessions, KOT, hold |
| Takeaway | `take_away` | Home → parcels → CreatePos | Parcel numbers |
| Mess | `mess` | Home → InvoiceMess | Sibling domain (doc 07) |

## Capability map → FeatureEngine

| Capability | Flag / gate | Live classes |
|------------|-------------|--------------|
| Fast / Dine / Take / Mess | licence ∩ template | `RestaurantFoodModule.can*` |
| Tables floor | `tables` ∩ `company.tableStatus` | `InvoiceCompanyTable`, `DineInTableHelper` |
| KOT | `kot` ∩ printer `kotEnable` | `DineInKotHelper`, CreatePos KOT button |
| Portions | `portions` (Master UI) | Product portion picker always if product has rows |
| Combos | `combos` | CreatePos Combos tab |
| GST | `gst` ∩ shop | Print/save still read company row |

## Facade

`WithTable/.../Extra/RestaurantFoodModule.java`

- `isFoodHospitalityTemplate`
- `canFastBilling` / `canDineIn` / `canTakeAway` / `canMess`
- `canKot` / `canPortions` / `canCombos` / `canTablesFloor`
- `isTableWiseCart` / `cartOrderTable`
- `foodTypeCodeFood` / `foodTypeCodeBeverage`
- `shouldOfferPortionPicker` (data wins for price)
- `moduleSummary`

## Wired

| Site | Change |
|------|--------|
| `Home` mode clicks | `RestaurantFoodModule.can*` |
| `CreatePos` dine-in bar / KOT | `isTableWiseCart` + `canKot` |
| `CreatePos` Combos tab | hidden when `canCombos` false |
| `CreatePos` portion click | `shouldOfferPortionPicker` |
| `DineInTableHelper.isKotEnabled` | delegates to `canKot` |

## Key existing files (unchanged logic)

- `CreatePos`, `InvoiceCompanyTable`, `InvoiceTakeAway`
- `DineInTableHelper`, `DineInKotHelper`, `DineInSettlementHelper`
- `ManageProductPortions`, `AddPortionMaster`, `ComboMaster`
- Tables: `dining_area`, `pos_table`, `dining_session`, `kot`, `kot_item`
- Catalog: `food_type`, `product_category`, `product_subcategory`, `product`, `portion_master`, `product_portion`, `combo*`

## Safe rules

1. Default template `restaurant_default` must behave exactly as before  
2. Do not rename food_type tables or invoice wire strings  
3. Portion rows on a product still open the picker (correct price) even if Master portions UI is hidden  
4. KOT still default-on when printer settings missing  

## Done

- Spec + `RestaurantFoodModule` facade  
- Home / CreatePos / KOT gates wired  
- Combos tab respects FeatureEngine  

## Next

Pack complete for F&B — see **06 Bar Restaurant** (live BOT split + optional BOT BT) and deploy checklist in **23**.
