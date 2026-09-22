# WithTable UI Screen Inventory

Companion docs: [LOCAL_DATABASE_INVENTORY.md](./LOCAL_DATABASE_INVENTORY.md) (SQLite tables / sync).

## Tech stack

- **Not Flutter / not React** — native **Android Java** POS app
- Activities + Fragments, ViewBinding, Material Components, SQLite, Retrofit, WorkManager, Bluetooth print (Woosim), ZXing, MPAndroidChart, Lottie, AdMob, Firebase
- Package: `com.pos_billingwala` | Module: `WithTable/`
- Navigation: no Jetpack Navigation graph — `MainActivity.loadFragment()` / `startActivity()`
- i18n: EN / HI / MR via string resources

## High-level navigation flow

```
SplashScreen
  → Login (licence key) → LoginMPin → MainActivity
  → (or Register trial)
MainActivity hosts:
  Home (default)
    → Fast Billing (CreatePos) → BluetoothPrint
    → Table Billing (InvoiceCompanyTable) → CreatePos(table) → BluetoothPrint
    → Take Away (InvoiceTakeAway)
    → Mess (InvoiceMess) → members / QR / tokens / sessions
    → Catalog shortcuts / Sales / Settings
  UserSetting → MasterData / ReportsHub / Store / Ops / Support / Share / Sync
```

## Screens (76)

### SplashScreen

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `LAUNCHER` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/SplashScreen.java` |
| **Layout** | `app/src/main/res/layout/activity_splash_screen.xml` |
| **Navigation** | → Login or LoginMPin or MainActivity |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `relativeLayout` | FrameLayout | — |
| `logoIcon` | ImageView | Billingwala |

---

### Login

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `Login` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/Login.java` |
| **Layout** | `app/src/main/res/layout/activity_login.xml` |
| **Navigation** | → LoginMPin / Register |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `licenceKey` | EditText | Enter your shop licence key | textCapCharacters |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `loginLayout` | LinearLayout | — |
| `forgotLicenceKey` | AppCompatTextView | Forgot Licence key? |
| `loginCheck` | LinearLayout | — |

---

### LoginMPin

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `LoginMPin` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/LoginMPin.java` |
| **Layout** | `app/src/main/res/layout/activity_login_mpin.xml` |
| **Navigation** | → MainActivity |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `otp1` | EditText | — | numberPassword |
| `otp2` | EditText | — | numberPassword |
| `otp3` | EditText | — | numberPassword |
| `otp4` | EditText | — | numberPassword |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `loginLayout` | LinearLayout | — |
| `backToPage` | ImageView | Back |
| `loginSubtitle` | AppCompatTextView | Sign in to your POS Billingwala account. |
| `loginMpin` | LinearLayout | — |

---

### Register (Trial Signup)

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `Register` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/Register.java` |
| **Layout** | `app/src/main/res/layout/activity_register.xml` |
| **Navigation** | ← Login |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `signupName` | TextInput | — | textPersonName |
| `signupContact` | TextInput | — | phone |
| `signupShopName` | TextInput | — | textCapWords |
| `signupAddress` | TextInput | — | textPostalAddress|textMultiLine |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToLogin` | ImageView | Already have an account? Login |
| `registerLayout` | LinearLayout | — |
| `submitSignup` | AppCompatTextView | Create Free Account |

---

### MainActivity (shell)

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MainActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MainActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_main.xml` |
| **Navigation** | Hosts fragments in frameLayout; deep-link intents to billing modes |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `frameLayout` | FrameLayout | — |

---

### Home

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `default` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/Home.java` |
| **Layout** | `app/src/main/res/layout/fragment_home.xml` |
| **Navigation** | → CreatePos / InvoiceCompanyTable / InvoiceTakeAway / InvoiceMess / ProductMaster / ComboMaster / AddSubcategory / SalesOverview / SalesDashboard / UserSetting / CompanyPrinterSetting |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `linearLayout` | LinearLayout | — |
| `homeGreeting` | AppCompatTextView | Good Morning |
| `homeShopName` | AppCompatTextView | — |
| `homeNotificationBtn` | FrameLayout | — |
| `homeNotificationBadge` | TextView | — |
| `userSettingIcon` | ImageView | User Setting |
| `homeStatusPrinterRow` | LinearLayout | — |
| `homeStatusPill` | LinearLayout | — |
| `homeStatusOpen` | TextView | — |
| `homeStatusCloseTime` | TextView | — |
| `homePrinterStatusRow` | LinearLayout | Printer connection status. Tap to open printer settings. |
| `homeBillPrinterChip` | LinearLayout | — |
| `homeBillPrinterLabel` | TextView | Printer |
| `homeBillPrinterStatus` | TextView | — |
| `homeDateTime` | AppCompatTextView | — |
| `homeSalesBlock` | LinearLayout | — |
| `totalSalesCardView` | PosCardView | — |
| `todaySalesCardView` | PosCardView | — |
| `homeCatalogBlock` | LinearLayout | — |
| `subcategoryCardView` | PosCardView | — |
| `productCardView` | PosCardView | — |
| `comboCardView` | PosCardView | — |
| `fetchDataLayout` | LinearLayout | — |
| `synchronizeLayout` | LinearLayout | — |
| `homeSyncSubtitle` | TextView | Keep your data up to date |
| `inventoryCardView` | PosCardView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

---

### User Setting

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `UserSetting` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/UserSetting.java` |
| **Layout** | `app/src/main/res/layout/fragment_user_setting.xml` |
| **Navigation** | Menu hub → OrderInvoice, ReportsHub, MasterData, Store, Printer, Inventory, Expenses, Support, About, Share, CloudSync, LoginMPin (change PIN) |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |
| `menuIcon` | ImageView | — |
| `menuTitle` | TextView | Title |
| `menuSubtitle` | TextView | Subtitle |

**Includes:** `include_pos_report_toolbar`, `item_grouped_menu_row`

---

### Master Data

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `MasterData` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/MasterData.java` |
| **Layout** | `app/src/main/res/layout/fragment_master_data.xml` |
| **Navigation** | → AddCategory / AddSubcategory / AddPortionMaster / ProductMaster / ComboMaster / TableMasterActivity |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |
| `menuIcon` | ImageView | — |
| `menuTitle` | TextView | Title |
| `menuSubtitle` | TextView | Subtitle |

**Includes:** `include_pos_report_toolbar`, `item_grouped_menu_row`

---

### Order Invoice (list)

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `OrderInvoice` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/OrderInvoice.java` |
| **Layout** | `app/src/main/res/layout/fragment_order_invoice.xml` |
| **Navigation** | From Settings → invoice list |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Create POS / Fast Billing

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `CreatePos` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/CreatePos.java` |
| **Layout** | `app/src/main/res/layout/fragment_create_pos.xml` |
| **Navigation** | → BluetoothPrint; dialogs: portion, qty, share |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `productSearch` | EditText | search product by name, product code | text |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `homeCardView` | PosCardView | — |
| `home` | ImageView | Back |
| `clearCart` | TextView | Clear Cart |
| `menuIcon` | ImageView | — |
| `productSearchLayout` | LinearLayout | — |
| `catalogToggleLayout` | LinearLayout | — |
| `productLinearLayout` | LinearLayout | — |
| `backToCategory` | TextView | Back |
| `cartLayout` | LinearLayout | — |
| `viewCartButton` | TextView | View Cart |
| `kotButton` | TextView | KOT |
| `holdButton` | TextView | SAVE |

**Key display widgets**

- RecyclerView `#categoryRecyclerView`
- RecyclerView `#subcategoryRecyclerView`
- AutoFitGridRecyclerView `#productRecyclerView`

