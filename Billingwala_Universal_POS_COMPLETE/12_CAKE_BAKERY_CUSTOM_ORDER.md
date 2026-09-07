# 12 Cake Bakery Custom Order

Bakery / cake custom orders. Reuse portions/takeaway where useful.

## Facade
`Extra/CakeBakeryModule.java` — `CUSTOM_ORDERS` flag.

## Wired
- CreatePos: custom order sheet (note, delivery date, optional deposit)
- **Attach reference photo** → `filesDir/custom_orders/` · `| Photo:…` on line
- **Deposit ledger** (`custom_order_deposit`) when deposit entered — Master Data long-press
- Skip available for walk-in retail cakes
- Cloud sync: `UserSynchronizeData` → `insertCustomOrderDeposit.php` (metadata; photo binary stays local)
- **Print photo on bill**: 2″/3″ layouts embed local photo under line items; filename stripped from printed name

## Server
| File | Role |
|------|------|
| `API/insertCustomOrderDeposit.php` | Upsert by `userId` + `localDepositId` |
| `API/getCustomOrderDepositList.php` | Download list for POS fetch chain |
| `API/Owner/getCustomOrderDepositList.php` | Owner list by `licenceId` |
| `API/Owner/updateCustomOrderDepositStatus.php` | Owner status (`open`/`applied`/`refunded`) |
| `API/migrations/p28_custom_order_deposit.sql` | `custom_order_deposits` table |

POS: upload on cloud sync; download via `CustomOrderDepositWorker`. Mark `synced` only when upload `status == "1"`.

## Owner UI
**Settings → Outlet deposits** — ledger for a franchise outlet (All / Open only). Tap → mark applied / refunded / open. Photo binary stays on POS.
