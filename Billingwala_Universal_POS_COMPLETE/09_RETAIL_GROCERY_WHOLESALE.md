# 09 Retail Grocery Wholesale

Retail / grocery / wholesale on Fast Billing + inventory + barcode/wholesale flags.

## Facade
`Extra/RetailGroceryModule.java` — family detect, `BARCODE`, `WHOLESALE_PRICING`, tiers, barcode scan.

## Wired (CreatePos)
When `BARCODE` on:
- Search hint: scan barcode
- Enter / camera → exact `productCode` → add to cart

When `WHOLESALE_PRICING` on:
- Edit Product → **Wholesale price tiers** (min qty + price)
- CreatePos: tier picker (Retail + Qty N+ @ price) as pseudo portions
- Table `product_price_tier` (DB v36 + `tierNetworkStatus`)
- Cloud sync: upload/download via `insertProductPriceTier.php` / `getProductPriceTierList.php`

## Server
| File | Role |
|------|------|
| `API/insertProductPriceTier.php` | Upsert by `userId` + `localTierId` |
| `API/getProductPriceTierList.php` | Download list for POS fetch chain |
| `API/migrations/p29_product_price_tier.sql` | `product_price_tiers` table |

Product resolve on server uses `productNetworkStatus` (same as portions). Soft-delete tiers re-upload as pending.

## Live reuse
`productCode` (SKU), inventory, Fast Billing wire `invoiceType=fast_billing`.