---

### Invoice Company Table (Dine-in list)

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceCompanyTable` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceCompanyTable.java` |
| **Layout** | `app/src/main/res/layout/fragment_invoice_company_table.xml` |
| **Navigation** | → CreatePos (table) / share dialog / occupied-table sheets |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `homeCardView` | PosCardView | — |
| `home` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- AutoFitGridRecyclerView `#tableRecyclerView`

**Includes:** `include_empty_list_state`

---

### Invoice Take Away

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceTakeAway` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceTakeAway.java` |
| **Layout** | `app/src/main/res/layout/fragment_invoice_take_away.xml` |
| **Navigation** | → CreatePos / BluetoothPrint / share |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `homeCardView` | PosCardView | — |
| `home` | ImageView | — |
| `menuIcon` | ImageView | — |
| `btnNewParcel` | TextView | New Parcel |
| `takeAwayOrderLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#recyclerView`

---

### Invoice Mess

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceMess` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceMess.java` |
| **Layout** | `app/src/main/res/layout/fragment_invoice_mess.xml` |
| **Navigation** | → MessMemberList / MessQrManagement / MessMealTokenToday / MessMealSessions / walk-in / PIN dialog |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `searchMessMember` | EditText | Search Mess Member | text |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `homeCardView` | PosCardView | — |
| `home` | ImageView | — |
| `menuIcon` | ImageView | — |
| `memberListLayout` | PosCardView | — |
| `qrManagementLayout` | PosCardView | — |
| `todayTokensLayout` | PosCardView | — |
| `mealSessionsLayout` | PosCardView | — |
| `messOrderLayout` | LinearLayout | — |

**Key display widgets**

- AutoFitGridRecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Bluetooth Print (Checkout)

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `BluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/BluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_bluetooth_print.xml` |
| **Navigation** | Payment + print; dialogs: discount, packing, customer, payment mode, share |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `cash` | RadioButton | Cash | — |
| `online` | RadioButton | UPI | — |
| `splitCashUpi` | RadioButton | Cash + UPI | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backCardView` | PosCardView | — |
| `backButton` | ImageView | Back |
| `clearCart` | TextView | Clear Cart |
| `menuIcon` | ImageView | — |
| `twoKOTLinearLayout` | LinearLayout | — |
| `twoLinearLayout` | LinearLayout | — |
| `twoShopCGSTLayout` | LinearLayout | — |
| `twoShopSGSTLayout` | LinearLayout | — |
| `twoDiscountLayout` | LinearLayout | — |
| `twoPackingLayout` | LinearLayout | — |
| `threeKOTLinearLayout` | LinearLayout | — |
| `threeLinearLayout` | LinearLayout | — |
| `threeShopCGSTLayout` | LinearLayout | — |
| `threeShopSGSTLayout` | LinearLayout | — |
| `threeDiscountLayout` | LinearLayout | — |
| `threePackingLayout` | LinearLayout | — |
| `cartLayout` | RelativeLayout | — |
| `cartAmountLayout` | RelativeLayout | — |
| `printInvoiceCardView` | PosCardView | — |
| `printInvoice` | ImageView | — |
| `paymentDetailLayout` | LinearLayout | — |
| `discountLayout` | LinearLayout | — |
| `paymentModeLayout` | PosCardView | — |
| `linearLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#twoKOTRecyclerView`
- RecyclerView `#twoRecyclerView`
- RecyclerView `#threeKOTRecyclerView`
- RecyclerView `#threeRecyclerView`
- RecyclerView `#cartRecyclerView`

**Includes:** `include_print_powered_by`, `include_empty_list_state`

---

### Edit Invoice

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `EditInvoice` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/EditInvoice.java` |
| **Layout** | `app/src/main/res/layout/activity_edit_invoice.xml` |
| **Navigation** | Dialogs: amount/qty, discount, packing, payment; product picker |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |
| `addProductButton` | TextView | Add Product |
| `saveButton` | TextView | Save Bill |
| `printButton` | TextView | Print Bill |

**Key display widgets**

- RecyclerView `#recyclerView`

---

### Duplicate Bluetooth Print

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `DuplicateBluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/DuplicateBluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_duplicate_bluetooth_print.xml` |
| **Navigation** | Reprint/share duplicate bill |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `cash` | RadioButton | Cash | — |
| `online` | RadioButton | UPI | — |
| `splitCashUpi` | RadioButton | Cash + UPI | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `menuIcon` | ImageView | — |
| `twoLinearLayout` | LinearLayout | — |
| `twoShopCGSTLayout` | LinearLayout | — |
| `twoShopSGSTLayout` | LinearLayout | — |
| `twoDiscountLayout` | LinearLayout | — |
| `threeLinearLayout` | LinearLayout | — |
| `threeShopCGSTLayout` | LinearLayout | — |
| `threeShopSGSTLayout` | LinearLayout | — |
| `threeDiscountLayout` | LinearLayout | — |
| `cartLayout` | RelativeLayout | — |
| `linearLayout` | LinearLayout | — |
| `paymentDetailLayout` | LinearLayout | — |
| `discountLayout` | LinearLayout | — |
| `paymentModeLayout` | LinearLayout | — |
| `cartAmountLayout` | RelativeLayout | — |
| `printInvoiceCardView` | PosCardView | — |
| `printInvoice` | ImageView | — |
| `shareInvoiceCardView` | PosCardView | — |
| `shareInvoice` | ImageView | — |

**Key display widgets**

- RecyclerView `#twoRecyclerView`
- RecyclerView `#threeRecyclerView`
- RecyclerView `#cartRecyclerView`

**Includes:** `include_print_powered_by`, `include_empty_list_state`

---

