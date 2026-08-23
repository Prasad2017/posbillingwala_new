# Converts hardcoded android:text and android:hint in layouts to @string resources
# Generates values/strings_ui_generated.xml and updates layout files

$ErrorActionPreference = "Stop"
$layoutDir = Join-Path $PSScriptRoot "..\app\src\main\res\layout"
$stringsEn = Join-Path $PSScriptRoot "..\app\src\main\res\values\strings_ui.xml"
$stringsHi = Join-Path $PSScriptRoot "..\app\src\main\res\values-hi\strings_ui.xml"
$stringsMr = Join-Path $PSScriptRoot "..\app\src\main\res\values-mr\strings_ui.xml"

# Skip patterns - not user-facing or already resources
function Should-SkipText([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return $true }
    if ($text -match '^@string/') { return $true }
    if ($text -match '^[\d\s\.,+\-=*%$]+$') { return $true }
    if ($text -eq 'POS') { return $true }
    if ($text -match '^tools:') { return $true }
    if ($text.Length -eq 1 -and $text -notmatch '[A-Za-z]') { return $true }
    return $false
}

function Make-Key([string]$text) {
    $t = $text -replace '\\n', ' ' -replace '\s+', ' '
    $t = $t.Trim().ToLower()
    $t = $t -replace '[^\w\s]', ''
    $t = $t -replace '\s+', '_'
    if ($t.Length -gt 40) { $t = $t.Substring(0, 40) }
    if ($t -match '^\d') { $t = "ui_$t" }
    if ([string]::IsNullOrWhiteSpace($t)) { $t = "ui_text" }
    return "ui_$t"
}

function Escape-Xml([string]$s) {
    if ($null -eq $s) { return '' }
    $s = $s -creplace '&(?!amp;|lt;|gt;|quot;|apos;)', '&amp;'
    $s = $s -replace '<', '&lt;'
    $s = $s -replace '>', '&gt;'
    $s = $s -replace '"', '&quot;'
    return $s
}

# Known semantic mappings (prefer existing keys)
$existingKeys = @{}
$baseStrings = Join-Path $PSScriptRoot "..\app\src\main\res\values\strings.xml"
if (Test-Path $baseStrings) {
    $content = Get-Content $baseStrings -Raw -Encoding UTF8
    [regex]::Matches($content, '<string name="([^"]+)">([^<]*)</string>') | ForEach-Object {
        $val = $_.Groups[2].Value -replace '\\n', "`n" -replace "\\'", "'"
        $existingKeys[$val.Trim()] = $_.Groups[1].Value
        # also without newlines normalized
        $norm = ($val -replace '\\n', ' ' -replace '\s+', ' ').Trim()
        if (-not $existingKeys.ContainsKey($norm)) { $existingKeys[$norm] = $_.Groups[1].Value }
    }
}

$textToKey = @{}  # normalized text -> key
$keyToText = @{}  # key -> english text

function Get-OrCreateKey([string]$text) {
    $norm = ($text -replace '\\n', "`n").Trim()
    $normFlat = ($text -replace '\\n', ' ' -replace '\s+', ' ').Trim()
    if ($existingKeys.ContainsKey($norm)) { return $existingKeys[$norm] }
    if ($existingKeys.ContainsKey($normFlat)) { return $existingKeys[$normFlat] }
    if ($textToKey.ContainsKey($normFlat)) { return $textToKey[$normFlat] }
    $key = Make-Key $normFlat
    $base = $key
    $i = 2
    while ($keyToText.ContainsKey($key)) {
        if ($keyToText[$key] -eq $norm) { return $key }
        $key = "${base}_$i"; $i++
    }
    $textToKey[$normFlat] = $key
    $keyToText[$key] = $norm
    return $key
}

$attrPattern = 'android:(text|hint)="([^"]*)"'
$files = Get-ChildItem $layoutDir -Filter "*.xml" -Recurse
$replaceCount = 0

foreach ($file in $files) {
    $utf8 = New-Object System.Text.UTF8Encoding $false
    $content = [System.IO.File]::ReadAllText($file.FullName, $utf8)
    $changed = $false
    $newContent = [regex]::Replace($content, $attrPattern, {
        param($m)
        $attr = $m.Groups[1].Value
        $text = $m.Groups[2].Value
        if (Should-SkipText $text) { return $m.Value }
        $key = Get-OrCreateKey $text
        $script:replaceCount++
        return "android:${attr}=`"@string/$key`""
    })
    if ($newContent -ne $content) {
        [System.IO.File]::WriteAllText($file.FullName, $newContent, $utf8)
    }
}

Write-Host "Layout replacements: $replaceCount"
Write-Host "Unique new strings: $($keyToText.Count)"

# Write English strings_ui.xml
$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine('<?xml version="1.0" encoding="utf-8"?>')
[void]$sb.AppendLine('<resources>')
[void]$sb.AppendLine('    <!-- Auto-generated UI strings - do not edit by hand; re-run scripts/localize_all.ps1 -->')
foreach ($kv in ($keyToText.GetEnumerator() | Sort-Object Key)) {
    $escaped = Escape-Xml $kv.Value
    $escaped = $escaped -replace "`n", '\n' -replace "`r", ''
    [void]$sb.AppendLine("    <string name=`"$($kv.Key)`">$escaped</string>")
}
[void]$sb.AppendLine('</resources>')
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($stringsEn, $sb.ToString(), $utf8NoBom)
Write-Host "Wrote $stringsEn"

# Hindi/Marathi: load translation dictionary if present, else copy English
$dictPath = Join-Path $PSScriptRoot "i18n_hi_mr.json"
if (Test-Path $dictPath) {
    $dict = Get-Content $dictPath -Raw -Encoding UTF8 | ConvertFrom-Json
    function Write-LocaleFile($path, $lang) {
        $sb2 = New-Object System.Text.StringBuilder
        [void]$sb2.AppendLine('<?xml version="1.0" encoding="utf-8"?>')
        [void]$sb2.AppendLine('<resources>')
        foreach ($kv in ($keyToText.GetEnumerator() | Sort-Object Key)) {
            $en = $kv.Value
            $trans = $en
            if ($dict.$lang.PSObject.Properties.Name -contains $en) {
                $trans = $dict.$lang.$en
            } elseif ($dict.$lang.PSObject.Properties.Name -contains ($en -replace '\\n', ' ')) {
                $trans = $dict.$lang.($en -replace '\\n', ' ')
            }
            $escaped = Escape-Xml $trans
            $escaped = $escaped -replace "`n", '\n'
            [void]$sb2.AppendLine("    <string name=`"$($kv.Key)`">$escaped</string>")
        }
        [void]$sb2.AppendLine('</resources>')
        [System.IO.File]::WriteAllText($path, $sb2.ToString(), $utf8NoBom)
        Write-Host "Wrote $path"
    }
    Write-LocaleFile $stringsHi "hi"
    Write-LocaleFile $stringsMr "mr"
} else {
    Copy-Item $stringsEn $stringsHi -Force
    Copy-Item $stringsEn $stringsMr -Force
    Write-Host "No i18n_hi_mr.json - copied English to hi/mr (add dictionary for translations)"
}

Write-Host "Done."
