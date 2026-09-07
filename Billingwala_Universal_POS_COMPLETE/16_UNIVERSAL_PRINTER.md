# 16 Universal Printer

Bluetooth bill + KOT (Woosim/SPP). Optional BOT for bar: split tickets; optional dedicated **BOT Bluetooth** (`bluetoothBotAddress`, fallback = KOT MAC).

## Facade
`Extra/UniversalPrinterEngine.java`

## Live
`BluetoothPrint`, `CompanyPrinterSetting`, `WoosimPrnMng`, `KOTWoosimPrnMng`, `BluetoothPrinterChannel`.

## Hard rule
**Print failure never deletes a saved bill.**

## Settings (`CompanyPrinterSetting`)
| Control | Notes |
|---------|--------|
| KOT enable / prefix / copies / auto-print | Kitchen tickets |
| BOT enable | Visible only when template has `bot` (Bar+Restaurant) |
| BOT printer MAC | Optional dedicated bar printer; empty = same as KOT |
| BOT prefix | Ticket number prefix on bar tickets (default `BOT-`) |

## BOT routing
1. Kitchen → KOT BT  
2. Bar → BOT BT when set, else same KOT channel  
3. Empty `bluetoothBotAddress` = prior single-printer behaviour  
4. Bar ticket header uses `botPrefix` when printing BOT batch