### Invoice Details Bluetooth Print

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `InvoiceDetailsBluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/InvoiceDetailsBluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_invoice_details_bluetooth_print.xml` |
| **Navigation** | Print/share invoice details |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToInvoice` | ImageView | — |
| `printInvoiceCardView` | PosCardView | — |
| `printInvoice` | ImageView | — |
| `shareInvoiceCardView` | PosCardView | — |
| `shareInvoice` | ImageView | — |
| `editInvoiceButton` | TextView | Edit Bill |
| `refundInvoiceButton` | TextView | Refund |
| `twoLinearLayout` | LinearLayout | — |
| `twoShopCGSTLayout` | LinearLayout | — |
| `twoShopSGSTLayout` | LinearLayout | — |
| `twoDiscountLayout` | LinearLayout | — |
| `threeLinearLayout` | LinearLayout | — |
| `threeShopCGSTLayout` | LinearLayout | — |
| `threeShopSGSTLayout` | LinearLayout | — |
| `threeDiscountLayout` | LinearLayout | — |
| `invoiceLinearLayout` | LinearLayout | — |
| `invoiceShopCGSTLayout` | LinearLayout | — |
| `invoiceShopSGSTLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#twoRecyclerView`
- RecyclerView `#threeRecyclerView`
- RecyclerView `#invoiceRecyclerView`

**Includes:** `include_print_powered_by`

---

### Product List Bluetooth Print

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `ProductListBluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/ProductListBluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_product_list_bluetooth_print.xml` |
| **Navigation** | Print/share product list |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `twoLinearLayout` | LinearLayout | — |
| `threeLinearLayout` | LinearLayout | — |
| `productLayout` | RelativeLayout | — |
| `linearLayout` | LinearLayout | — |
| `productPrintLayout` | RelativeLayout | — |
| `printProductCardView` | PosCardView | — |
| `printProduct` | ImageView | — |
| `shareProductCardView` | PosCardView | — |
| `shareProduct` | ImageView | — |

**Key display widgets**

- RecyclerView `#twoRecyclerView`
- RecyclerView `#threeRecyclerView`
- RecyclerView `#productRecyclerView`

**Includes:** `include_empty_list_state`

---

### Test Invoice Bluetooth Print

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `TestInvoiceBluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/TestInvoiceBluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_test_invoice_bluetooth_print.xml` |
| **Navigation** | From printer settings preview |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `connectPrinter` | TextView | Connect |

**Key display widgets**

- RecyclerView `#twoRecyclerView`
- RecyclerView `#threeRecyclerView`
- RecyclerView `#previewRecyclerView`

**Includes:** `include_print_powered_by`

---

### Coupon Bluetooth Print

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `CouponBluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/CouponBluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_coupon_bluetooth_print.xml` |
| **Navigation** | Mess coupon print |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `twoLinearLayout` | LinearLayout | — |
| `threeLinearLayout` | LinearLayout | — |
| `linearLayout` | LinearLayout | — |
| `printInvoiceCardView` | PosCardView | — |
| `printInvoice` | ImageView | — |

---

### Mess Token Bluetooth Print

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MessTokenBluetoothPrint` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MessTokenBluetoothPrint.java` |
| **Layout** | `app/src/main/res/layout/activity_mess_token_bluetooth_print.xml` |
| **Navigation** | Walk-in token print |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `printInvoiceCardView` | PosCardView | — |

**Includes:** `include_print_powered_by`

---

### Device List (BT printers)

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `DeviceListActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Print/DeviceListActivity.java` |
| **Layout** | `app/src/main/res/layout/device_list.xml` |
| **Navigation** | Pick paired/new Bluetooth device |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `closeDeviceSheet` | ImageView | @android:string/cancel |
| `button_scan` | TextView | Scan |

**Key display widgets**

- ListView `#paired_devices`
- ListView `#new_devices`

---

### Company Printer Setting

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `CompanyPrinterSetting` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/CompanyPrinterSetting.java` |
| **Layout** | `app/src/main/res/layout/activity_company_printer_setting.xml` |
| **Navigation** | → DeviceList / Test print |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `invoicePrefix` | TextInput | — | textCapCharacters |
| `printerFeedLines` | TextInput | — | numberDecimal |
| `kotPrefix` | TextInput | — | text |
| `kotCopies` | TextInput | — | number |
| `KotPrinterFeedLines` | TextInput | — | numberDecimal |
| `invoiceTitle` | TextInput | — | textCapWords |
| `invoiceTermsCondition` | TextInput | — | textCapWords|textMultiLine |
| `printerDropdown` | SearchableDropdown | Bill Printer Name | — |
| `kotPrinterDropdown` | SearchableDropdown | KOT Print Printer Name | — |
| `kotEnableSwitch` | Switch | Enable KOT | — |
| `kotAutoPrintSwitch` | Switch | Auto Print KOT | — |
| `logoSwitch` | Switch | Use Logo on Bill | — |
| `paymentSwitch` | Switch | Use Payment QR on Bill | — |
| `customerSwitch` | Switch | Use Customer Details on Bill | — |
| `productQuantityUpdate` | Switch | Product Quantity Update | — |
| `duplicateBillSwitch` | Switch | Duplicate Bill Copy (Invoice List) | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `actionButtonIcon` | ImageView | — |
| `backToSetting` | ImageView | — |
| `printerFormContainer` | LinearLayout | — |
| `connectPrinter` | TextView | Connect |
| `disconnectPrinter` | TextView | Disconnect |
| `KOTPrinterLayout` | LinearLayout | — |
| `connectKOTPrinter` | TextView | Connect |
| `disconnectKOTPrinter` | TextView | Disconnect |

**Includes:** `include_printer_section_header_printers`, `include_printer_section_header_kot`, `include_printer_section_header_bill`, `include_printer_section_header_terms`, `include_printer_section_header_preview`, `include_action_button`

---

### Table Master

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `TableMasterActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/TableMasterActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_table_master.xml` |
| **Navigation** | Bottom sheet form add/edit table |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backBtn` | ImageView | — |
| `btnAdd` | TextView | + Add Area |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Add Category

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddCategory` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddCategory.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_category.xml` |
| **Navigation** | ← MasterData; update via update_category_dialog |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `categoryName` | TextInput | — | textCapWords |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToHome` | ImageView | — |
| `addCategory` | TextView | Add Category |
| `categoryListCardView` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#categoryRecyclerview`

**Includes:** `include_empty_list_state`

---

### Add Subcategory

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddSubcategory` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddSubcategory.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_subcategory.xml` |
| **Navigation** | ← MasterData/Home; update_subcategory_dialog |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `subcategoryName` | TextInput | — | textCapWords |
| `categoryDropdown` | SearchableDropdown | Category Name | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToHome` | ImageView | — |
| `addSubcategory` | TextView | Add Subcategory |
| `subcategoryListCardView` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#subcategoryRecyclerview`

**Includes:** `include_empty_list_state`

---

### Add Portion Master

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddPortionMaster` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddPortionMaster.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_portion_master.xml` |
| **Navigation** | ← MasterData; update_portion_master_dialog |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `portionMasterName` | TextInput | — | textCapWords |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToCategory` | ImageView | — |
| `addPortionMaster` | TextView | Add Portion |
| `portionMasterListCardView` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#portionMasterRecyclerview`

