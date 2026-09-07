# 20 Database Api Architecture

## Stack
- POS SQLite: `POSBillingWalaDatabase` (additive `addColumnIfNotExists`)
- API: `API/*.php` + `auth_tokens.php`
- MySQL shared by apps + web admin
- Migrations: `API/migrations/`

## Facade
`Extra/DatabaseApiArchitecture.java`

## Hard rule
Additive upgrades only — no DROP TABLE / data wipe paths for production.
