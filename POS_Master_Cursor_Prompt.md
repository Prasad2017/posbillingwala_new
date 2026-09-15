# MASTER CURSOR PROMPT
## POS — Multi-User, Multi-Device, Multi-Printer, Remote Printing, Permissions, Offline/Online & Licensing

You are working on an existing POS ecosystem containing:

- POS Android App
- POS iOS App
- POS Web App
- Owner App/Panel
- Dealer App/Panel
- Admin Panel
- Backend APIs
- Database

The goal is to implement a production-ready **multi-user + multi-device + multi-printer + remote printing + role/module permissions + offline/online synchronization + license management** architecture without breaking existing functionality.

---

# 1. CORE BUSINESS MODEL

Use this hierarchy:

```text
BUSINESS
   |
   +-- STORE / BRANCH A
   |      |
   |      +-- LICENSE A
   |             |
   |             +-- USERS
   |             +-- DEVICES
   |             +-- PRINTERS
   |             +-- ROLES / PERMISSIONS
   |             +-- POS DATA
   |
   +-- STORE / BRANCH B
   |      |
   |      +-- LICENSE B
   |
   +-- STORE / BRANCH C
          |
          +-- LICENSE C
```

Mandatory rule:

```text
ONE LICENSE = ONE STORE / BRANCH
```

Multi-branch requires multiple licenses.

Do not allow one normal single-store license to access multiple branches.

---

# 2. USER MANAGEMENT MODES

Each license/store has:

```text
userManagementEnabled
```

## MODE A — USER MANAGEMENT OFF

When:

```text
userManagementEnabled = false
```

the POS continues the existing single-user/single-license behavior.

Requirements:

- Do not force user creation.
- Do not break existing customers.
- Do not change existing single-license behavior unnecessarily.
- Existing device/license behavior must remain compatible.
- Existing offline operation must continue.

Existing licenses should default safely to:

```text
userManagementEnabled = false
```

unless existing data indicates otherwise.

---

## MODE B — USER MANAGEMENT ON

When:

```text
userManagementEnabled = true
```

allow:

- Multiple users
- Multiple devices
- Android
- iOS
- Web
- Fixed roles
- User-specific permissions
- PIN login
- Device management
- Shared store data
- Multi-printer support
- Remote printing

All users/devices remain restricted to the same store/license.

---

# 3. FIXED USER ROLES

Roles are FIXED.

Do NOT implement custom role creation.

Do NOT allow role deletion.

Do NOT allow role renaming.

The system must support exactly these standard roles:

```text
OWNER
MANAGER
WAITER
KITCHEN
HELPER
BAR_ATTENDER
SECURITY
ACCOUNTANT
```

Display names:

```text
Owner
Manager
Waiter
Kitchen
Helper
Bar Attender
Security
Accountant
```

A user can be assigned one fixed role.

The role itself is not editable as master data.

---

# 4. USER FORM

When creating a user, the form must contain:

## Required

```text
Name *
Mobile Number *
Role *
App Login PIN *
Confirm App Login PIN *
```

## Optional

```text
Address
Image
```

Do not add unnecessary fields unless the existing product requires them.

Example:

```text
ADD USER

Profile Image
[ Upload ]

Name *
[ Enter full name ]

Mobile Number *
[ Enter mobile number ]

Address
[ Optional ]

Role *
[ Waiter ]

App Login PIN *
[ • • • • ]

Confirm PIN *
[ • • • • ]
```

After role selection, load the role's default permissions.

---

# 5. USER CREATION + PERMISSION FLOW

The user creation flow must be:

```text
Add User
   |
   +-- Basic Information
   |
   +-- Select Fixed Role
   |
   +-- Load Role Default Permissions
   |
   +-- Show Module / Action Permissions
   |
   +-- Owner/Admin Adjusts User Permissions
   |
   +-- Save User
```

Example:

```text
Role = Waiter
       |
       v
Load Waiter Default Permissions
       |
       v
Owner modifies specific permissions
       |
       v
Save User
```

---

# 6. ROLE DEFAULT + USER OVERRIDE MODEL

Use:

```text
FIXED ROLE
    |
    +-- DEFAULT PERMISSIONS
             |
             +-- USER-SPECIFIC OVERRIDES
                         |
                         v
                 EFFECTIVE PERMISSIONS
```

Do not duplicate the complete permission matrix unnecessarily for every user.

Recommended logical states:

```text
DEFAULT
ALLOW
DENY
```

Meaning:

```text
DEFAULT
= Follow role default

ALLOW
= Explicitly allow this user

DENY
= Explicitly deny this user
```

Effective permission:

```text
If user override exists:
    use user override

Otherwise:
    use role default
```

Example:

```text
Role: Waiter
KOT_PRINT = ALLOW

User: Rahul
KOT_PRINT = DEFAULT

Effective:
KOT_PRINT = ALLOW
```

Example:

```text
Role: Waiter
KOT_PRINT = ALLOW

User: Rahul
KOT_PRINT = DENY

Effective:
KOT_PRINT = DENY
```

---

# 7. USER PERMISSION ASSIGNMENT UI

During Add User/Edit User, show permissions grouped by module.

Example:

