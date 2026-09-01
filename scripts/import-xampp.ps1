# Import posbillingwala_xampp.sql into XAMPP MySQL
# Usage: .\scripts\import-xampp.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$SqlFile = Join-Path $Root "database\posbillingwala_xampp.sql"
$Mysql = "C:\xampp\mysql\bin\mysql.exe"
$Db = "posbillingwala"

if (-not (Test-Path $Mysql)) {
    Write-Host "XAMPP MySQL not found at $Mysql" -ForegroundColor Red
    Write-Host "Start MySQL in XAMPP Control Panel first." -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $SqlFile)) {
    Write-Host "SQL file missing. Building from server dump..." -ForegroundColor Yellow
    node (Join-Path $Root "scripts\build-xampp-sql.js")
}

Write-Host "Increasing max_allowed_packet..." -ForegroundColor Cyan
& $Mysql -u root -e "SET GLOBAL max_allowed_packet=1073741824;"

Write-Host "Recreating database '$Db'..." -ForegroundColor Cyan
& $Mysql -u root -e "DROP DATABASE IF EXISTS ``$Db``; CREATE DATABASE ``$Db`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

Write-Host "Importing (may take 1-2 minutes)..." -ForegroundColor Cyan
cmd /c "`"$Mysql`" -u root --max-allowed-packet=512M $Db < `"$SqlFile`""
if ($LASTEXITCODE -ne 0) {
    Write-Host "Import failed." -ForegroundColor Red
    exit 1
}

$info = & $Mysql -u root $Db -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Db'; SELECT COUNT(*) FROM users;"
Write-Host ""
Write-Host "Import OK — database: $Db" -ForegroundColor Green
Write-Host "Tables: $($info[0]) | Users: $($info[1])" -ForegroundColor Green
Write-Host ""
Write-Host "Admin .env should use:" -ForegroundColor Yellow
Write-Host "  DB_DATABASE=posbillingwala" -ForegroundColor White
Write-Host "  DB_USERNAME=root" -ForegroundColor White
Write-Host "  DB_PASSWORD=" -ForegroundColor White