**Includes:** `include_empty_list_state`

---

### Product Master

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `ProductMaster` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/ProductMaster.java` |
| **Layout** | `app/src/main/res/layout/fragment_product_master.xml` |
| **Navigation** | → AddProduct / UpdateProduct / ManageProductPortions / ProductListBluetoothPrint |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `searchProduct` | TextInput | — | text |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToHome` | ImageView | — |
| `printProductCardView` | PosCardView | Print |
| `printProduct` | ImageView | — |
| `addProduct` | TextView | Add Product |
| `linearLayout` | LinearLayout | — |
| `addProductEmpty` | TextView | Add Product |

**Key display widgets**

- RecyclerView `#productRecyclerView`

---

### Add Product

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddProduct` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddProduct.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_product.xml` |
| **Navigation** | Uses include_product_form_body + portion section |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `inlinePortionPrice` | TextInput | — | numberDecimal |
| `portionMasterDropdown` | SearchableDropdown | Select Portion | — |
| `productCode` | TextInput | — | textCapWords |
| `productName` | TextInput | — | textCapWords |
| `productPrice` | TextInput | — | numberDecimal |
| `productCGST` | TextInput | — | numberDecimal |
| `productSGST` | TextInput | — | numberDecimal |
| `categoryDropdown` | SearchableDropdown | Product Category* | — |
| `subcategoryDropdown` | SearchableDropdown | Subcategory (optional) | — |
| `unitDropdown` | SearchableDropdown | Product Unit* | — |
| `openPriceSwitch` | Switch | Open Price | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `addInlinePortion` | TextView | Add Portion to Product |
| `backToProduct` | ImageView | — |
| `addProduct` | TextView | Add Product |

**Key display widgets**

- RecyclerView `#inlinePortionRecyclerview`

**Includes:** `include_product_form_body`

---

### Update Product

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `UpdateProduct` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/UpdateProduct.java` |
| **Layout** | `app/src/main/res/layout/fragment_update_product.xml` |
| **Navigation** | Same form includes as Add Product |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `inlinePortionPrice` | TextInput | — | numberDecimal |
| `portionMasterDropdown` | SearchableDropdown | Select Portion | — |
| `productCode` | TextInput | — | textCapWords |
| `productName` | TextInput | — | textCapWords |
| `productPrice` | TextInput | — | numberDecimal |
| `productCGST` | TextInput | — | numberDecimal |
| `productSGST` | TextInput | — | numberDecimal |
| `categoryDropdown` | SearchableDropdown | Product Category* | — |
| `subcategoryDropdown` | SearchableDropdown | Subcategory (optional) | — |
| `unitDropdown` | SearchableDropdown | Product Unit* | — |
| `openPriceSwitch` | Switch | Open Price | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `addInlinePortion` | TextView | Add Portion to Product |
| `backToProduct` | ImageView | — |

**Key display widgets**

- RecyclerView `#inlinePortionRecyclerview`

**Includes:** `include_product_form_body`

---

### Manage Product Portions

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `ManageProductPortions` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/ManageProductPortions.java` |
| **Layout** | `app/src/main/res/layout/fragment_manage_product_portions.xml` |
| **Navigation** | update_portion_dialog |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `portionPrice` | TextInput | — | numberDecimal |
| `portionSortOrder` | TextInput | — | number |
| `portionMasterSpinner` | SearchableDropdown | Select Portion | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToProductMaster` | ImageView | — |
| `addPortion` | TextView | Add Portion |
| `portionListCardView` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#portionRecyclerview`

**Includes:** `include_empty_list_state`

---

### Combo Master

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `ComboMaster` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/ComboMaster.java` |
| **Layout** | `app/src/main/res/layout/fragment_combo_master.xml` |
| **Navigation** | → AddCombo / UpdateCombo |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `searchCombo` | TextInput | — | text |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToHome` | ImageView | — |
| `addCombo` | TextView | Add Combo |
| `linearLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#comboRecyclerView`

**Includes:** `include_empty_list_state`

---

### Add Combo

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddCombo` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddCombo.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_combo.xml` |
| **Navigation** | dialog_add_combo_item |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `comboCode` | TextInput | — | textCapCharacters |
| `comboName` | TextInput | — | textCapWords |
| `comboPrice` | TextInput | — | numberDecimal |
| `comboCGST` | TextInput | — | numberDecimal |
| `comboSGST` | TextInput | — | numberDecimal |
| `comboActiveSwitch` | Switch | Active on POS | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToCombo` | ImageView | — |
| `addComboItem` | TextView | Add Combo Item |
| `saveCombo` | TextView | Save Combo |

**Key display widgets**

- RecyclerView `#comboItemRecyclerView`

---

### Update Combo

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `UpdateCombo` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/UpdateCombo.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_combo.xml` |
| **Navigation** | Reuses add combo layout |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `comboCode` | TextInput | — | textCapCharacters |
| `comboName` | TextInput | — | textCapWords |
| `comboPrice` | TextInput | — | numberDecimal |
| `comboCGST` | TextInput | — | numberDecimal |
| `comboSGST` | TextInput | — | numberDecimal |
| `comboActiveSwitch` | Switch | Active on POS | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToCombo` | ImageView | — |
| `addComboItem` | TextView | Add Combo Item |
| `saveCombo` | TextView | Save Combo |

**Key display widgets**

- RecyclerView `#comboItemRecyclerView`

---

### Inventory

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `Inventory` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/Inventory.java` |
| **Layout** | `app/src/main/res/layout/fragment_inventory.xml` |
| **Navigation** | → AddInventory |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `addInventory` | TextView | Add Inventory |
| `linearLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Add Inventory

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddInventory` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddInventory.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_inventory.xml` |
| **Navigation** | ← Inventory |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `inventoryQty` | TextInput | — | numberDecimal |
| `productSpinner` | SearchableDropdown | Select Item | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToInventory` | ImageView | — |
| `addInventory` | TextView | Add Inventory |

---

### Expenses

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `Expenses` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/Expenses.java` |
| **Layout** | `app/src/main/res/layout/fragment_expenses.xml` |
| **Navigation** | → AddExpenses |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `addExpense` | TextView | Add Expense |
| `linearLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Add Expenses

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddExpenses` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddExpenses.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_expenses.xml` |
| **Navigation** | ← Expenses |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `expensesName` | TextInput | — | textCapWords |
| `expensesAmount` | TextInput | — | numberDecimal |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToExpenses` | ImageView | — |
| `addExpenses` | TextView | Add Expense |

---