```text
PERMISSIONS

[ Reset to Role Defaults ]

ORDER
--------------------------------
View       [✓]
Create     [✓]
Edit       [✓]
Delete     [ ]

KOT
--------------------------------
View       [✓]
Create     [✓]
Edit       [✓]
Delete     [ ]
Print      [✓]
Reprint    [ ]

BILL
--------------------------------
View       [ ]
Create     [ ]
Edit       [ ]
Delete     [ ]
Print      [ ]
Reprint    [ ]

PRODUCTS
--------------------------------
View       [✓]
Create     [ ]
Edit       [ ]
Delete     [ ]

REPORTS
--------------------------------
View       [ ]
Export     [ ]

USERS
--------------------------------
View       [ ]
Create     [ ]
Edit       [ ]
Deactivate [ ]
Change Role [ ]
Reset PIN  [ ]

PRINTERS
--------------------------------
View       [ ]
Manage     [ ]
Test Print [ ]
```

Use the existing application UI/design system.

---

# 8. RESET TO ROLE DEFAULTS

Provide:

```text
[ Reset to Role Defaults ]
```

When clicked:

```text
Remove user-specific overrides
        |
        v
Apply role default permissions
```

Ask for confirmation if permission changes have already been made.

---

# 9. PERMISSION DEPENDENCIES

Implement logical dependencies.

Examples:

```text
BILL_CREATE
    requires BILL_VIEW

BILL_EDIT
    requires BILL_VIEW

BILL_DELETE
    requires BILL_VIEW

BILL_PRINT
    requires BILL_VIEW

BILL_REPRINT
    requires BILL_VIEW

KOT_PRINT
    requires KOT_VIEW

KOT_REPRINT
    requires KOT_VIEW

USER_EDIT
    requires USER_VIEW

USER_DEACTIVATE
    requires USER_VIEW

USER_RESET_PIN
    requires USER_VIEW
```

Prevent invalid permission combinations.

---

# 10. SELECT ALL / CLEAR ALL

Where appropriate, support:

```text
[ Select All ]
[ Clear All ]
```

at module level.

Example:

```text
KOT
[✓] All

View
Create
Edit
Print
Reprint
```

Only authorized Owner/Admin users should manage permissions.

---

# 11. USER FORM VALIDATION

Validate:

### Name

- Required
- Trim whitespace
- Reasonable max length

### Mobile

- Required
- Validate according to existing mobile validation
- For India, support standard 10-digit mobile numbers
- Prevent duplicate active login identities within the same store

Recommended uniqueness:

```text
storeId + mobileNumber
```

Do not unnecessarily make mobile globally unique across every store.

### Address

Optional.

### Image

Optional.

### Role

Required and must be one of the fixed roles.

### PIN

Required.

Validate:

- Allowed PIN length
- Numeric PIN
- Confirm PIN matches

Recommended configurable PIN length:

```text
4 or 6 digits
```

---

# 12. PIN SECURITY

CRITICAL.

Never store the App Login PIN as plain text.

Never store plain PIN in:

- Database
- SQLite
- SharedPreferences
- LocalStorage
- Logs
- Analytics
- Crash reports
- API logs

Backend must securely hash the PIN.

Do not log the PIN or PIN hash.

Do not expose the existing PIN to Owner/Admin.

Only support:

```text
Set PIN
Reset PIN
```

not:

```text
View PIN
```

---

# 13. USER LOGIN

When User Management is ON:

```text
POS LOGIN

Mobile Number
[ 9876543210 ]

App PIN
[ • • • • ]

[ Login ]
```

Authentication flow:

```text
Login
  |
  v
Authenticate User
  |
  v
Validate License
  |
  v
Validate Store
  |
  v
Validate User Status
  |
  v
Validate Device
  |
  v
Load Fixed Role
  |
  v
Load Effective Permissions
  |
  v
Open POS
```

---

# 14. USER STATUS

Support:

```text
ACTIVE
INACTIVE
BLOCKED
```

Inactive/blocked users cannot log in.

Do not delete users unnecessarily when deactivation is sufficient.

---

# 15. USER DETAILS

Show:

```text
Profile Image
Name
Mobile Number
Address
Role
Status
Last Login
Devices
Effective Permissions
```

Never show PIN.

Authorized actions:

```text
Edit
Change Role
Reset PIN
Deactivate
```

---

# 16. OWNER PROTECTION

A store must always have at least one active Owner.

Do not allow:

```text
Delete final Owner
Deactivate final Owner
Remove all final Owner permissions
Change final Owner to another role
```

unless another valid active Owner has already been assigned.

---

# 17. DEFAULT ROLE PERMISSION INTENT

Define centralized defaults.

### OWNER

Full store access.

Typical:

```text
Dashboard
Billing
Orders
KOT
Tables
Products
Inventory
Customers
Reports
Users
Printers
Settings
```

### MANAGER

Operational management:

```text
Dashboard
Billing
Orders
KOT
Tables
Products
Inventory
Customers
Reports
Printers
```

### WAITER

Typical:

```text
Orders
Tables
KOT View
KOT Create
KOT Print
```

Normally restricted:

