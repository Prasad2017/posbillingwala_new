# 06 Bar Restaurant Module

Bar + Restaurant on top of the live food module. Reuse tables, KOT, portions, combos. **BOT** split when printer `botEnable` is on.

## Wired
1. Template `bar_restaurant` includes `bot`
2. Printer `botEnable` (default **off**) + `botPrefix` + optional **`bluetoothBotAddress`**
3. `BarRestaurantModule.partitionCartByRoute` — beverage → bar, else kitchen
4. `BluetoothPrint` — kitchen on KOT BT, then bar on **BOT BT** when set (else same as KOT)

## Ticket routing

| Food type code | Route when BOT on | Route when BOT off |
|----------------|-------------------|--------------------|
| `beverage` | `BAR_BOT` | `KITCHEN_KOT` |
| `food` / other / combo | `KITCHEN_KOT` | `KITCHEN_KOT` |

## Printer settings (Bar template)
- Enable BOT
- **BOT printer** — pick paired device (optional; blank = use KOT printer)
- Clear — fall back to KOT MAC

## Safe rules
1. Restaurant default: BOT off → identical to today
2. Empty `bluetoothBotAddress` → same KOT printer (prior behaviour)
3. Additive SQLite only (`botEnable` / `botPrefix` / `bluetoothBotAddress`)
