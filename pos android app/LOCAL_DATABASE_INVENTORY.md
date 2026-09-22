# WithTable Local Database Inventory

Companion to [UI_SCREEN_INVENTORY.md](./UI_SCREEN_INVENTORY.md).

## Tech stack

- **Engine:** Android SQLite via `SQLiteOpenHelper`
- **Class:** `app/src/main/java/com/pos_billingwala/Database/POSBillingWalaDatabase.java`
- **File name:** `pos_billingwala_db`
- **Schema version:** `DATABASE_VERSION = 30`
- **Tables:** 30
- **Migration style:** Additive only (`CREATE TABLE IF NOT EXISTS`, `addColumnIfNotExists`) — no DROP of production tables on upgrade
- **Offline-first:** Bills, cart, masters, mess, inventory write locally first; sync flags push/pull via Retrofit + WorkManager

## High-level data flow

```
UI (Activity / Fragment)
  → POSBillingWalaDatabase (SQLite)     ← source of truth on device
  → UserSynchronizeData / OfflineNetworkData   (pending *Status = 0 → server)
  → NetworkDataFetcher workers                 (server → replace / upsert local)
```

**Fetch Data (Home):** wipe selected ops/catalog tables → download chain  
**Sync / Upload:** upload rows where local sync status is not synced (`0`)

## Sync column convention

| Pattern | Meaning |
|---------|---------|
| `*NetworkStatus` | Cloud idempotency / public key (UUID-like string) |
| `*Status` (TINYINT / `'0'`) | Local pending upload flag (`0` = pending, synced after upload) |
| `*DeletedStatus` | Soft delete (`'0'` active, `'1'` deleted) |
| `organizationId` / `branchId` / `deviceId` | Multi-branch stamp (filter at runtime; not FK constraints) |

Catalog fetch/upload uses Android **`ownerId`**. Ops (invoices, tables, mess, inventory) usually use **`userId`**.

---

## Domain map (30 tables)

| Domain | Count | Tables |
|--------|------:|--------|
| Catalog | 8 | `food_type`, `product_category`, `product_subcategory`, `portion_master`, `product_portion`, `product`, `combo`, `combo_item` |
| Cart / billing | 5 | `cart_product`, `cart_combo_item`, `invoice`, `invoice_final_product`, `invoice_combo_item` |
| Floor / dine-in | 7 | `dining_area`, `table_type`, `pos_table`, `dining_session`, `order_round`, `kot`, `kot_item` |
| Mess | 5 | `member`, `member_payment`, `mess_invoice`, `mess_token`, `mess_meal_token_queue` |
| Inventory / expenses | 2 | `inventory`, `expenses` |
| Company / printer | 2 | `company`, `company_printer_setting` |
| Queues | 1 | `invoice_product_delete_queue` |

---

## 1. Catalog

### food_type

| | |
|---|---|
| **Constant** | `FOOD_TYPE_TABLE` |
| **Purpose** | Food vs Beverage type (seeded: Food, Beverage) |
| **PK** | `foodTypeId` AUTOINCREMENT |
| **Sync** | none |

| Column | Type | Notes |
|--------|------|-------|
| `foodTypeId` | INTEGER PK | |
| `foodTypeName` | VARCHAR | |
| `foodTypeCode` | VARCHAR | UNIQUE index |
| `foodTypeSortOrder` | INTEGER | default 0 |
| `foodTypeStatus` | TINYINT | default 1 |

---

### product_category

| | |
|---|---|
| **Constant** | `PRODUCT_CATEGORY_TABLE` |
| **Purpose** | Menu categories |
| **PK** | `categoryId` |
| **Sync** | `categoryNetworkStatus`, `categoryStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `categoryId` | INTEGER PK | |
| `categoryName` | VARCHAR | |
| `foodTypeId` | INTEGER | indexed |
| `categorySortOrder` | INTEGER | |
| `categoryDeletedStatus` | VARCHAR | soft delete |
| `categoryNetworkStatus` | VARCHAR | |
| `categoryStatus` | TINYINT | pending upload |

---

### product_subcategory

| | |
|---|---|
| **Constant** | `PRODUCT_SUBCATEGORY_TABLE` |
| **Purpose** | Subcategories under a category |
| **PK** | `subcategoryId` |
| **Sync** | `subcategoryNetworkStatus`, `subcategoryStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `subcategoryId` | INTEGER PK | |
| `categoryId` | INTEGER | indexed |
| `subcategoryName` | VARCHAR | |
| `subcategorySortOrder` | INTEGER | |
| `subcategoryDeletedStatus` | VARCHAR | default `'0'` |
| `subcategoryNetworkStatus` | VARCHAR | |
| `subcategoryStatus` | TINYINT | |

