# Store Details API Changes

Structured shop / address / phone fields for POS Billingwala Store Details (Shop Details).

## Summary

Extend the existing `companys` table and company sync APIs. Do **not** create a separate store-settings API.

Legacy fields remain for backward compatibility:

| Legacy field | Role after upgrade |
|---|---|
| `companyName` | Kept; mirrored from `shopName1` on save |
| `companyAddress` | Kept; composed from address lines on save |
| `companyMobile` | Kept; mirrored from `phoneNo1` on save |

## New columns (`companys`)

| Column | Type | Required | Notes |
|---|---|---|---|
| `shopName1` | VARCHAR(255) NULL | Yes (POS app) | Primary shop heading on bills |
| `shopName2` | VARCHAR(255) NULL | No | Secondary line under shop name |
| `addressLine1` | TEXT NULL | No | Migrated from legacy `companyAddress` when empty |
| `addressLine2` | TEXT NULL | No | |
| `addressLine3` | TEXT NULL | No | |
| `phoneNo1` | VARCHAR(32) NULL | Yes (POS, existing rule) | Migrated from `companyMobile` |
| `phoneNo2` | VARCHAR(32) NULL | No | |

Naming matches existing API camelCase (`companyName`, `companyMobile`, …).

## Migration

Run either:

- `API/migrations/p10_store_details_structured.sql`, or
- full `API/migrations/server_upgrade_all.sql` (includes STEP 13)

Migration is additive (never DROP `companys` or legacy columns).

## Shared PHP helper

`API/company_store_fields.php` — used by POS + Owner/Admin/Dealer APIs:

- `company_structured_fields($row)`
- `company_shop_details_block($row)`
- `company_display_address_oneline($row)`

## API endpoints

### POS

| Endpoint | Change |
|---|---|
| `POST insertCompanyDetail.php` | Accepts + mirrors structured fields |
| `GET getCompanyList.php` | Returns structured fields with fallbacks |

### Owner / Admin / Dealer

| Endpoint | Change |
|---|---|
| `API/Owner/getInvoiceProductList.php` | Returns structured shop fields for invoice header |
| `API/Owner/getProfile.php` | License list includes structured fields |
| `API/Owner/getStoreWise.php` | Store list includes structured fields |
| `API/Owner/getCustomerDetails.php` | Same |
| `API/Owner/getBranchComparison.php` | Uses `shopName1` |
| `API/Admin/getCustomerDetails.php` | Same |
| `API/Dealer/getCustomerDetails.php` | Same |
| `API/Dealer/getCustomerList.php` | Same |

## Clients

| Client | Store edit | Display |
|---|---|---|
| **WithTable POS** | Yes (Shop Details) | Bills via `ShopHeaderBuilder` |
| **Owner** | No | Invoice header + license/store lists |
| **Admin** | No (`users.shopName` only) | License cards show shop name / address / phones |
| **Dealer** | No (`users.shopName` only) | Same as Admin |
| **Laravel admin** | No (`users.shopName` only) | Invoice PDF/view via `invoices/partials/store_header`; customer-home address from structured lines |

## Deploy order

1. Run `p10_store_details_structured.sql` on MySQL
2. Deploy PHP APIs (`company_store_fields.php`, POS insert/get, Owner/Admin/Dealer endpoints)
3. Deploy Laravel adminpanel views/model/controller
4. Release WithTable (DB v21), then Owner / Admin / Dealer apps

Older clients keep working via legacy `companyName` / `companyAddress` / `companyMobile`.