### Company / Store Details

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `CompanyDetailSetting` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/CompanyDetailSetting.java` |
| **Layout** | `app/src/main/res/layout/fragment_company_detail_setting.xml` |
| **Navigation** | picture_selection_dialog for logo/QR |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `shopName1` | TextInput | — | textCapWords |
| `shopName2` | TextInput | — | textCapWords |
| `addressLine1` | TextInput | — | textCapWords |
| `addressLine2` | TextInput | — | textCapWords |
| `addressLine3` | TextInput | — | textCapWords |
| `phoneNo1` | TextInput | — | number |
| `phoneNo2` | TextInput | — | number |
| `cashierName` | TextInput | — | textCapWords |
| `noOfTable` | TextInput | — | numberDecimal |
| `countryName` | TextInput | — | textCapWords |
| `stateName` | TextInput | — | textCapWords |
| `gstNumber` | TextInput | — | textCapCharacters |
| `shopCGST` | TextInput | — | numberDecimal |
| `shopSGST` | TextInput | — | numberDecimal |
| `panNumber` | TextInput | — | textCapCharacters |
| `shopFssai` | TextInput | — | textCapCharacters |
| `upiId` | TextInput | UPI ID (e.g. shopname@upi) | textEmailAddress |
| `currencyDropdown` | SearchableDropdown | Invoice Currency | — |
| `tableSwitch` | Switch | Use Table | — |
| `gstSwitch` | Switch | GST | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `actionButtonIcon` | ImageView | — |
| `backToSetting` | ImageView | — |
| `addProfile` | ImageView | Branding |
| `tableSectionLayout` | LinearLayout | — |
| `shopGSTLayout` | LinearLayout | — |

**Includes:** `include_action_button`

---

### Reports Hub

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `ReportsHub` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/ReportsHub.java` |
| **Layout** | `app/src/main/res/layout/fragment_reports_hub.xml` |
| **Navigation** | All report fragments |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |
| `menuIcon` | ImageView | — |
| `menuTitle` | TextView | Title |
| `menuSubtitle` | TextView | Subtitle |

**Includes:** `include_pos_report_toolbar`, `item_grouped_menu_row`

---

### Report Setting (legacy menu)

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `ReportSetting` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/ReportSetting.java` |
| **Layout** | `app/src/main/res/layout/fragment_report_setting.xml` |
| **Navigation** | Same report destinations as hub |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `invoiceWiseReportLayout` | LinearLayout | — |
| `invoiceTableWiseReportLayout` | LinearLayout | — |
| `invoiceTakeAwayWiseReportLayout` | LinearLayout | — |
| `invoicePaymentWiseReportLayout` | LinearLayout | — |
| `invoiceMemberPaymentWiseReportLayout` | LinearLayout | — |
| `invoiceMessWiseReportLayout` | LinearLayout | — |
| `productWiseReportLayout` | LinearLayout | — |
| `comboWiseReportLayout` | LinearLayout | — |
| `saleWiseReportLayout` | LinearLayout | — |
| `discountWiseReportLayout` | LinearLayout | — |
| `refundWiseReportLayout` | LinearLayout | — |
| `expenseWiseReportLayout` | LinearLayout | — |
| `clearInvoiceLayout` | LinearLayout | — |

---

### Sales Dashboard

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `SalesDashboard` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/SalesDashboard.java` |
| **Layout** | `app/src/main/res/layout/fragment_sales_dashboard.xml` |
| **Navigation** | Charts/KPIs |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |

**Key display widgets**

- LineChart `#chartTrend`

**Includes:** `include_pos_report_toolbar`, `include_report_kpi_card`

---

### Sales Overview

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `SalesOverview` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/SalesOverview.java` |
| **Layout** | `app/src/main/res/layout/fragment_sales_overview.xml` |
| **Navigation** | → SalesList |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |

**Key display widgets**

- LineChart `#chartSalesTrend`

**Includes:** `include_pos_report_toolbar`, `include_report_kpi_card`

---

### Sales List

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `SalesList` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/SalesList.java` |
| **Layout** | `app/src/main/res/layout/fragment_sales_list.xml` |
| **Navigation** | ← SalesOverview |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar`, `include_empty_list_state`

---

### Sale Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `SaleReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/SaleReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | sale_wise_dialog date filter |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | sale_wise_dialog; → InvoiceProductDetails |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Table Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceTableReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceTableReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | → InvoiceTableListReport |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Table List Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceTableListReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceTableListReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_invoice_table_list_report.xml` |
| **Navigation** | table_sale_wise_dialog; table_list_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_invoice_detail_header`, `include_empty_list_state`

---

### Invoice Take Away Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceTakeAwayReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceTakeAwayReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | sale_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Payment Mode Wise Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoicePaymentModeWiseReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoicePaymentModeWiseReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | payment_mode_sale_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Discount Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceDiscountReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceDiscountReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | Likely reuses sale report layout; sale_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Refund Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceRefundReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceRefundReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | sale_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Product Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceProductReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceProductReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | product_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Expense Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceExpenseReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceExpenseReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | sale_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Invoice Mess Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceMessReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceMessReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | sale_wise_dialog |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Mess Member Report List

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceMessMemberReportList` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceMessMemberReportList.java` |
| **Layout** | `app/src/main/res/layout/fragment_operational_report.xml` |
| **Navigation** | → InvoiceMessMemberPaymentReport |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `shareInvoice` | ImageView | — |
| `menuIcon` | ImageView | — |

**Key display widgets**

- PieChart `#chartDonut`
- BarChart `#chartBar`
- RecyclerView `#recyclerView`

**Includes:** `include_pos_report_toolbar_actions`, `include_report_kpi_card`, `include_empty_list_state`

---

### Mess Member Payment Report

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceMessMemberPaymentReport` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceMessMemberPaymentReport.java` |
| **Layout** | `app/src/main/res/layout/fragment_invoice_mess_member_payment_report.xml` |
| **Navigation** | ← member report list |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `linearLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Invoice Product Details

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `InvoiceProductDetails` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/InvoiceProductDetails.java` |
| **Layout** | `app/src/main/res/layout/fragment_invoice_product_details.xml` |
| **Navigation** | From invoice report row |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToInvoice` | ImageView | — |
| `shareIcon` | ImageView | — |
| `printIcon` | ImageView | — |
| `twoLinearLayout` | LinearLayout | — |
| `twoShopCGSTLayout` | LinearLayout | — |
| `twoShopSGSTLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#twoRecyclerView`

**Includes:** `include_print_powered_by`

---

