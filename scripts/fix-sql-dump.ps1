# Fix common MySQL dump issues before phpMyAdmin import (XAMPP)
# Usage: .\scripts\fix-sql-dump.ps1 "C:\path\to\rgusomuk_posbilling (2).sql"

param(
    [Parameter(Mandatory = $true)]
    [string]$InputFile
)

if (-not (Test-Path $InputFile)) {
    Write-Host "File not found: $InputFile" -ForegroundColor Red
    exit 1
}

$dir = Split-Path -Parent $InputFile
$base = [IO.Path]::GetFileNameWithoutExtension($InputFile)
$out = Join-Path $dir ($base + "_fixed.sql")

Write-Host "Reading: $InputFile" -ForegroundColor Cyan
$content = [IO.File]::ReadAllText($InputFile)

# MySQL uses 0/1 — not ON/OFF (phpMyAdmin static analysis error)
$content = $content -replace 'FOREIGN_KEY_CHECKS\s*=\s*ON\b', 'FOREIGN_KEY_CHECKS = 1'
$content = $content -replace 'FOREIGN_KEY_CHECKS\s*=\s*OFF\b', 'FOREIGN_KEY_CHECKS = 0'
$content = $content -replace 'SQL_MODE\s*=\s*NO_AUTO_VALUE_ON_ZERO\s*,', 'SQL_MODE = ''NO_AUTO_VALUE_ON_ZERO'','

[IO.File]::WriteAllText($out, $content)

$mb = [math]::Round((Get-Item $out).Length / 1MB, 2)
Write-Host "Saved:  $out ($mb MB)" -ForegroundColor Green
Write-Host ""
Write-Host "Import this fixed file instead." -ForegroundColor Yellow
Write-Host "If file is large (>50 MB), use command line (see docs/LOCAL_XAMPP_IMPORT.md)." -ForegroundColor Yellow
