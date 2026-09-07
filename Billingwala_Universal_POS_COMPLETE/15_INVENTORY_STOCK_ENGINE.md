# 15 Inventory Stock Engine

Formalize live stock tracking. **Do not rewrite** inventory schema or sync contracts. Sale deduct goes through a facade; FeatureEngine `inventory` gates UI and deduct.

---

## Live model

```text
inventory
  productId
  productInventoryQuantity   — reference / cumulative in
  afterSaleInventoryQuantity — current balance (sale deducts from this)
  saleInventoryQuantity      — qty sold on this movement
  inventoryDate
  inventoryNetworkStatus
```

Latest row per product: `ORDER BY inventoryId DESC LIMIT 1`.

Low stock: `afterSaleInventoryQuantity < 6`.

## Sale deduct (unchanged maths)

On invoice save, for each cart line with an inventory row:

```text
newAfter = afterSale − soldQty
append inventory row (oldIn, newAfter, soldQty)
```

Combos: deduct each component × combo qty.

## Feature gate

`FeatureFlags.INVENTORY` = template only (restaurant/retail/… include it).

When off: Settings inventory hidden; sale deduct skipped.

## Facade

`Extra/InventoryStockEngine.java`

| API | Role |
|-----|------|
| `isEnabled` / `ensureEnabled` / `openHub` | Feature + UI |
| `deductCartOnSale` | Invoice save deduct |
| `deductProduct` | Single product |
| `stockIn` | Add Inventory maths |
| `availableQty` / `isLowStock` | Read helpers |
| `isProductQuantityUpdateFlagOn` | Legacy printer flag (unused on sale historically) |

## Wired

| Site | Change |
|------|--------|
| `BluetoothPrint.saveInvoice` | `deductCartOnSale` |
| `AddInventory` | `stockIn` |
| `Inventory` fragment | `ensureEnabled` |
| `UserSetting` | `openHub` + existing FeatureEngine visibility |

## Safe rules

1. Keep append-only inventory rows (no UPDATE of prior balances)  
2. Restaurant default template has inventory → deduct behaviour unchanged  
3. Do not require `productQuantityUpdate` for deduct (legacy unused)  
4. Offline sync of inventory rows unchanged  

## Done

- Spec + engine  
- Sale deduct + stock-in via facade  
- Settings / Inventory gated  

## Next

**16 Universal Printer** or **08 Weight Fresh**.
