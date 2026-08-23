# Database deploy (P5-5)

## Credentials — not in git

Production MySQL credentials must **not** live in the repository.

On each server:

1. Copy `API/db_local.example.php` → `API/db_local.php`
2. Set `$dbHost`, `$dbUser`, `$dbPass`, `$dbName` for that environment
3. Ensure `API/db_local.php` is **never** committed (listed in root `.gitignore`)

Alternatively set environment variables: `DB_HOST`, `DB_USER`, `DB_PASS`, `DB_NAME`.

All apps (POS, Owner, Dealer, Admin) share one database via `API/db_connection.php`.

## Schema reference

- **Structure only:** `API/schema/schema_reference.sql` (no customer/licence rows)
- **Single-file upgrade (recommended):** `API/migrations/server_upgrade_all.sql`
  - Combines P3-1 → P3-7 → P5-3 → P6 (licensing) → P7 (multi-branch) + sync indexes
  - **One import only** — no separate `p7_multi_branch_scope.sql` needed
  - **Keeps all existing data** (shops, licences, categories, products, bills)
  - New columns are nullable — old products work without subcategory/portion
  - Additive only; column adds skipped if already present; safe to re-run
  - **phpMyAdmin:** select database → Import → choose this file → Go
  - **CLI:** `mysql -u USER -p DATABASE < API/migrations/server_upgrade_all.sql`
  - After import: all `*_ok` = `1`, and before/after row counts must match

### Old users after upgrade

| Existing data | After SQL |
|---------------|-----------|
| Categories / products / bills | Unchanged (same rows) |
| Product price | Still used if no portions added |
| Subcategory | Optional (`NULL` until shop adds one) |
| Portions | Optional (shop adds Half/Full etc. in new app) |
| Food / Beverage | Categories mapped; drink-like names → Beverage |

Old APKs can keep syncing; new APK unlocks subcategory chips, portions, Food/Beverage on the **same** database.

### Individual migration files (optional / reference)

```text
1. API/migrations/p3_1_food_types.sql
2. API/migrations/p3_2_product_subcategories.sql
3. API/migrations/p3_3_product_portions.sql
4. API/migrations/p3_4_bill_line_snapshots.sql
5. API/migrations/p3_7_beverage_category_mapping.sql
6. API/migrations/p5_3_api_tokens.sql
7. API/migrations/p6_production_licensing.sql
8. API/migrations/p7_multi_branch_scope.sql
```

`p4_2_trial_limits_note.sql` is documentation-only (no DDL). Trial limits are enforced in PHP (`insertInvoice.php`, auth responses).

After migrations, deploy updated PHP endpoints for catalog sync (`getSubcategoryList`, `getPortionList`, `insertSubcategory`, `insertPortion`, etc.) and auth (`auth_tokens.php`, login token issuance).

## After removing the old production dump from git

If `spllmgkn_posbill.sql` was ever pushed or shared:

1. **Rotate** the MySQL user password on the hosting panel
2. Update `API/db_local.php` on the server with the new password
3. Treat licence keys, device IDs, MPINs, and contact numbers from that dump as **compromised** — renew or invalidate where practical
4. Change the Admin account password in `users` (role_id = 1) if the bcrypt hash was exposed

## Full backups

Keep full SQL backups **off-repo** (encrypted storage, hosting backup, or local secure copy only).
