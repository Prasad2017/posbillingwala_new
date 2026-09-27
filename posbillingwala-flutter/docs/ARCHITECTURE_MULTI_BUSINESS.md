# POS Billingwala — One Software, All Business Types

## Deep wiring (complete)

| Area | Behaviour |
|------|-----------|
| Barcode / hold-resume | `/pos/scan`, hold cart, `/pos/held` |
| Offers / BOGO / modifiers | Coupons + BOGO; cart add-on picker (`type=modifier`) |
| Kitchen KOT routing | One KOT per kitchen dept (match category/code) |
| Warehouse stock | WH A→B ledger on transfer RECEIVED; Stock tab |
| Serial / lots / returns / approvals / loyalty / credit / folio / night audit | As previously wired |

## Kitchen routing rule

Create kitchen departments with `type=kitchen`. Match product **category name** to dept **name**, or product **code** to dept **code**.

## Warehouse stock

Transfers with `fromWarehouse` / `toWarehouse` / `productId` / `qty` → on **RECEIVED** move ledger qty and post branch stock-in. View balances under **Warehouses → Stock**.