```text
Billing management
Product management
Inventory
Reports
Users
Settings
Printer management
```

### KITCHEN

Typical:

```text
KOT View
KOT Print
Kitchen Orders
Order Status
```

### HELPER

Limited operational permissions.

### BAR ATTENDER

Typical:

```text
Bar Orders
KOT
Bar KOT Printing
Order Status
```

### SECURITY

Only security/entry-related modules as applicable.

### ACCOUNTANT

Typical:

```text
Bills/View
Payments
Expenses
Financial Reports
GST
Account Reports
Export
```

These are default permissions only. Inspect the existing POS modules and create the final mapping according to actual product functionality.

---

# 18. USER ≠ DEVICE ≠ PRINTER

This is a mandatory architectural principle.

```text
USER
  |
  +-- has ROLE + PERMISSIONS

DEVICE
  |
  +-- runs POS

PRINTER
  |
  +-- belongs to STORE
  +-- may be connected to DEVICE

PRINT JOB
  |
  +-- connects business transaction to PRINTER
```

Do NOT design:

```text
User -> Printer
```

Design:

```text
User
 ↓
Permission
 ↓
Business Transaction
 ↓
Print Job
 ↓
Printer Routing
 ↓
Print Host
 ↓
Printer
```

---

# 19. MULTI-DEVICE

When User Management is ON, support multiple authorized devices under the same license/store.

Example:

```text
License L001
Store A

Users:
    Owner
    Manager
    Waiter 1
    Waiter 2
    Cashier

Devices:
    POS Android
    POS Web
    Waiter Android 1
    Waiter Android 2
    Manager iOS
```

Device is independent from user.

A user may use multiple devices according to license/device limits.

---

# 20. LICENSE LIMITS

Do not hard-code limits.

Support configurable fields where the commercial license requires them:

```text
maxUsers
maxDevices
maxPrinters
```

Example:

```text
License L001

User Management = ON
Max Users = 10
Max Devices = 5
Max Printers = configurable
```

If current plans do not enforce maxPrinters, keep the architecture future-ready without adding arbitrary restrictions.

---

# 21. MULTI-PRINTER ON SINGLE POS APP

A single POS installation must support multiple printers.

Example:

```text
POS Android
 |
 +-- Kitchen Printer
 +-- Bar Printer
 +-- Counter Printer
 +-- Packing Printer
```

Do NOT assume:

```text
1 Device = 1 Printer
```

Correct:

```text
1 Device = N Printers
```

where supported by the platform/integration.

---

# 22. PRINTER MASTER

Inspect the existing database first.

Do not create duplicate printer tables if an existing printer master already exists.

Conceptually support:

```text
printerId
businessId
storeId
licenseId
deviceId
printerName
printerType
connectionType
ipAddress
port
bluetoothAddress
usbConfiguration
purpose
status
enabled
isDefault
createdAt
updatedAt
```

Use existing naming conventions.

---

# 23. PRINTER CONNECTION TYPES

Continue supporting existing printer technology:

```text
Bluetooth
USB
Wi-Fi
Network ESC/POS
```

Preserve existing:

```text
ESC/POS
2-inch printers
3-inch printers
Bluetooth
USB
Wi-Fi
Auto Cutter
Cash Drawer
KOT
Bill
```

Do not rewrite working printing code unnecessarily.

---

# 24. MULTIPLE BLUETOOTH PRINTERS

Where platform capabilities support it, allow multiple independently configured Bluetooth printers.

Example:

```text
Android POS

Kitchen Printer → Bluetooth
Bar Printer     → Bluetooth
Counter Printer → Bluetooth
```

Do not use only one global printer connection.

Maintain independent printer connection/state per printer.

One disconnected printer must not crash or break other printers.

---

# 25. MULTIPLE USB PRINTERS

Where supported by device/OS:

```text
USB Kitchen Printer
USB Counter Printer
```

Each printer must have independent configuration.

---

# 26. MULTIPLE NETWORK PRINTERS

Example:

```text
Kitchen Printer
192.168.1.101:9100

Bar Printer
192.168.1.102:9100

Counter Printer
192.168.1.103:9100
```

Each printer must be independently testable.

---

# 27. PRINTER PURPOSE

Support:

```text
KOT
BILL
REPRINT
PACKING
LABEL
REPORT
```

and logical areas:

```text
KITCHEN
BAR
COUNTER
PACKING
TAKEAWAY
```

---

# 28. CATEGORY-WISE PRINTER ROUTING

Support routing by category/subcategory/product where existing POS supports those entities.

Example:

```text
Food
  -> Kitchen Printer

Beverage
  -> Bar Printer

Dessert
  -> Dessert Printer
```

Example order:

```text
Pizza
Burger
Coke
```

Print:

```text
Kitchen Printer:
    Pizza
    Burger

Bar Printer:
    Coke
```

A single order may generate multiple print jobs.

---

# 29. BILL PRINTER ROUTING

Bill:

```text
Cashier
  |
  v
Generate Bill
  |
  v
Print Bill
  |
  v
Counter Printer
```

The printer can be connected to another POS device.

Example:

```text
Cashier Android
  |
  v
Print Job
  |
  v
POS Web Print Host
  |
  v
Counter Printer
```

