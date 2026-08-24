# Combo API requirements (Owner / Admin / Dealer)

**Status:** POS Android + POS PHP (`insertCombo.php`, `insertComboItem.php`, `insertInvoiceComboItem.php`) are implemented.  
**Owner / Admin / Dealer Android UI is out of scope** this pass. This file is the contract those apps must follow when they add Combo masters.

Combo is a **separate catalog master**. Do **not** insert combo rows into `products`. Do **not** change Portion Master APIs.

## Deploy first

Run `API/migrations/p9_combo_items.sql` (or `server_upgrade_all.sql`) **before** any client uploads combo rows. Tables: `combos`, `combo_items`, `invoice_combo_items`. Extra columns on `invoice_final_product`: `invoiceItemType`, `comboNetworkStatus`, `snapshotComboComponents`.

## Auth (same as existing customer catalog)

| App | Auth helper | Role |
|-----|-------------|------|
| Owner | `auth_user_id_from_request($con, $userId, 'owner')` | owner |
| Admin | `auth_user_id_from_request($con, $userId, 'admin')` | admin |
| Dealer | `auth_user_id_from_request($con, $userId, 'dealer')` | dealer |

Mirror `Owner/insertCustomerPortion.php`: POST form fields, JSON `{status,message}` (`1` / `0`), prepared statements via `db_prepared.php`.

Suggested filenames (not implemented):

| Endpoint | Purpose |
|----------|---------|
| `insertCustomerCombo.php` | Upsert combo master |
| `insertCustomerComboItem.php` | Upsert combo component |
| `getCustomerComboList.php` | List combos for a shop `userId` |
| `getCustomerComboItemList.php` | List combo items for a shop `userId` |

Invoice combo snapshots stay POS-only (`insertInvoiceComboItem.php`). Owner/Admin/Dealer should not rewrite historical bills.

## `insertCustomerCombo.php`

Idempotent upsert on `comboNetworkStatus`.

| Field | Required | Notes |
|-------|----------|-------|
| `userId` | yes | Target shop (`products.userId` / `combos.userId`) |
| `comboNetworkStatus` | yes | Client-generated unique key |
| `comboName` | yes | Trimmed name |
| `comboPrice` | yes | Manual selling price, numeric `> 0`. **Never** sum of components |
| `comboCode` | no | Optional SKU |
| `comboCGST` / `comboSGST` | no | Copied onto POS cart line |
| `comboWithGSTPrice` | no | Server may compute if omitted |
| `comboActiveStatus` | no | `'1'` POS-visible, `'0'` hidden. Default `'1'` |
| `comboDeletedStatus` | no | `'1'` / `deactive` → `comboStatus=deactive` (soft delete) |
| `comboSortOrder` | no | Default `0` |

Response: `{status, message, comboId}`.

## `insertCustomerComboItem.php`

Idempotent upsert on `comboItemNetworkStatus`. Resolve parents like `insertPortion.php`:

1. Combo: `comboNetworkStatus` (preferred) then `comboId`
2. Product: `productNetworkStatus` (preferred) then `productId`
3. Portion: `portionNetworkStatus` optional. If the product has portions, a matching `product_portions` row is required. If the product has no portions, send empty portion.

| Field | Required | Notes |
|-------|----------|-------|
| `userId` | yes | Shop id |
| `comboItemNetworkStatus` | yes | Client key |
| `comboNetworkStatus` | yes* | Required unless server `comboId` is known |
| `productNetworkStatus` | yes* | Required unless server `productId` is known |
| `comboItemQuantity` | yes | Numeric `> 0` (qty per 1 combo) |
| `portionNetworkStatus` | if product has portions | Must belong to that product |
| `comboItemSortOrder` | no | Default `0` |
| `comboItemDeletedStatus` | no | `'1'` soft-deletes the component |

Unique among non-deleted: `(comboId, productId, IFNULL(portionId,''))`.

Response: `{status, message, comboItemId}`.

## Rules locked with POS

- At least one active combo item.
- Combo selling price is **manual**.
- Variable combos (“choose any drink”) are out of scope.
- Editing a combo master must **not** rewrite old `invoice_final_product` / `invoice_combo_items` snapshots.
- Inventory is product-level only. A combo sale deducts **components** (`combo qty × component qty`), never a combo `productId`.
- Bill line `invoiceItemType=COMBO`; unit price is the frozen combo price. Components are not extra invoice lines and are not priced on the bill.

## GET list shape (for those apps)

Match POS:

```json
{ "comboResponse": [ { "comboId", "comboName", "comboCode", "comboPrice", "comboCGST", "comboSGST", "comboActiveStatus", "comboDeletedStatus", "comboNetworkStatus", "comboSortOrder" } ] }
```

```json
{ "comboItemResponse": [ { "comboItemId", "comboNetworkStatus", "productNetworkStatus", "portionNetworkStatus", "comboItemQuantity", "comboItemSortOrder", "comboItemDeletedStatus", "productName", "portionName" } ] }
```

`comboDeletedStatus` / `comboItemDeletedStatus` are `'0'` / `'1'` (server `active` / `deactive`).
