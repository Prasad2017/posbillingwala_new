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

## Invoice paper layouts

| Size | Layout |
|------|--------|
| 2″ (58mm) | Compact thermal: stacked meta, Item/Qty/Rate/Amount |
| 3″ (80mm) | Wider thermal: two-column meta, same columns |
| A4 | Tax invoice preview/share; thermal print uses 80mm of same data |

Shop name, GSTIN, items, totals, UPI QR come from live shop/bill data — not sample mockups.