---

# 30. WAITER WITHOUT PRINTER

A waiter device does not need a physical printer.

Example:

```text
Waiter Android
    |
    +-- No printer
```

Waiter can:

```text
Login
Select Table
Take Order
Create KOT
Print KOT
```

The print job routes to a printer connected to another authorized device in the same store.

---

# 31. PRINT HOST

Any POS Android/Web installation with configured physical printers can act as a:

```text
PRINT HOST
```

Example:

```text
POS Android
 |
 +-- Kitchen Printer
 +-- Bar Printer
 +-- Counter Printer
 |
 Print Host = YES
```

Waiter device:

```text
Waiter Android
 |
 +-- No Printer
 |
 Print Host = NO
```

---

# 32. ONE PRINT HOST + MULTIPLE PRINTERS

Mandatory.

A Print Host may have:

```text
Kitchen Printer
Bar Printer
Counter Printer
Packing Printer
```

All printers must be independently addressable.

---

# 33. REMOTE KOT PRINT FLOW

Implement:

```text
Waiter Android
      |
      | Create Order
      v
Backend / Local Sync
      |
      | Create Print Job
      v
Printer Routing
      |
      v
Authorized Print Host
      |
      v
Kitchen / Bar Printer
      |
      v
KOT Printed
```

The waiter must receive status:

```text
KOT Created
Printing...
Printed Successfully
```

or:

```text
KOT Created
Printer Offline
Queued for Printing
```

Never report successful printing until the Print Host confirms it.

---

# 34. REMOTE BILL PRINT

Same architecture:

```text
Cashier
  |
  v
Create Bill
  |
  v
Print Job
  |
  v
Counter Print Host
  |
  v
Counter Printer
```

---

# 35. PRINT JOB

Introduce/reuse a Print Job abstraction.

Conceptually:

```text
PrintJob

id
businessId
storeId
licenseId
createdByUserId
createdByDeviceId
printerId
documentType
documentId
payload
status
priority
retryCount
createdAt
printedAt
failedAt
errorMessage
idempotencyKey
```

Statuses:

```text
QUEUED
SENT
RECEIVED
PRINTING
PRINTED
FAILED
CANCELLED
```

Do not duplicate print jobs on retry.

---

# 36. PRINTING IS SEPARATE FROM TRANSACTION

Mandatory rule:

```text
ORDER CREATION != PRINTING
BILL CREATION != PRINTING
KOT CREATION != PRINTING
```

Example:

```text
Create KOT = SUCCESS
Print KOT = FAILED
```

must NOT create another KOT.

Only retry the print job.

---

# 37. IDEMPOTENCY

Every transaction and print request must use safe unique identifiers/idempotency.

Retries must not create:

```text
Duplicate Order
Duplicate KOT
Duplicate Bill
Duplicate Print Job
```

---

# 38. PRINT QUEUE

Support local/server print queues.

Example:

```text
Print Queue

KOT #1001     PRINTED
KOT #1002     PRINTING
KOT #1003     QUEUED
Bill #501     FAILED
```

Authorized users can retry failed jobs.

---

# 39. PRINT ACKNOWLEDGEMENT

Print Host should send:

```text
JOB_RECEIVED
PRINT_STARTED
PRINT_SUCCESS
PRINT_FAILED
```

Only mark:

```text
PRINTED
```

after the printer integration confirms successful printing.

---

# 40. MULTIPLE PRINTER SIMULTANEOUS/INDEPENDENT PRINTING

Example:

```text
Order #1001

Pizza
Burger
Coke
Packing Item
```

Create:

```text
JOB-001 -> Kitchen
JOB-002 -> Bar
JOB-003 -> Packing
```

One slow/offline printer must not block unrelated printers.

---

# 41. PRINTER FAILOVER

Support optional primary/backup configuration.

Example:

```text
Primary:
Kitchen Printer 1

Backup:
Kitchen Printer 2
```

If primary is unavailable:

```text
Primary Offline
   |
   v
Backup Available
   |
   v
Print
```

Do not route to unrelated printers silently.

---

# 42. PRINTER MANAGEMENT

Owner/authorized manager should see:

```text
Settings
   |
   +-- Printer Management
```

Actions:

```text
Add Printer
Edit Printer
Delete/Disable Printer
Test Print
Set Default
Assign Purpose
Assign Categories
View Status
```

Only users with printer-management permission can access these actions.

---

# 43. PRINTING PERMISSION

Separate:

```text
KOT_PRINT
BILL_PRINT
KOT_REPRINT
BILL_REPRINT
```

from:

```text
PRINTER_VIEW
PRINTER_MANAGE
PRINTER_TEST
```

Example:

```text
Waiter:
    KOT Print = YES
    Printer Manage = NO

Cashier:
    Bill Print = YES
    Printer Manage = NO/optional

Owner:
    KOT Print = YES
    Bill Print = YES
    Printer Manage = YES
```

A waiter can trigger remote printing without changing printer configuration.

---

# 44. LOCAL PRINTING

If a waiter/cashier device itself has a configured printer:

```text
User Device
   |
   v
Local Printer
```

allow direct/local printing.

