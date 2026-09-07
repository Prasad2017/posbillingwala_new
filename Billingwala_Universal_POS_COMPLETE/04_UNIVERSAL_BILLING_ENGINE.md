# 04 Universal Billing Engine

Shared adapter over existing cart/invoice modes. **Do not rename** legacy `cartOrderStatus` / `invoiceType` wire values. **Do not rewrite** `BluetoothPrint.saveInvoice` — wrap and route only.

---

## Legacy wire values (frozen)

| Mode | Wire string | CreatePos cart? | Feature flag |
|------|-------------|-----------------|--------------|
| Fast Billing | `fast_billing` | Yes | `fast_billing` |
| Dine-In | `table_wise` | Yes | `dine_in` |
| Takeaway | `take_away` | Yes | `take_away` |
| Mess | `mess` | No (parallel domain) | `mess` |

Unknown / empty cart status → treat as `fast_billing` (same as prior save fallback).

## Architecture

```text
Home / MainActivity / CreatePos
        ↓  BillingMode.wireValue
cart_product.cartOrderStatus
        ↓  checkout
UniversalBillingEngine.invoiceTypeForCartSave(...)
        ↓
BluetoothPrint.saveInvoice  →  invoice.invoiceType
        ↓
Reports / sync (unchanged string filters)
```

Mess stays on `InvoiceMess` + mess tables — not the cart save path.

## Classes

| Class | Role |
|-------|------|
| `BillingMode` | Enum + frozen wire values |
| `UniversalBillingEngine` | `canStart`, mode resolve, invoiceType mapping, line helpers, GST/total helper |
| `CartItemType` | PRODUCT / COMBO (existing) |
| `BillLineSnapshot` | Line name/price snapshots (existing) |

## Wired call sites

| Site | Change |
|------|--------|
| `Home` mode clicks | `UniversalBillingEngine.canStart` + `BillingMode` wire values |
| `MainActivity` post-print restore | `BillingMode.fromWire` switch |
| `BluetoothPrint.saveInvoice` | `invoiceTypeForCartSave` + `isTableWise` checks |
| `DineInTableHelper.CART_ORDER_TABLE` | `BillingMode.TABLE.getWireValue()` |

## Helpers (for later modules)

- `displayLineName` / `resolvedLinePrice` → existing cart snapshot APIs  
- `isComboLine` / `normalizeLineType`  
- `isGstOn` → FeatureEngine GST  
- `computeGrandTotal` → same formula as saveInvoice  

## Safe rules

1. Never change stored strings `fast_billing` / `table_wise` / `take_away` / `mess`  
2. Keep offline-first save; print failure must not wipe bill  
3. Mess QR / membership remains separate  
4. New business types (retail/weight) reuse `fast_billing` cart until specialised modes exist  
5. No DB migration in this phase  

## Done

- Spec + `BillingMode` + `UniversalBillingEngine`  
- Home / MainActivity / BluetoothPrint / dine-in constant wired  
- Save path behaviour unchanged for restaurant default  

## Next

**05 Restaurant Food Module** — formalize current dine-in/KOT/portions as the first template implementation (extract, don’t rewrite)  
or **07 Mess QR Token** — already strong; document + FeatureEngine gates.