---

### portion_master

| | |
|---|---|
| **Constant** | `PORTION_MASTER_TABLE` |
| **Purpose** | Shared portion names (Half / Full / …) |
| **PK** | `portionMasterId` |
| **Sync** | `portionMasterNetworkStatus`, `portionMasterStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `portionMasterId` | INTEGER PK | |
| `portionName` | VARCHAR | |
| `portionMasterDeletedStatus` | VARCHAR | |
| `portionMasterNetworkStatus` | VARCHAR | unique when set |
| `portionMasterStatus` | TINYINT | |

---

### product_portion

| | |
|---|---|
| **Constant** | `PRODUCT_PORTION_TABLE` |
| **Purpose** | Per-product portion price rows |
| **PK** | `portionId` |
| **Sync** | `portionNetworkStatus`, `portionStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `portionId` | INTEGER PK | |
| `productId` | INTEGER | indexed |
| `portionMasterId` | INTEGER | |
| `portionName` | VARCHAR | |
| `portionPrice` | VARCHAR | |
| `portionSortOrder` | INTEGER | |
| `portionDeletedStatus` | VARCHAR | |
| `portionNetworkStatus` | VARCHAR | |
| `portionStatus` | TINYINT | |

---

### product

| | |
|---|---|
| **Constant** | `PRODUCT_TABLE` |
| **Purpose** | Sellable menu products |
| **PK** | `productId` |
| **Sync** | `productNetworkStatus`, `productStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `productId` | INTEGER PK | |
| `userId` | VARCHAR | legacy owner stamp |
| `categoryId` | VARCHAR | |
| `categoryName` | VARCHAR | |
| `subcategoryId` | INTEGER | |
| `productCode` | VARCHAR | |
| `productName` | VARCHAR | |
| `productPrice` | VARCHAR | |
| `openPrice` | VARCHAR | |
| `productUnit` | VARCHAR | |
| `productCGST` | VARCHAR | |
| `productSGST` | VARCHAR | |
| `productWithGSTPrice` | VARCHAR | |
| `productDeletedStatus` | VARCHAR | |
| `productNetworkStatus` | VARCHAR | |
| `productStatus` | TINYINT | |

---

### combo

| | |
|---|---|
| **Constant** | `COMBO_TABLE` |
| **Purpose** | Combo meal master |
| **PK** | `comboId` |
| **Sync** | `comboNetworkStatus`, `comboStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `comboId` | INTEGER PK | |
| `comboName` | VARCHAR | |
| `comboCode` | VARCHAR | |
| `comboPrice` | VARCHAR | |
| `comboCGST` / `comboSGST` / `comboWithGSTPrice` | VARCHAR | |
| `comboActiveStatus` | VARCHAR | default `'1'` |
| `comboDeletedStatus` | VARCHAR | |
| `comboNetworkStatus` | VARCHAR | |
| `comboStatus` | TINYINT | |
| `comboSortOrder` | INTEGER | |

---

### combo_item