Therefore support both:

```text
LOCAL PRINTING
REMOTE PRINTING
```

---

# 45. OFFLINE-FIRST

The POS must remain offline-first.

Network available:

```text
ONLINE
```

Network unavailable:

```text
OFFLINE
```

The POS should continue supported local operations:

```text
Orders
KOT
Bills
Local Printing
Local Data
```

according to existing business rules.

---

# 46. OFFLINE REMOTE PRINTING

If the waiter is offline and the remote printer host cannot be reached:

Do NOT report remote print success.

Show:

```text
KOT saved locally.

Remote printer is currently unavailable.
It will print when connection is restored.
```

When connectivity returns:

```text
Local Transaction
   |
   v
Sync
   |
   v
Print Job
   |
   v
Print Host
   |
   v
Printer
```

---

# 47. ONLINE/OFFLINE SYNC

When online:

```text
POS
 |
 v
API
 |
 v
Server Database
```

When offline:

```text
POS
 |
 v
Local Database
 |
 v
Sync Queue
```

When network returns:

```text
Local Queue
 |
 v
Sync Engine
 |
 v
Backend
 |
 v
Server
```

Use:

```text
Unique IDs
Timestamps
Versioning
Idempotency
Retry
Conflict Handling
```

according to the existing architecture.

---

# 48. PERMISSION SYNC

When Owner changes user permissions:

```text
Server
  |
  v
Permission Version Updated
  |
  v
POS Sync
  |
  v
Latest Effective Permissions
```

Use a:

```text
permissionVersion
```

or equivalent.

Do not allow stale permission data to overwrite newer configuration.

---

# 49. OFFLINE PERMISSIONS

For offline operation, securely cache the user's effective permissions where necessary.

Example:

```text
userId
role
effectivePermissions
permissionVersion
lastUpdatedAt
```

Never cache the PIN in plain text.

When online, refresh permissions.

---

# 50. STORE ISOLATION

Every protected record/API must validate store ownership.

Relevant data includes:

```text
Users
Devices
Printers
Print Jobs
Products
Orders
KOT
Bills
Customers
Tables
Inventory
Reports
```

At minimum use:

```text
businessId
storeId
licenseId
```

where appropriate.

Never allow:

```text
Store A User
   |
   X
Store B Data
```

---

# 51. MULTI-BRANCH

Example:

```text
Business ABC

Pune
  License L001

Mumbai
  License L002

Nashik
  License L003
```

Each branch independently has:

```text
Users
Devices
Printers
Roles
Permissions
Transactions
Inventory
Reports
```

No cross-branch access.

---

# 52. ADMIN PANEL

Admin should manage:

```text
Businesses
Stores
Licenses
Users
Devices
Printers
Print Hosts
```

License fields:

```text
License Number
Store
Plan
Status
Start Date
Expiry Date
User Management
Max Users
Max Devices
Max Printers if applicable
```

Actions:

```text
Activate
Deactivate
Renew
Extend
Enable User Management
Disable User Management
Change Limits
View Users
View Devices
View Printers
Revoke Device
```

---

# 53. DEALER PANEL

Dealer only sees customers/licenses assigned to that dealer.

Dealer can:

```text
View Customer
View Store
Activate License
Renew License
View License
View User Management Status
View User/Device counts
```

Never expose PINs.

---

# 54. OWNER PANEL

Owner manages only their own store.

Owner can:

```text
User Management
Users
Role Assignment
Permission Assignment
Devices
Printers
Printer Routing
License Information
```

Owner cannot create custom roles.

---

# 55. BACKEND AUTHORIZATION

CRITICAL.

Frontend permission checks are for UX only.

Backend must enforce every protected action.

Authorization flow:

```text
Request
  |
  v
Authenticate User
  |
  v
Identify Business
  |
  v
Identify Store
  |
  v
Identify License
  |
  v
Validate User
  |
  v
Get Fixed Role
  |
  v
Get Role Defaults
  |
  v
Get User Overrides
  |
  v
Calculate Effective Permission
  |
  v
Authorize API
  |
  v
Execute
```

---

# 56. PRINT SECURITY

Print jobs can only target:

```text
Same Business
Same Store
Same License
Authorized Print Host
Authorized Printer
```

Never allow cross-store printing.

---

# 57. DEVICE MANAGEMENT

Track:

```text
deviceId
businessId
storeId
licenseId
userId where applicable
platform
deviceName
appVersion
osVersion
status
lastSeenAt
lastSyncAt
```

Platforms:

```text
ANDROID
IOS
WEB
```

Do not rely only on IMEI.

Use secure application-generated device identity where appropriate.

---

# 58. SESSION MANAGEMENT

Track:

```text
sessionId
userId
deviceId
storeId
licenseId
loginAt
lastActivityAt
logoutAt
status
```

Statuses:

```text
ACTIVE
EXPIRED
LOGGED_OUT
REVOKED
```

---

# 59. DATABASE

FIRST inspect the existing schema.

Do not create duplicate tables.

Potential entities:

```text
business
store
license
user
role
permission
role_permission
user_permission_override
device
user_device
session
printer
printer_route
print_host
print_job
audit_log
```

Use existing naming conventions.

