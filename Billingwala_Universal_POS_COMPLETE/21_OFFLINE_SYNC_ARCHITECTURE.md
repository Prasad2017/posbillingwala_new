# 21 Offline Sync Architecture

## Flow
Local SQLite first → upload (`OfflineToNetworkReceiver` / `OfflineSyncExecutor`) → download workers (`NetworkDataFetcher` chain).

## Facade
`Extra/OfflineSyncArchitecture.java`

## Rules
- Mark synced only when API `status == "1"`
- Never wipe unsynced bills on fetch
- UI: `CloudSyncStatus` / `CloudSyncTracker`

## Universal extras in chain
| Direction | Entity | Endpoints / worker |
|-----------|--------|--------------------|
| Upload | Salon appointments | `insertServiceAppointment.php` via `UniversalPendingUpload` (`UserSynchronizeData` / `OfflineToNetworkReceiver` / `OfflineNetworkData`) |
| Download | Salon appointments | `getServiceAppointmentList.php` → `ServiceAppointmentWorker` |
| Upload | Bakery deposits | `insertCustomOrderDeposit.php` via `UniversalPendingUpload` |
| Download | Bakery deposits | `getCustomOrderDepositList.php` → `CustomOrderDepositWorker` |
| Upload | Wholesale price tiers | `insertProductPriceTier.php` via `UniversalPendingUpload` |
| Download | Wholesale price tiers | `getProductPriceTierList.php` → `ProductPriceTierWorker` |
| Upload | Fashion variants | `insertProductVariant.php` via `UniversalPendingUpload` |
| Download | Fashion variants | `getProductVariantList.php` → `ProductVariantWorker` |
| Upload | Business template | `insertBusinessTemplate.php` via `UniversalPendingUpload` |
| Download | Business template | `getBusinessTemplate.php` → `BusinessTemplateWorker` |
| Upload | Staff roster | `insertStaffUser.php` via `UniversalPendingUpload` |
| Download | Staff roster | `getStaffUserList.php` → `StaffUserWorker` |

Photo binaries stay on device; deposit sync is metadata (`photoFile` name only).
Price tiers and variants resolve products via `productNetworkStatus` (same as portions).
Template sync is prefs-based (`businessType` / `businessTemplateId` / optional JSON); pending local changes are not overwritten on Fetch.
Staff PIN values sync as stored on device (`sha256:<hex>` preferred; legacy plaintext still verifies and re-hashes on roster read).