### Mess Member List

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `MessMemberList` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/MessMemberList.java` |
| **Layout** | `app/src/main/res/layout/fragment_mess_member_list.xml` |
| **Navigation** | → AddMessMember / UpdateMessMember / AddMemberPayment; add_member_dialog |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `searchMessMember` | EditText | Search Mess Member | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | — |
| `menuIcon` | ImageView | — |
| `linearLayout` | LinearLayout | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Add Mess Member

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddMessMember` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddMessMember.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_mess_member.xml` |
| **Navigation** | ← MessMemberList |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `memberName` | TextInput | — | textCapWords |
| `memberMobileNumber` | TextInput | — | number |
| `memberAlternetMobileNumber` | TextInput | — | number |
| `memberAddress` | TextInput | — | textCapWords|textMultiLine |
| `rollNo` | TextInput | — | text |
| `college` | TextInput | — | textCapWords |
| `studentYear` | TextInput | — | text |
| `company` | TextInput | — | textCapWords |
| `messAmount` | TextInput | — | numberDecimal |
| `messPaidAmount` | TextInput | — | numberDecimal |
| `registrationNo` | EditText | — | — |
| `memberTypeSpinner` | SearchableDropdown | Member Type* | — |
| `messDaySpinner` | SearchableDropdown | Mess Days* | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToMess` | ImageView | — |
| `studentFieldsLayout` | LinearLayout | — |
| `workingFieldsLayout` | LinearLayout | — |
| `addMember` | TextView | Add Member |

---

### Update Mess Member

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `UpdateMessMember` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/UpdateMessMember.java` |
| **Layout** | `app/src/main/res/layout/fragment_update_mess_member.xml` |
| **Navigation** | ← MessMemberList |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `memberName` | TextInput | — | textCapWords |
| `memberMobileNumber` | TextInput | — | number |
| `memberAlternetMobileNumber` | TextInput | — | number |
| `memberAddress` | TextInput | — | textCapWords|textMultiLine |
| `rollNo` | TextInput | — | text |
| `college` | TextInput | — | textCapWords |
| `studentYear` | TextInput | — | text |
| `company` | TextInput | — | textCapWords |
| `registrationNo` | EditText | — | — |
| `memberTypeSpinner` | SearchableDropdown | Member Type* | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToMess` | ImageView | — |
| `studentFieldsLayout` | LinearLayout | — |
| `workingFieldsLayout` | LinearLayout | — |

---

### Add Member Payment

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AddMemberPayment` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AddMemberPayment.java` |
| **Layout** | `app/src/main/res/layout/fragment_add_member_payment.xml` |
| **Navigation** | ← MessMemberList |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `memberName` | TextInput | — | textCapWords |
| `memberMobileNumber` | TextInput | — | numberDecimal |
| `messAmount` | TextInput | — | numberDecimal |
| `messPaidAmount` | TextInput | — | numberDecimal |
| `messDaySpinner` | SearchableDropdown | Mess Days* | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToMess` | ImageView | — |
| `addPayment` | TextView | Add Payment |

---

### Update Mess Payment

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `UpdateMessPayment` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/UpdateMessPayment.java` |
| **Layout** | `app/src/main/res/layout/fragment_update_mess_payment.xml` |
| **Navigation** | ← MessMemberList |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `memberName` | TextInput | — | textCapWords |
| `memberMobileNumber` | TextInput | — | numberDecimal |
| `messAmount` | TextInput | — | numberDecimal |
| `messPaidAmount` | TextInput | — | numberDecimal |
| `messPendingAmount` | TextInput | — | numberDecimal |
| `messDaySpinner` | SearchableDropdown | Mess Days* | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToMess` | ImageView | — |

---

### Mess Member Payment History

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `MessMemberPaymentHistory` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/MessMemberPaymentHistory.java` |
| **Layout** | `app/src/main/res/layout/fragment_mess_member_payment_history.xml` |
| **Navigation** | ← member |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backBtn` | ImageView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

**Includes:** `include_empty_list_state`

---

### Mess Walk-In Token

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MessWalkInTokenActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MessWalkInTokenActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_mess_walk_in_token.xml` |
| **Navigation** | → MessTokenBluetoothPrint |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `walkInName` | TextInput | — | textPersonName |
| `walkInMobile` | TextInput | — | phone |
| `walkInAmount` | TextInput | — | numberDecimal |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `issueTokenCardView` | PosCardView | — |

---

### Mess QR Management

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MessQrManagementActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MessQrManagementActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_mess_qr_management.xml` |
| **Navigation** | Generate/share/download/print/deactivate QR |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `btnGenerate` | Button | Generate QR |
| `btnShare` | Button | Share QR |
| `btnDownload` | Button | Download QR |
| `btnPrintQr` | Button | Print QR |
| `btnDeactivate` | Button | Deactivate QR |
| `backBtn` | ImageView | — |

---

### Mess Token Scan

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MessTokenScanActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MessTokenScanActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_mess_token_scan.xml` |
| **Navigation** | Camera QR scan verify |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `startScanCardView` | PosCardView | — |
| `scanResultText` | TextView | — |

---

### Mess Meal Sessions

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MessMealSessionsActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MessMealSessionsActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_mess_meal_sessions.xml` |
| **Navigation** | Inline row edit Save Session |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backBtn` | ImageView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

---

### Mess Meal Token Today

| | |
|---|---|
| **Kind** | Activity |
| **Route / entry** | `MessMealTokenTodayActivity` |
| **Class** | `app/src/main/java/com/pos_billingwala/Activity/MessMealTokenTodayActivity.java` |
| **Layout** | `app/src/main/res/layout/activity_mess_meal_token_today.xml` |
| **Navigation** | Retry print / cancel token |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backBtn` | ImageView | — |

**Key display widgets**

- RecyclerView `#recyclerView`

---

### Support Hub

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `SupportHub` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/SupportHub.java` |
| **Layout** | `app/src/main/res/layout/fragment_support_hub.xml` |
| **Navigation** | → CreateSupportTicket / MySupportTickets / dial |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `supportCallButton` | LinearLayout | — |
| `backButton` | ImageView | Back |
| `headerActionIcon` | ImageView | — |

**Includes:** `include_support_notice_card`, `include_support_urgent_help`, `include_support_hours_card`

---

### Create Support Ticket

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `CreateSupportTicket` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/CreateSupportTicket.java` |
| **Layout** | `app/src/main/res/layout/fragment_create_support_ticket.xml` |
| **Navigation** | ← SupportHub |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `subjectInput` | EditText | Enter subject | textCapSentences |
| `descriptionInput` | EditText | Type your issue in detail… | textMultiLine|textCapSentences |
| `categorySpinner` | SearchableDropdown | Category | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `actionButtonIcon` | ImageView | — |
| `backButton` | ImageView | Back |

**Includes:** `include_support_notice_card`, `include_action_button`

---

### My Support Tickets

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `MySupportTickets` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/MySupportTickets.java` |
| **Layout** | `app/src/main/res/layout/fragment_my_support_tickets.xml` |
| **Navigation** | → SupportTicketDetails |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `statusFilter` | SearchableDropdown | Select Item | — |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `actionButtonIcon` | ImageView | — |
| `supportCallButton` | LinearLayout | — |
| `backButton` | ImageView | Back |

**Key display widgets**

- RecyclerView `#ticketRecyclerView`