Use:

```text
Foreign Keys
Indexes
Unique Constraints
Status
Soft Delete
CreatedAt
UpdatedAt
Versioning
```

where appropriate.

---

# 60. USER DATA MODEL

Conceptually:

```text
users

id
businessId
storeId
name
mobileNumber
address
profileImage
role
pinHash
status
createdAt
updatedAt
```

Do not store plain PIN.

Role should be a controlled fixed value/reference.

---

# 61. USER PERMISSION OVERRIDE MODEL

Conceptually:

```text
user_permission_override

id
userId
permissionId
overrideState
createdAt
updatedAt
```

Where:

```text
overrideState =
ALLOW
DENY
```

No row means:

```text
DEFAULT
```

This is preferable to duplicating every role permission for every user.

---

# 62. API REQUIREMENTS

Inspect existing APIs first.

Reuse existing APIs where possible.

Required functionality:

```text
Create User
List Users
Get User
Update User
Deactivate User
Change Role
Reset PIN

Login
Get Current User
Get Effective Permissions

Register Device
List Devices
Revoke Device

List Printers
Create Printer
Update Printer
Delete/Disable Printer
Test Printer

Create Print Job
Get Print Job
Acknowledge Print Job
Retry Print Job
```

Follow existing API naming conventions.

---

# 63. USER CREATION API

Conceptually:

```json
{
  "name": "Rahul Patil",
  "mobileNumber": "9876543210",
  "address": "Pune",
  "profileImage": "...",
  "role": "WAITER",
  "appLoginPin": "****",
  "permissionOverrides": {
    "kot.print": "ALLOW",
    "kot.reprint": "DENY",
    "bill.view": "DENY",
    "bill.print": "DENY"
  }
}
```

Never log the actual PIN.

Backend securely hashes it.

---

# 64. ROLE CHANGE

When Owner changes:

```text
Waiter -> Manager
```

do not silently retain incompatible permissions without informing the Owner.

Recommended flow:

```text
Role Change
   |
   v
Manager default permissions
   |
   v
Reset old overrides
```

or explicitly allow "keep compatible overrides" if the architecture requires it.

Show a confirmation.

---

# 65. USER EDIT

Edit User should support:

```text
Basic Information
Role
Permission Overrides
Status
Reset PIN
```

Do not show current PIN.

---

# 66. PRINTER STATUS

Support:

```text
ONLINE
OFFLINE
CONNECTING
ERROR
DISABLED
```

Track:

```text
lastHeartbeatAt
lastPrintAt
deviceId
printerId
```

Do not delete printers just because the host is temporarily offline.

---

# 67. PRINTER TEST PRINT

Every configured printer should have:

```text
[ Test Print ]
```

The test print should verify actual printer connectivity.

---

# 68. EXISTING PRINTING CODE

Before changing printing:

1. Locate Android printing.
2. Locate iOS printing.
3. Locate Web printing.
4. Locate ESC/POS encoding.
5. Locate bitmap/image printing.
6. Locate Marathi/English support.
7. Locate logo printing.
8. Locate QR printing.
9. Locate KOT printing.
10. Locate bill printing.
11. Locate cash drawer.
12. Locate auto cutter.
13. Locate printer settings.

Reuse working code.

Do not break:

```text
Marathi
English
Mixed Marathi + English
₹ / Rupee
Logo
QR
2-inch
3-inch
KOT
Bill
Cash Drawer
Auto Cutter
```

---

# 69. WEB PRINTING

Inspect the existing Web POS printing implementation.

Do not assume browser:

```text
window.print()
```

is sufficient for ESC/POS thermal printers.

If the existing architecture uses a local print bridge/agent/native helper, reuse it.

The Web POS can act as a Print Host.

---

# 70. ANDROID PRINT HOST

Android POS may act as:

```text
Print Host = YES
```

when one or more printers are configured.

Example:

```text
Android POS
 |
 +-- Bluetooth Kitchen
 +-- Bluetooth Bar
 +-- Wi-Fi Counter
 +-- USB Packing
```

All must remain independently manageable.

---

# 71. IOS

Implement equivalent architecture supported by iOS and the existing printer integrations.

Do not claim unsupported printer connectivity.

An iOS Waiter can still create remote Print Jobs to an Android/Web Print Host when network connectivity exists.

---

# 72. UI NAVIGATION

When User Management is ON, authorized users see:

```text
Profile
Switch User / Logout
```

Modules should be shown according to effective permissions.

If a user lacks permission:

- Hide module where appropriate.
- Hide action buttons.
- Prevent deep-link access.
- Backend must reject unauthorized API calls.

---

# 73. USER MANAGEMENT SCREENS

Implement:

```text
User List
Add User
Edit User
User Details
Reset PIN
Role Selection
Permission Assignment
Permission Summary
Device List
```

Do NOT create:

```text
Custom Role Management
```

---

# 74. PRINTER SCREENS

Implement:

```text
Printer List
Add Printer
Edit Printer
Printer Details
Test Print
Printer Routing
Print Queue
Print Job Details
```

Only authorized roles/users can access printer-management features.

---

# 75. AUDIT LOG

Audit:

