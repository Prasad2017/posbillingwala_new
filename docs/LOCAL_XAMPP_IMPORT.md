# XAMPP — import database `posbillingwala`

## Ready-made file (recommended)

Use the fixed dump in this repo:

**`database/posbillingwala_xampp.sql`**

Built from your server export with:
- Database `posbillingwala` created automatically
- `FOREIGN_KEY_CHECKS = 0/1` (not invalid ON/OFF)
- XAMPP-compatible header

### One-click import

```powershell
.\scripts\import-xampp.ps1
```

Or rebuild from original dump then import:

```powershell
.\scripts\build-xampp-sql.js
.\scripts\import-xampp.ps1
```

---

## Manual import


MySQL accepts **0** and **1**, not `ON` / `OFF`.

**Fix:** Run on your dump file:

```powershell
.\scripts\fix-sql-dump.ps1 "C:\path\to\rgusomuk_posbilling (2).sql"
```

Then import `*_fixed.sql` in phpMyAdmin.

Or find/replace in editor:

| Wrong | Correct |
|-------|---------|
| `SET FOREIGN_KEY_CHECKS = ON;` | `SET FOREIGN_KEY_CHECKS = 1;` |
| `SET FOREIGN_KEY_CHECKS = OFF;` | `SET FOREIGN_KEY_CHECKS = 0;` |

---

## Error 2: `#2006 - MySQL server has gone away`

Large dump + phpMyAdmin limits. MySQL connection drops mid-import.

### Step A — Increase XAMPP limits

**1. MySQL** — edit `C:\xampp\mysql\bin\my.ini` (under `[mysqld]`):

```ini
max_allowed_packet=512M
wait_timeout=600
net_read_timeout=600
net_write_timeout=600
innodb_buffer_pool_size=256M
```

**2. PHP** — edit `C:\xampp\php\php.ini`:

```ini
upload_max_filesize=512M
post_max_size=512M
memory_limit=512M
max_execution_time=600
max_input_time=600
```

**3.** XAMPP Control Panel → **Stop** MySQL → **Start** MySQL (and Apache if using phpMyAdmin).

### Step B — Create database first

phpMyAdmin → SQL:

```sql
CREATE DATABASE IF NOT EXISTS posbillingwala
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Step C — Import via command line (best for large files)

```powershell
C:\xampp\mysql\bin\mysql.exe -u root -e "SET GLOBAL max_allowed_packet=1073741824;"
C:\xampp\mysql\bin\mysql.exe -u root --max-allowed-packet=512M posbillingwala < "database\posbillingwala_xampp.sql"
```

If root has a password:

```powershell
.\mysql.exe -u root -p posbillingwala < "C:\path\to\rgusomuk_posbilling_fixed.sql"
```

### Step D — Admin `.env` for local XAMPP

In `admin.posbillingwala.com\.env`:

```env
APP_ENV=local
APP_DEBUG=true
APP_URL=http://127.0.0.1:8000
ASSET_URL=http://127.0.0.1:8000

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=posbillingwala
DB_USERNAME=root
DB_PASSWORD=
```

Then:

```powershell
cd admin.posbillingwala.com
C:\xampp\php\php.exe artisan config:clear
C:\xampp\php\php.exe artisan serve --host=127.0.0.1 --port=8000
```

Website CMS tables missing? Run once:

```powershell
C:\xampp\mysql\bin\mysql.exe -u root posbillingwala < "API\migrations\p23_website_catalog.sql"
```

---

## Quick checklist

1. Fix SQL with `fix-sql-dump.ps1`
2. Update `my.ini` + `php.ini`, restart MySQL
3. Create database `posbillingwala`
4. Import via **mysql.exe** command line (not phpMyAdmin upload if file is big)
5. Point admin `.env` to `root` / empty password
6. Run `.\scripts\start-local.ps1`