**Includes:** `include_support_notice_card`, `include_action_button`, `include_empty_list_state`, `include_support_urgent_help`

---

### Support Ticket Details

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `SupportTicketDetails` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/SupportTicketDetails.java` |
| **Layout** | `app/src/main/res/layout/fragment_support_ticket_details.xml` |
| **Navigation** | Reply thread |

**Input fields**

| ID | Type | Label / hint | Input type |
|---|---|---|---|
| `replyInput` | EditText | Type your reply... | textCapSentences|textMultiLine |

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `actionButtonIcon` | ImageView | — |
| `backButton` | ImageView | Back |
| `overflowButton` | ImageView | Ticket Details |
| `statusBannerIcon` | ImageView | — |
| `sortButton` | LinearLayout | — |
| `closedActions` | LinearLayout | — |
| `openNewTicketButton` | LinearLayout | — |
| `attachButton` | ImageView | Add Attachment (Optional) |
| `emojiButton` | ImageView | — |
| `refreshTicketsButton` | LinearLayout | — |

**Includes:** `include_action_button`

---

### About Us

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `AboutUs` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/AboutUs.java` |
| **Layout** | `app/src/main/res/layout/fragment_about_us.xml` |
| **Navigation** | ← Settings |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backToSetting` | ImageView | Back |
| `headerMenuIcon` | ImageView | Rate Us |

---

### Share App

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `ShareApp` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/ShareApp.java` |
| **Layout** | `app/src/main/res/layout/fragment_share_app.xml` |
| **Navigation** | dialog_share_qr |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `btnShare` | Button | Share Now |
| `btnQR` | Button | Show QR Code |
| `shareContentContainer` | LinearLayout | — |
| `backToSetting` | ImageView | Back |
| `btnCopyIcon` | ImageView | Copy App Link |

**Includes:** `benefit_growth`, `benefit_earnings`, `benefit_billing`

---

### Cloud Sync Status

| | |
|---|---|
| **Kind** | Fragment |
| **Route / entry** | `CloudSyncStatus` |
| **Class** | `app/src/main/java/com/pos_billingwala/Fragment/CloudSyncStatus.java` |
| **Layout** | `app/src/main/res/layout/fragment_cloud_sync_status.xml` |
| **Navigation** | From synchronize |

**Input fields:** none (display / menu / list only)

**Buttons / actions**

| ID | Widget | Label |
|---|---|---|
| `backButton` | ImageView | — |
| `actionButton` | TextView | Sync |

**Includes:** `include_pos_report_toolbar`

---

## Dialogs & bottom sheets (42)

### Searchable List Dialog

- **Layout:** `app/src/main/res/layout/searchable_list_dialog.xml`
- **Used by:** SearchableSpinner / dropdowns
- **Fields:** `search` (SearchView)
- **Lists:** `listItems`

### Confirm Bottom Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_confirm.xml`
- **Used by:** BottomSheetUi
- **Buttons:** `btnSheetNegative`; `btnSheetPositive`

### Info Bottom Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_info.xml`
- **Used by:** BottomSheetUi
- **Buttons:** `sheetIcon` — Billingwala; `btnSheetSecondary`; `btnSheetPrimary`

### Custom Bottom Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_custom.xml`
- **Used by:** BottomSheetUi
- **Buttons:** `closeCustomSheet` — Billingwala; `btnSheetNegative`; `btnSheetPositive`

### Single Choice Bottom Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_single_choice.xml`
- **Used by:** BottomSheetUi / language picker etc.
- **Buttons:** `btnSheetCancel` — @android:string/cancel

### No Internet Bottom Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_no_internet.xml`
- **Used by:** DetectConnection

### Home Notifications Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_home_notifications.xml`
- **Used by:** Home
- **Lists:** `notificationRecycler`

### Occupied Table Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_occupied_table.xml`
- **Used by:** Dine-in ops

### Move Items Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_move_items.xml`
- **Used by:** DineInOpsUi

### Split Input Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_split_input.xml`
- **Used by:** DineInOpsUi
- **Fields:** `splitInputField` (EditText)

### Table Master Form Sheet

- **Layout:** `app/src/main/res/layout/bottom_sheet_table_master_form.xml`
- **Used by:** TableMasterActivity
- **Fields:** `fieldOne` (TextInput); `fieldTwo` (TextInput); `fieldThree` (TextInput); `areaDropdown` (SearchableDropdown); `typeDropdown` (SearchableDropdown)
- **Buttons:** `btnCancel` — @android:string/cancel; `btnSave` — Save

### Set Payment Mode Dialog

- **Layout:** `app/src/main/res/layout/set_payment_mode_dialog.xml`
- **Used by:** BluetoothPrint / EditInvoice / TableAdapter / DineInOpsUi
- **Fields:** `cashAmountInput` (EditText: 0.00); `upiAmountInput` (EditText: 0.00); `cash` (RadioButton: Cash); `online` (RadioButton: UPI); `splitCashUpi` (RadioButton: Cash + UPI)
- **Buttons:** `paymentModeLayout`; `splitPaymentLayout`

### Update Customer Dialog

- **Layout:** `app/src/main/res/layout/update_customer_dialog.xml`
- **Used by:** BluetoothPrint
- **Fields:** `customerName` (TextInput); `customerMobile` (TextInput); `customerEmail` (TextInput); `customerAddress` (TextInput)
- **Buttons:** `addCustomer` — Add Customer

### Update Discount Dialog

- **Layout:** `app/src/main/res/layout/update_discount_dialog.xml`
- **Used by:** BluetoothPrint / EditInvoice
- **Fields:** `discountPercentage` (TextInput); `discountTypeSpinner` (SearchableDropdown: Discount Type*)
- **Buttons:** `addDiscountPercentage` — Add Discount

### Update Packing Dialog

- **Layout:** `app/src/main/res/layout/update_packing_dialog.xml`
- **Used by:** BluetoothPrint / EditInvoice
- **Fields:** `packingCharge` (TextInput); `packingTypeSpinner` (SearchableDropdown: Packing Type*)
- **Buttons:** `addPackingCharge` — Add Packing

### Update Amount/Quantity Dialog

- **Layout:** `app/src/main/res/layout/update_amount_quantity_dialog.xml`
- **Used by:** CreatePos / EditInvoice / CartAdapter
- **Fields:** `amount` (TextInput); `quantity` (TextInput)

### Update Quantity Dialog

- **Layout:** `app/src/main/res/layout/update_quantity_dialog.xml`
- **Used by:** CartAdapter
- **Fields:** `quantity` (TextInput)

### Cart Product Dialog

- **Layout:** `app/src/main/res/layout/cart_product_dialog.xml`
- **Used by:** POS cart open-price
- **Fields:** `productPrice` (TextInput); `productQuantity` (TextInput)
- **Buttons:** `addToCart` — Add To Cart