```text
User Created
User Updated
Role Changed
Permission Changed
User Deactivated
PIN Reset
User Login
User Logout
Device Registered
Device Revoked

Printer Added
Printer Updated
Printer Disabled
Printer Test Print
Print Job Created
Print Job Failed
Print Job Retried

User Management Enabled
User Management Disabled
```

Do NOT log:

```text
PIN
PIN hash
authentication secrets
tokens
```

---

# 76. TEST CASE — USER MANAGEMENT

Create:

```text
Rahul Patil
9876543210
Waiter
PIN
```

Verify:

```text
Waiter default permissions load.
Owner can modify module/action permissions.
User saves successfully.
Effective permissions are stored correctly.
```

---

# 77. TEST CASE — WAITER

Configure:

```text
KOT_VIEW = YES
KOT_CREATE = YES
KOT_PRINT = YES
KOT_REPRINT = NO

BILL_VIEW = NO
BILL_CREATE = NO
BILL_PRINT = NO
BILL_REPRINT = NO

PRINTER_MANAGE = NO
```

Verify:

```text
Waiter can take order.
Waiter can create KOT.
Waiter can print KOT.
Waiter cannot print/reprint bills.
Waiter cannot manage printers.
```

---

# 78. TEST CASE — REMOTE KOT

Configuration:

```text
Store A
License L001

Waiter Android
No printer

POS Android
Kitchen Printer
Bar Printer

POS Web
Counter Printer
```

Test:

```text
1. Login as Waiter.
2. Select Table.
3. Add Pizza.
4. Add Coke.
5. Create KOT.
6. KOT is saved.
7. Kitchen item routes to Kitchen Printer.
8. Beverage routes to Bar Printer.
9. Print Host receives jobs.
10. Both printers print.
11. Waiter sees print status.
```

---

# 79. TEST CASE — BILL

```text
Cashier
   |
   v
Generate Bill
   |
   v
Print Bill
   |
   v
Counter Printer
```

Verify bill prints even when the Counter Printer is connected to another authorized POS Web/Android Print Host.

---

# 80. TEST CASE — MULTIPLE PRINTERS

One POS Android:

```text
Kitchen = Bluetooth
Bar = Bluetooth
Counter = Wi-Fi
Packing = USB
```

Verify:

```text
All printers can be configured.
All printers can be tested independently.
Disconnecting Kitchen does not break Bar.
Disconnecting Bar does not break Counter.
Counter continues printing.
Packing continues printing.
No global printer connection crash occurs.
```

---

# 81. TEST CASE — OFFLINE

Disconnect Internet.

Verify:

```text
Order can be created.
KOT can be created.
Bill can be created.
Local printer can print.
Transactions are saved locally.
```

Reconnect:

```text
Transactions sync.
Queued remote print jobs sync.
Print jobs are not duplicated.
Bills are not duplicated.
KOTs are not duplicated.
```

---

# 82. TEST CASE — MULTI-BRANCH SECURITY

Create:

```text
Store A -> License A
Store B -> License B
```

Store A user must NOT access:

```text
Store B Users
Store B Printers
Store B Print Jobs
Store B Products
Store B Orders
Store B Bills
Store B Reports
```

Test both:

```text
Frontend
Backend API
```

---

# 83. BACKWARD COMPATIBILITY TEST

Existing license:

```text
User Management OFF
```

Verify:

```text
Existing login works.
Existing billing works.
Existing KOT works.
Existing printing works.
Existing offline mode works.
Existing sync works.
```

No unnecessary migration should break existing customers.

---

# 84. IMPLEMENTATION PROCESS

Do NOT immediately modify files.

Follow these phases.

## PHASE 1 — AUDIT

Inspect the entire repository.

Identify:

```text
Admin
Dealer
Owner
Android
iOS
Web
Backend
Database
Authentication
License
Store/Branch
User
Device
Printer
Printing
Offline DB
Sync
WebSocket
```

Create:

```text
EXISTING ARCHITECTURE
EXISTING AUTHENTICATION
EXISTING LICENSE FLOW
EXISTING STORE FLOW
EXISTING USER FLOW
EXISTING DEVICE FLOW
EXISTING PRINTING
EXISTING DATABASE
EXISTING APIs
EXISTING OFFLINE/SYNC
```

---

## PHASE 2 — GAP ANALYSIS

Create:

```text
ALREADY EXISTS
NEEDS MODIFICATION
NEW IMPLEMENTATION
DATABASE MIGRATION
API CHANGE
UI CHANGE
POTENTIAL CONFLICT
```

Do not duplicate existing functionality.

---

## PHASE 3 — DATABASE

Implement safe migrations.

Existing data must remain valid.

---

## PHASE 4 — BACKEND

Implement:

```text
User
Fixed Role
Role Defaults
User Permission Overrides
Effective Permissions
Device
Session
Printer
Print Host
Print Job
Printer Routing
Authorization
```

---

## PHASE 5 — ADMIN

Implement license/user/device/printer administration.

---

## PHASE 6 — DEALER

Implement dealer-scoped visibility.

---

## PHASE 7 — OWNER

Implement:

```text
User Management
Users
Fixed Role Assignment
User Permissions
Devices
Printers
Printer Routing
```

