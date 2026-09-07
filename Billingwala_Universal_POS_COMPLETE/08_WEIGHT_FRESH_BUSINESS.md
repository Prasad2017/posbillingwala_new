# 08 Weight Fresh Business

Weight/fresh (fish, meat, chicken, vegetable, fruit). Reuse Fast Billing + inventory.

## Facade
`Extra/WeightFreshModule.java` — `WEIGHT_SCALE` flag, KG/GRAM helpers, `shouldPromptWeight`, `lineAmount`, SPP BT scale.

## Wired (CreatePos)
When template has `weight_scale` and product unit is KG/GRAM (and not open-price):
- Bottom sheet **Enter Weight** (rate × weight)
- **Read from Bluetooth scale** (SPP ASCII) — first tap picks paired device; long-press re-picks
- Cart stores **unit rate** in price and **weight** in quantity

## Wired (Settings)
When weight template is on: **Bluetooth scale** row — pick / test read / clear MAC (`bluetoothScaleAddress`).

## Scale parsing (SPP ASCII — production path)
Common lines: `1.250`, `1,250 kg`, `ST,GS,+  1.250kg`, `N.W.:1.234kg`, `Net 1234 g`, grams → kg.  
Read window ~2.2s. Vendor proprietary SDKs are optional only if a scale does not speak SPP ASCII.

## Safe rules
- Restaurant default: flag off → no weight sheet / no Settings scale row
- Empty scale MAC → keypad only (prior behaviour)
- Template: `weight_fresh_default`