### Select Portion Dialog

- **Layout:** `app/src/main/res/layout/dialog_select_portion.xml`
- **Used by:** CreatePos
- **Buttons:** `addPortionToCart` — Add To Cart

### Add Combo Item Dialog

- **Layout:** `app/src/main/res/layout/dialog_add_combo_item.xml`
- **Used by:** ComboItemPicker / EditBillProductPicker
- **Fields:** `comboItemProductSearch` (EditText: search product by name, product code)
- **Lists:** `comboItemProductRecyclerView`

### Update Category Dialog

- **Layout:** `app/src/main/res/layout/update_category_dialog.xml`
- **Used by:** CategoryAdapter
- **Fields:** `categoryName` (TextInput)

### Update Subcategory Dialog

- **Layout:** `app/src/main/res/layout/update_subcategory_dialog.xml`
- **Used by:** SubcategoryAdapter
- **Fields:** `subcategoryName` (TextInput)

### Update Portion Master Dialog

- **Layout:** `app/src/main/res/layout/update_portion_master_dialog.xml`
- **Used by:** PortionMasterAdapter
- **Fields:** `portionMasterName` (TextInput)

### Update Portion Dialog

- **Layout:** `app/src/main/res/layout/update_portion_dialog.xml`
- **Used by:** PortionAdapter / ProductAdapter
- **Fields:** `portionName` (TextInput); `portionPrice` (TextInput); `portionSortOrder` (TextInput)

### Report Password / PIN Dialog

- **Layout:** `app/src/main/res/layout/report_password_dialog.xml`
- **Used by:** UserSetting / InvoiceMess / MessInvoiceAdapter
- **Fields:** `reportPin` (TextInput)

### Business Hours Dialog

- **Layout:** `app/src/main/res/layout/dialog_business_hours.xml`
- **Used by:** UserSetting

### Login Device Dialog

- **Layout:** `app/src/main/res/layout/login_device_dialog.xml`
- **Used by:** Login / LoginMPin

### New User / Trial Dialog

- **Layout:** `app/src/main/res/layout/new_user_dialog.xml`
- **Used by:** Login flow
- **Fields:** `signupName` (TextInput); `signupContact` (TextInput); `signupShopName` (TextInput); `signupAddress` (TextInput)
- **Buttons:** `submitSignup` — Create Free Account

### Share Dialog

- **Layout:** `app/src/main/res/layout/share_dialog.xml`
- **Used by:** CreatePos / InvoiceCompanyTable / InvoiceTakeAway / BluetoothPrint
- **Buttons:** `saveInvoiceLayout`; `saveInvoice` — Save Invoice; `shareInvoiceLayout`; `duplicateInvoicePrintLayout`

### Share QR Dialog

- **Layout:** `app/src/main/res/layout/dialog_share_qr.xml`
- **Used by:** ShareApp
- **Buttons:** `closeQrDialog` — Close

### Picture Selection Dialog

- **Layout:** `app/src/main/res/layout/picture_selection_dialog.xml`
- **Used by:** CompanyDetailSetting
- **Buttons:** `closeDialog`; `cameraLayout`; `chooseGalleryLayout`

### Sale Wise Date Dialog

- **Layout:** `app/src/main/res/layout/sale_wise_dialog.xml`
- **Used by:** Most reports
- **Buttons:** `dayWiseLayout`; `monthWiseLayout`; `yearWiseLayout`

### Table Sale Wise Dialog

- **Layout:** `app/src/main/res/layout/table_sale_wise_dialog.xml`
- **Used by:** InvoiceTableListReport
- **Buttons:** `dayWiseLayout`; `monthWiseLayout`; `yearWiseLayout`; `tableWiseLayout`

### Product Wise Dialog

- **Layout:** `app/src/main/res/layout/product_wise_dialog.xml`
- **Used by:** InvoiceProductReport
- **Buttons:** `topSaleProductLayout`; `lessSaleProductLayout`; `dayWiseTopSaleProductLayout`; `dayWiseLessSaleProductLayout`; `monthWiseLessSaleProductLayout`; `monthWiseTopSaleProductLayout`; `yearWiseTopSaleProductLayout`; `yearWiseLessSaleProductLayout`

### Payment Mode Sale Wise Dialog

- **Layout:** `app/src/main/res/layout/payment_mode_sale_wise_dialog.xml`
- **Used by:** InvoicePaymentModeWiseReport
- **Buttons:** `cashModeWiseLayout`; `onlineModeWiseLayout`; `splitModeWiseLayout`; `bankModeWiseLayout`; `dayWiseLayout`; `monthWiseLayout`; `yearWiseLayout`

### Table List Dialog

- **Layout:** `app/src/main/res/layout/table_list_dialog.xml`
- **Used by:** InvoiceTableListReport
- **Fields:** `tableSpinner` (SearchableDropdown: Select Table)

### Pending Amount Dialog

- **Layout:** `app/src/main/res/layout/pending_amount_dialog.xml`
- **Used by:** MessInvoiceAdapter
- **Buttons:** `pendingAmountLayout` — Pending Payment

### Add Member Action Dialog

- **Layout:** `app/src/main/res/layout/add_member_dialog.xml`
- **Used by:** MessMemberList
- **Buttons:** `addMemberLayout`; `addMember`

### Mess Menu Dialog

- **Layout:** `app/src/main/res/layout/mess_menu_dialog.xml`
- **Used by:** Mess flows
- **Buttons:** `memberListLayout`; `qrManagementLayout`; `todayTokensLayout`; `mealSessionsLayout`

### Invoice Dialog

- **Layout:** `app/src/main/res/layout/invoice_dialog.xml`
- **Used by:** Invoice actions
- **Buttons:** `viewInvoiceLayout`; `shareInvoiceLayout`

### Warning Dialog

- **Layout:** `app/src/main/res/layout/dialog_warning.xml`
- **Used by:** Warnings

### Month Picker Dialog

- **Layout:** `app/src/main/res/layout/month_picker_dialog.xml`
- **Used by:** MonthPickerDialog / mess payments

## Notes for Flutter UI update

1. Primary chrome is AppBar + back PosCardView/ImageView, not MaterialToolbar menus everywhere.
2. Many “buttons” are clickable `PosCardView` / `LinearLayout` rows / `ImageView` icons, not `ElevatedButton`.
3. Dropdowns are custom `SearchableDropdownView` (opens searchable bottom sheet), not Spinner.
4. Switches use `PosSwitchRowView` (`SwitchCompat` + label).
5. Billing checkout (`BluetoothPrint`) is the densest screen: cart list, payment radios, discount/packing/customer dialogs, print/share.
6. Report screens share date-range popup patterns (`sale_wise_dialog` family) + RecyclerView results + export actions in toolbar includes.