---

## PHASE 8 — ANDROID

Implement:

```text
User Login
PIN
Permissions
Multi-device
Multi-printer
Print Host
Remote Print
Offline Queue
Sync
```

---

## PHASE 9 — IOS

Implement equivalent functionality supported by iOS.

---

## PHASE 10 — WEB

Implement:

```text
User Login
Permissions
Multi-device
Print Host
Multi-printer
Remote Print
Print Queue
```

---

## PHASE 11 — TESTING

Run:

```text
Unit Tests
Integration Tests
API Tests
Database Tests
Permission Tests
Security Tests
Offline Tests
Sync Tests
Multi-user Tests
Multi-device Tests
Multi-printer Tests
Remote Print Tests
Multi-branch Tests
Backward Compatibility Tests
```

---

# 85. CODING RULES

Do not:

- Rewrite unrelated modules.
- Remove existing features.
- Break printing.
- Break offline operation.
- Break sync.
- Create duplicate authentication.
- Create duplicate printer architecture.
- Create custom roles.
- Store plain PIN.
- Trust frontend authorization.
- Allow cross-store access.
- Hard-code arbitrary license limits.
- Use one global printer connection for multiple printers.

Do:

- Reuse existing architecture.
- Reuse existing API client.
- Reuse existing database layer.
- Reuse existing printer libraries.
- Reuse existing offline/sync architecture.
- Follow existing coding conventions.
- Add migrations.
- Add tests.
- Add audit logging.
- Keep backend authorization authoritative.

---

# 86. FINAL ARCHITECTURE

The complete system should work as:

```text
                         BUSINESS
                            |
                +-----------+-----------+
                |                       |
             STORE A                 STORE B
                |                       |
            LICENSE A                LICENSE B
                |
        +-------+--------+
        |       |        |
      USERS   DEVICES  PRINTERS
        |       |        |
        v       v        v
      ROLE    POS      PRINT HOST
        |                |
        v                +---- Kitchen
    DEFAULT              +---- Bar
    PERMISSIONS          +---- Counter
        |                +---- Packing
        v
USER OVERRIDES
        |
        v
EFFECTIVE PERMISSIONS
        |
        v
      POS
        |
   +----+----+
   |         |
 ORDER      BILL
   |
   v
 KOT
   |
   v
PRINT JOB
   |
   v
PRINTER ROUTING
   |
   v
PRINT HOST
   |
   v
THERMAL PRINTER
```

---

# 87. CORE RULES TO VERIFY

```text
USER MANAGEMENT OFF
    |
    v
Existing single-user/license behavior

USER MANAGEMENT ON
    |
    +-- Fixed roles
    +-- Multiple users
    +-- Multiple devices
    +-- User PIN login
    +-- User-specific module/action permissions
    +-- Multiple printers
    +-- Remote printing
    +-- Offline operation
    +-- Online synchronization

ONE LICENSE
    |
    v
ONE STORE / BRANCH

MULTI-BRANCH
    |
    v
MULTIPLE LICENSES

USER != DEVICE != PRINTER

ONE DEVICE
    |
    v
MULTIPLE PRINTERS

WAITER DEVICE WITHOUT PRINTER
    |
    v
CREATE KOT
    |
    v
PRINT JOB
    |
    v
REMOTE PRINT HOST
    |
    v
KITCHEN / BAR PRINTER
```

---

# 88. FINAL ACCEPTANCE CRITERIA

Do not declare implementation complete until all are verified:

- Single-user existing license works.
- User Management ON works.
- Fixed 8 roles work.
- User creation works.
- PIN login works securely.
- Role defaults load automatically.
- User-specific permissions work.
- Permission overrides work.
- Reset to role defaults works.
- Backend authorization works.
- Multiple users work.
- Multiple devices work.
- Android works.
- iOS works.
- Web works.
- One POS can manage multiple printers.
- Bluetooth printers work where supported.
- USB printers work where supported.
- Wi-Fi/network printers work.
- KOT printing works.
- Bill printing works.
- Category-wise routing works.
- Remote KOT printing works.
- Remote bill printing works.
- Waiter without printer works.
- POS Android can be Print Host.
- POS Web can be Print Host.
- Local printing works.
- Offline queue works.
- Online synchronization works.
- Print acknowledgement works.
- Print retry works.
- Duplicate prevention works.
- Store isolation works.
- Multi-branch licensing works.
- Existing printing functionality remains intact.
- Marathi/English printing remains intact.
- Audit logs work.
- No PIN/token/security secrets are logged.

After implementation, provide a final report containing:

```text
1. Architecture Changes
2. Database Changes
3. API Changes
4. Admin Changes
5. Dealer Changes
6. Owner Changes
7. Android Changes
8. iOS Changes
9. Web Changes
10. User/Role/Permission Changes
11. Printer Changes
12. Print Host Changes
13. Print Job Changes
14. Offline/Sync Changes
15. Security Changes
16. Migration Steps
17. Tests Passed
18. Tests Failed
19. Known Limitations
20. Files Changed
```

Do not claim 100% completion unless the acceptance criteria have actually been implemented and verified end-to-end.
