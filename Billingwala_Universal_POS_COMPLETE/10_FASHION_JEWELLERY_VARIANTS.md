# 10 Fashion Jewellery Variants

Clothing / footwear / jewellery variants (size, color, SKU). **Not** F&B portions.

## Facade
`Extra/FashionJewelleryModule.java` — `VARIANTS` flag, `product_variant` table, cart key `var:{id}`.

## Wired
- DB v37 `product_variant` (+ `variantNetworkStatus`)
- Edit Product → **Size / Color variants** link when flag on
- CreatePos picker reuses portion UI with pseudo-portions
- Cart line shows size/color via `portionName` / `portionId=var:…`
- Cloud sync: upload/download via `insertProductVariant.php` / `getProductVariantList.php`

## Server
| File | Role |
|------|------|
| `API/insertProductVariant.php` | Upsert by `userId` + `localVariantId` |
| `API/getProductVariantList.php` | Download list for POS fetch chain |
| `API/migrations/p30_product_variant.sql` | `product_variants` table |

Product resolve on server uses `productNetworkStatus` (same as portions/tiers). Soft-delete variants re-upload as pending.

## Safe rules
Do not write apparel variants into `product_portion`.
Restaurant templates: flag off → unchanged.
