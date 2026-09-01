# POS Billingwala — local preview (website + admin)
# Usage:  .\scripts\start-local.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$WebsiteDir = Join-Path $Root "website"
$AdminDir = Join-Path $Root "admin.posbillingwala.com"
$ServeScript = Join-Path $Root "scripts\serve-website.js"
$WebsiteUrl = "http://127.0.0.1:8080"
$AdminUrl = "http://127.0.0.1:8000"

Write-Host ""
Write-Host " POS Billingwala — Local Preview" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

function Ensure-AdminAssets {
    param([string]$AdminDir)
    $pub = Join-Path $AdminDir "public\assets"
    $src = Join-Path $AdminDir "assets"
    $marker = Join-Path $pub "css\app.css"
    if ((Test-Path $marker)) { return }
    if (Test-Path $pub) { Remove-Item $pub -Recurse -Force -ErrorAction SilentlyContinue }
    if (Test-Path $src) {
        New-Item -ItemType Junction -Path $pub -Target $src | Out-Null
        Write-Host "Linked public/assets -> assets (admin CSS/JS)" -ForegroundColor DarkGray
    }
}

    $candidates = @(
        "php",
        "C:\laragon\bin\php\php-8.2.12-Win32-vs16-x64\php.exe",
        "C:\laragon\bin\php\php-8.3.0-Win32-vs16-x64\php.exe",
        "C:\xampp\php\php.exe",
        "C:\wamp64\bin\php\php8.2.0\php.exe"
    )
    foreach ($c in $candidates) {
        if ($c -eq "php") {
            $cmd = Get-Command $c -ErrorAction SilentlyContinue
            if ($cmd) { return $cmd.Source }
        } elseif (Test-Path $c) {
            return $c
        }
    }
    return $null
}

$NodeCmd = Get-Command node -ErrorAction SilentlyContinue
if (-not $NodeCmd) {
    Write-Host "ERROR: Node.js not found. Install from https://nodejs.org/" -ForegroundColor Red
    exit 1
}

$PhpExe = Find-Php
$StartAdmin = $false

if ($PhpExe) {
    $StartAdmin = $true
    Write-Host "PHP found:  $PhpExe" -ForegroundColor DarkGray
} else {
    Write-Host "WARNING: PHP not found — website only (mock data)." -ForegroundColor Yellow
    Write-Host "Install Laragon/XAMPP or add php to PATH for admin panel." -ForegroundColor Yellow
}

Write-Host "Starting website on     $WebsiteUrl" -ForegroundColor Green
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$Root'; Write-Host 'Website: $WebsiteUrl' -ForegroundColor Cyan; node '$ServeScript' 8080"
)

if ($StartAdmin) {
    Ensure-AdminAssets -AdminDir $AdminDir
    Start-Sleep -Seconds 1
    Write-Host "Starting admin panel on  $AdminUrl" -ForegroundColor Green
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$AdminDir'; Write-Host 'Admin: $AdminUrl' -ForegroundColor Cyan; & '$PhpExe' artisan serve --host=127.0.0.1 --port=8000"
    )
    Start-Sleep -Seconds 2
}

Start-Process $WebsiteUrl
if ($StartAdmin) {
    Start-Process "$AdminUrl/login"
}

Write-Host ""
Write-Host " Website:  $WebsiteUrl" -ForegroundColor White
if ($StartAdmin) {
    Write-Host " Admin:    $AdminUrl/login" -ForegroundColor White
    Write-Host " CMS:      $AdminUrl/website" -ForegroundColor White
} else {
    Write-Host " Admin:    skipped (install PHP)" -ForegroundColor DarkYellow
}
Write-Host ""
Write-Host "Mock data loads automatically when admin API is offline." -ForegroundColor DarkGray
Write-Host "Keep PowerShell windows open. Ctrl+C in each to stop." -ForegroundColor DarkGray
Write-Host ""
