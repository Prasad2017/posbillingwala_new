# 07 Mess QR Token

Formalize the **live** mess domain (membership + QR tokens). Parallel to CreatePos cart billing. **Do not change QR payload format.** Gate with FeatureEngine `mess`.

---

## Domain (live)

```text
Home → InvoiceMess (hub)
  ├── Members + payments (PIN-gated member list)
  ├── QR management (common branded mess QR)
  ├── Today meal tokens (queue + FCM print)
  └── Meal sessions
Walk-in token issue → MessWalkInTokenActivity → print slip
Scan / verify → MessTokenScanActivity
```

## SQLite tables

| Table | Role |
|-------|------|
| `member` | Mess members |
| `member_payment` | Membership days / fees |
| `mess_invoice` | Mess coupon invoices |
| `mess_token` | Walk-in / member QR tokens |
| `mess_meal_token_queue` | Today’s meal print queue |

## QR contract (frozen)

```text
POSBILL|v1|{tokenCode}|{userId}|{memberType}
```

- `memberType`: `walk_in` | `member`
- States: `active` → `verified`
- Helpers: `MessTokenQrHelper` (branded common QR + plain thermal QR)

## Feature gate

`FeatureFlags.MESS` = template ∩ licence (`MainActivity.mess`)

## Facade

`Extra/MessModule.java`

| API | Role |
|-----|------|
| `isEnabled` | FeatureEngine mess |
| `ensureEnabled(Activity)` | Soft finish if off |
| `openHub` | Load `InvoiceMess` |
| `walkInTokenIntent` / `scanTokenIntent` | Entry intents |
| `generateTokenCode` / `buildPayload` / `parsePayload` | QR (delegates) |
| `moduleSummary` | Ops readout |

## Wired

| Site | Change |
|------|--------|
| `Home` mess tile | `MessModule.openHub` |
| `RestaurantFoodModule.canMess` | Delegates to `MessModule` |
| `InvoiceMess` | Early exit if mess off |
| `MessWalkInTokenActivity` | `ensureEnabled` |
| `MessTokenScanActivity` | `ensureEnabled` |
| `MessQrManagementActivity` | `ensureEnabled` |

## Key existing files (logic unchanged)

- `MessTokenQrHelper`, `MessMealTokenPrintWorker`
- `MessWalkInTokenActivity`, `MessTokenScanActivity`, `MessTokenBluetoothPrint`
- `MessQrManagementActivity`, `MessMealTokenTodayActivity`, `MessMealSessionsActivity`
- `MessMemberList`, `MessMemberPaymentHistory`
- Sync: `saveMessToken` / `verifyMessToken` upload paths

## Safe rules

1. Never change `POSBILL|v1|` payload shape (scan + verify depend on it)  
2. Mess is not `invoiceType` cart billing — keep separate from `BillingMode` cart save  
3. Default restaurant template includes mess capability; licence still gates Home tile  
4. Offline token save + sync flags must keep working  

## Done

- Spec + `MessModule` facade  
- Hub / walk-in / scan / QR management gated  
- Home entry via `openHub`  

## Next

Pack complete for Mess — see **06 Bar Restaurant** (live BOT) and deploy checklist in **23**.