| | |
|---|---|
| **Constant** | `COMBO_ITEM_TABLE` |
| **Purpose** | Components inside a combo |
| **PK** | `comboItemId` |
| **Sync** | `comboItemNetworkStatus`, `comboItemStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `comboItemId` | INTEGER PK | |
| `comboId` | INTEGER | |
| `productId` | INTEGER | |
| `portionId` | INTEGER | nullable |
| `comboItemQuantity` | VARCHAR | |
| `comboItemSortOrder` | INTEGER | |
| `comboItemDeletedStatus` | VARCHAR | |
| `comboItemNetworkStatus` | VARCHAR | |
| `comboItemStatus` | TINYINT | |

---

## 2. Cart / billing

### cart_product

| | |
|---|---|
| **Constant** | `CART_PRODUCT_TABLE` |
| **Purpose** | Working cart lines (product or combo) |
| **PK** | `cartId` |
| **Sync** | none (local working set) |

| Column | Type | Notes |
|--------|------|-------|
| `cartId` | INTEGER PK | |
| `userId` | VARCHAR | |
| `productId` | VARCHAR | |
| `productName` | VARCHAR | |
| `productOldPrice` / `productNewPrice` | VARCHAR | |
| `productUnit` | VARCHAR | |
| `productCGST` / `productSGST` | VARCHAR | |
| `productQuantity` | VARCHAR | |
| `cartDiscount` / `cartDiscountType` | VARCHAR | |
| `cartPackingCharge` / `cartPackingChargeType` | VARCHAR | |
| `noOfTable` | VARCHAR | |
| `cartOrderStatus` | VARCHAR | e.g. table-wise |
| `cartStatus` | TINYINT | |
| `portionId` / `portionName` | VARCHAR | |
| `snapshotProductName` / `snapshotLinePrice` | VARCHAR | |
| `cartItemType` | VARCHAR | `PRODUCT` / combo |
| `comboId` / `snapshotComboComponents` | VARCHAR | |
| `diningSessionId` / `orderRoundId` | VARCHAR | |
| `kotPrinted` | VARCHAR | default `'0'` |

---

### cart_combo_item

| | |
|---|---|
| **Constant** | `CART_COMBO_ITEM_TABLE` |
| **Purpose** | Expanded combo components for a cart line |
| **PK** | `cartComboItemId` |
| **Sync** | none |

| Column | Type | Notes |
|--------|------|-------|
| `cartComboItemId` | INTEGER PK | |
| `cartId` | INTEGER | indexed |
| `comboId` | INTEGER | |
| `productId` / `productNameSnapshot` | VARCHAR | |
| `portionId` / `portionNameSnapshot` | VARCHAR | |
| `quantity` | VARCHAR | |
| `sortOrder` | INTEGER | |

---

### invoice

| | |
|---|---|
| **Constant** | `INVOICE_TABLE` |
| **Purpose** | Finalized sales bills |
| **PK** | `invoiceId` |
| **Sync** | `invoiceNetworkStatus`, `invoiceStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `invoiceId` | INTEGER PK | |
| `userId` | VARCHAR | |
| `noOfTable` | VARCHAR | |
| `invoiceNumber` | VARCHAR | |
| `customerName` / `customerMobile` / `customerEmail` / `customerAddress` | VARCHAR | |
| `invoiceDate` | VARCHAR | |
| `subTotal` / `totalGSTAmount` / `discount` / `discountType` | VARCHAR | |
| `packingCharge` / `packingChargeType` | VARCHAR | |
| `totalAmount` | VARCHAR | |
| `paymentMode` | VARCHAR | |
| `cashAmount` / `upiAmount` | VARCHAR | |
| `invoiceOrderStatus` / `invoiceType` | VARCHAR | |
| `invoiceNetworkStatus` | VARCHAR | unique when set |
| `invoiceStatus` | TINYINT | pending upload |
| `organizationId` / `branchId` / `deviceId` | VARCHAR | branch stamp |
| `diningSessionId` | VARCHAR | |
| `billPrintStatus` | VARCHAR | |

---

### invoice_final_product

| | |
|---|---|
| **Constant** | `INVOICE_PRODUCT_TABLE` |
| **Purpose** | Bill line items (product / combo snapshots) |
| **PK** | `invoiceProductId` |
| **Sync** | `invoiceProductNetworkStatus`, `invoiceProductStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `invoiceProductId` | INTEGER PK | |
| `invoiceNumber` | VARCHAR | |
| `productName` / `productPrice` / `productUnit` | VARCHAR | |
| `productCGST` / `productSGST` / `productQuantity` | VARCHAR | |
| `productStatus` | VARCHAR | |
| `invoiceProductNetworkStatus` | VARCHAR | |
| `invoiceProductStatus` | TINYINT | |
| `portionId` / `portionName` | VARCHAR | |
| `snapshotProductName` / `snapshotLinePrice` | VARCHAR | |
| `organizationId` / `branchId` / `deviceId` | VARCHAR | |
| `invoiceItemType` | VARCHAR | default `PRODUCT` |
| `comboId` / `snapshotComboComponents` | VARCHAR | |

---

### invoice_combo_item

| | |
|---|---|
| **Constant** | `INVOICE_COMBO_ITEM_TABLE` |
| **Purpose** | Combo component breakdown on billed lines |
| **PK** | `invoiceComboItemId` |
| **Sync** | `invoiceComboItemNetworkStatus`, `invoiceComboItemStatus` |

| Column | Type | Notes |
|--------|------|-------|
| `invoiceComboItemId` | INTEGER PK | |
| `invoiceNumber` | VARCHAR | |
| `invoiceProductNetworkStatus` | VARCHAR | parent line key |
| `comboId` / `comboNetworkStatus` | VARCHAR | |
| `productId` / `productNameSnapshot` | VARCHAR | |
| `portionId` / `portionNameSnapshot` | VARCHAR | |
| `quantity` | VARCHAR | |
| `sortOrder` | INTEGER | |
| `invoiceComboItemNetworkStatus` | VARCHAR | |
| `invoiceComboItemStatus` | TINYINT | |

---

## 3. Floor / dine-in

### dining_area

| | |
|---|---|
| **Constant** | `DINING_AREA_TABLE` |
| **Purpose** | Floor zones (AC, Garden, …) |
| **Sync** | `areaNetworkStatus`, `areaStatus` |

Columns: `areaId`, `areaName`, `areaSortOrder`, `areaActive`, `areaNetworkStatus`, `areaStatus`, `organizationId`, `branchId`, `deviceId`

---

### table_type

| | |
|---|---|
| **Constant** | `TABLE_TYPE_TABLE` |
| **Purpose** | Capacity types (2/4/6 Seater, …) |
| **Sync** | `tableTypeNetworkStatus`, `tableTypeStatus` |

Columns: `tableTypeId`, `tableTypeName`, `defaultCapacity`, `tableTypeActive`, `tableTypeNetworkStatus`, `tableTypeStatus`, `organizationId`, `branchId`, `deviceId`

---

### pos_table

| | |
|---|---|
| **Constant** | `POS_TABLE_TABLE` |
| **Purpose** | Physical restaurant tables |
| **Sync** | `posTableNetworkStatus`, `posTableStatus` |

Columns: `tableId`, `tableNumber`, `tableName`, `tableTypeId`, `capacity`, `areaId`, `tableActive`, `positionX`, `positionY`, `sortOrder`, `statusOverride`, `posTableNetworkStatus`, `posTableStatus`, `organizationId`, `branchId`, `deviceId`

---

### dining_session

| | |
|---|---|
| **Constant** | `DINING_SESSION_TABLE` |
| **Purpose** | Open dine-in visit (joins, guests) |
| **Sync** | none (device-local; some builds stamp network fields in app logic) |

Columns: `sessionId`, `primaryTableNumber`, `joinedTableNumbers`, `sessionStatus`, `guestCount`, `startedAt`, `closedAt`, `customerName`, `customerMobile`, `waiterName`, `unpaidInvoiceNumber`, `sessionVersion`, `organizationId`, `branchId`, `deviceId`, `paidAmount`

---

### order_round

| | |
|---|---|
| **Constant** | `ORDER_ROUND_TABLE` |
| **Purpose** | Ordering rounds within a session |
| **Sync** | none |

Columns: `orderRoundId`, `sessionId`, `roundNumber`, `createdAt`, `kotId`, `organizationId`, `branchId`, `deviceId`

---

### kot

| | |
|---|---|
| **Constant** | `KOT_TABLE` |
| **Purpose** | Kitchen order ticket header |
| **Sync** | none |

Columns: `kotId`, `sessionId`, `orderRoundId`, `kotNumber`, `tableNumber`, `printStatus`, `createdAt`, `kitchenName`, `organizationId`, `branchId`, `deviceId`

---

### kot_item

| | |
|---|---|
| **Constant** | `KOT_ITEM_TABLE` |
| **Purpose** | KOT printed lines |
| **Sync** | none |

Columns: `kotItemId`, `kotId`, `cartId`, `productName`, `productQuantity`, `portionName`, `organizationId`, `branchId`, `deviceId`

---

## 4. Mess

### member

| | |
|---|---|
| **Constant** | `MEMBER_TABLE` |
| **Purpose** | Mess members |
| **Sync** | `memberNetworkStatus`, `memberStatus` |

Columns: `memberId`, `memberName`, `memberAddress`, `memberMobileNumber`, `memberAlternetMobileNumber`, `memberNetworkStatus`, `memberStatus`, `registrationNo`, `memberType`, `rollNo`, `college`, `studentYear`, `company`

---

### member_payment

| | |
|---|---|
| **Constant** | `MEMBER_PAYMENT_TABLE` |
| **Purpose** | Mess fee / period payments |
| **Sync** | `paymentNetworkStatus`, `paymentStatus` |

Columns: `paymentId`, `memberId`, `memberName`, `paymentMessAmount`, `paymentPaidAmount`, `messTotalDays`, `paymentDate`, `paymentNetworkStatus`, `paymentStatus`

---

### mess_invoice

| | |
|---|---|
| **Constant** | `MESS_INVOICE_TABLE` |
| **Purpose** | Paper mess coupons / meal invoices |
| **Sync** | `messInvoiceNetworkStatus`, `messInvoiceStatus` |

Columns: `invoiceId`, `memberId`, `memberName`, `messType`, `messInvoiceDate`, `messInvoiceNetworkStatus`, `messInvoiceStatus`

---

### mess_token

| | |
|---|---|
| **Constant** | `MESS_TOKEN_TABLE` |
| **Purpose** | Issued / verified meal tokens |
| **Sync** | `tokenNetworkStatus`/`tokenStatus`; verify: `verifyNetworkStatus`/`verifyStatus` |

Columns: `tokenId`, `tokenCode` (UNIQUE), `memberId`, `memberName`, `memberMobile`, `memberType`, `messType`, `tokenAmount`, `tokenDate`, `verifiedDate`, `tokenNetworkStatus`, `tokenState`, `tokenStatus`, `verifyNetworkStatus`, `verifyStatus`

---

### mess_meal_token_queue

| | |
|---|---|
| **Constant** | `MESS_MEAL_TOKEN_QUEUE_TABLE` |
| **Purpose** | Local print queue for server-issued meal tokens |
| **Sync** | keyed by `serverPublicId` |

Columns: `id`, `serverPublicId` (UNIQUE), `tokenNumber`, `registrationNo`, `mealSession`, `tokenDate`, `memberName`, `createdAt`, `printStatus`, `localUpdatedAt`

---

## 5. Inventory / expenses

### inventory

| | |
|---|---|
| **Constant** | `INVENTORY_TABLE` |
| **Purpose** | Stock movement ledger |
| **Sync** | `inventoryNetworkStatus`, `inventoryStatus` |

Columns: `inventoryId`, `productId`, `productInventoryQuantity`, `afterSaleInventoryQuantity`, `saleInventoryQuantity`, `inventoryDate`, `inventoryNetworkStatus`, `inventoryStatus`, `organizationId`, `branchId`, `deviceId`

---

### expenses

| | |
|---|---|
| **Constant** | `EXPENSES_TABLE` |
| **Purpose** | Shop expenses |
| **Sync** | `expensesNetworkStatus`, `expensesStatus` |

Columns: `expensesId`, `expensesName`, `expensesAmount`, `expensesDate`, `expensesNetworkStatus`, `expensesStatus`, `organizationId`, `branchId`, `deviceId`

---

## 6. Company / printer

### company

| | |
|---|---|
| **Constant** | `COMPANY_TABLE` |
| **Purpose** | Shop profile, GST, logos, hours, table flags |
| **Sync** | local settings (`companyStatus` is a local flag) |

Key columns: `companyId`, `companyName`, `cashierName`, `companyMobile`, `companyAddress`, `shopName1`/`2`, `addressLine1`–`3`, `phoneNo1`/`2`, `currencyName`, `countryName`, `stateName`, `tableStatus`, `noOfTable`, `gstStatus`, `gstNumber`, `shopCGST`/`shopSGST`, `panNumber`, `companyFssis`, `companyLogo`, `paymentLogo`, `openingMinutes`, `closingMinutes`, `companyStatus`

---

### company_printer_setting

| | |
|---|---|
| **Constant** | `PRINTER_SETTING_TABLE` |
| **Purpose** | Bill / KOT printer options |
| **Sync** | local (`settingStatus`) |

Key columns: `settingId`, `printerName`, `invoicePrefix`, `invoiceTitle`, `invoiceTermsCondition`, `logoUse`, `paymentUse`, `customerUse`, `productQuantityUpdate`, `duplicateBillUse`, `bluetoothAddress`, `bluetoothKOTAddress`, `KOTPrinterName`, `printerFeedLines`, `KotPrinterFeedLines`, `settingStatus`, `kotEnable`, `kotPrefix`, `kotCopies`, `kotAutoPrint`, `kotPreview`

---

## 7. Queues

### invoice_product_delete_queue

| | |
|---|---|
| **Constant** | `INVOICE_PRODUCT_DELETE_QUEUE_TABLE` |
| **Purpose** | Outbox of deleted invoice lines to push to cloud |
| **Sync** | flushed via `deleteInvoiceProduct.php` |

| Column | Type |
|--------|------|
| `deleteId` | INTEGER PK |
| `invoiceNumber` | VARCHAR |
| `invoiceProductNetworkStatus` | VARCHAR |

---

## Lifecycle / reset notes

| Hook | Behavior |
|------|----------|
| `onCreate` | Create all tables, seed food types, `ensureAdditiveSchema` |
| `onUpgrade` / `onOpen` | Re-run additive ensure (columns + newer tables + indexes) |
| Fetch Data wipe | Clears catalog / invoices / mess / inventory / floor ops then re-downloads |
| Branch change | Purge or claim invoice/inventory/expense rows by `branchId` |

**Not a table:** cart order label `"table_wise"` is an order-status string, not a SQLite table.

---

## Upload order (pending → server)

Rough Android `UserSynchronizeData` order:

1. categories → subcategories → products → portion_master → portions  
2. combos → combo_items  
3. printer → company  
4. invoice delete queue → invoice products → invoice combo items → invoices  
5. mess members → payments → mess invoices → mess tokens (+ verify)  
6. inventory → expenses  
7. dining areas → table types → pos_tables  

## Download order (server → local)

Rough `NetworkDataFetcher` chain:

`FoodType → Category → Subcategory → Product → PortionMaster → Portion → Combo → ComboItem → Company → Printer → DiningArea → TableType → PosTable → Invoice → InvoiceProduct → InvoiceComboItem → MessMember → MessInvoice → MessPayment → Inventory → Expenses`

---

## Notes for Flutter (Drift) rewrite

1. Mirror the same **domain groups** and sync flag semantics (`0` pending / `1` synced).
2. Catalog fetch/upload must use **`ownerId`** (fallback `userId`); floor/ops use **`userId`**.
3. Prefer one atomic catalog replace (categories + subcategories + products + portions) on Fetch.
4. Keep branch stamps on invoices, floor, KOT, inventory, expenses.
5. Include `invoice_combo_item` + invoice product delete queue for bill parity.
6. Company / printer may live in prefs + API instead of full SQLite mirrors — document any intentional divergence.

---

## Quick constant → SQL name

| Constant | SQL |
|----------|-----|
| `FOOD_TYPE_TABLE` | `food_type` |
| `PRODUCT_CATEGORY_TABLE` | `product_category` |
| `PRODUCT_SUBCATEGORY_TABLE` | `product_subcategory` |
| `PORTION_MASTER_TABLE` | `portion_master` |
| `PRODUCT_PORTION_TABLE` | `product_portion` |
| `PRODUCT_TABLE` | `product` |
| `COMBO_TABLE` | `combo` |
| `COMBO_ITEM_TABLE` | `combo_item` |
| `CART_PRODUCT_TABLE` | `cart_product` |
| `CART_COMBO_ITEM_TABLE` | `cart_combo_item` |
| `INVOICE_TABLE` | `invoice` |
| `INVOICE_PRODUCT_TABLE` | `invoice_final_product` |
| `INVOICE_COMBO_ITEM_TABLE` | `invoice_combo_item` |
| `DINING_AREA_TABLE` | `dining_area` |
| `TABLE_TYPE_TABLE` | `table_type` |
| `POS_TABLE_TABLE` | `pos_table` |
| `DINING_SESSION_TABLE` | `dining_session` |
| `ORDER_ROUND_TABLE` | `order_round` |
| `KOT_TABLE` | `kot` |
| `KOT_ITEM_TABLE` | `kot_item` |
| `MEMBER_TABLE` | `member` |
| `MEMBER_PAYMENT_TABLE` | `member_payment` |
| `MESS_INVOICE_TABLE` | `mess_invoice` |
| `MESS_TOKEN_TABLE` | `mess_token` |
| `MESS_MEAL_TOKEN_QUEUE_TABLE` | `mess_meal_token_queue` |
| `INVENTORY_TABLE` | `inventory` |
| `EXPENSES_TABLE` | `expenses` |
| `COMPANY_TABLE` | `company` |
| `PRINTER_SETTING_TABLE` | `company_printer_setting` |
| `INVOICE_PRODUCT_DELETE_QUEUE_TABLE` | `invoice_product_delete_queue` |

**Source of truth:** `POSBillingWalaDatabase.java` (`DATABASE_VERSION = 30`).
