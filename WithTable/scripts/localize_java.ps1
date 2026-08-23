# Replaces hardcoded Toast and simple dialog strings in Java with getString(R.string.*)
$ErrorActionPreference = "Stop"
$javaDir = Join-Path $PSScriptRoot "..\app\src\main\java"
$stringsUi = Join-Path $PSScriptRoot "..\app\src\main\res\values\strings_ui.xml"

# Load string keys from strings.xml + strings_ui.xml
$keyByText = @{}
function Load-Strings($path) {
    if (-not (Test-Path $path)) { return }
    $content = Get-Content $path -Raw -Encoding UTF8
    [regex]::Matches($content, '<string name="([^"]+)">([^<]*)</string>') | ForEach-Object {
        $val = $_.Groups[2].Value -replace '\\n', "`n" -replace "\\'", "'"
        $norm = ($val -replace '\s+', ' ').Trim()
        if (-not $keyByText.ContainsKey($norm)) {
            $keyByText[$norm] = $_.Groups[1].Value
        }
    }
}
Load-Strings (Join-Path $PSScriptRoot "..\app\src\main\res\values\strings.xml")
Load-Strings $stringsUi

$newStrings = @{}
function Get-Key([string]$text) {
    $norm = ($text -replace '\s+', ' ').Trim()
    if ($keyByText.ContainsKey($norm)) { return $keyByText[$norm] }
    $t = $norm.ToLower() -replace '[^\w\s]', '' -replace '\s+', '_'
    if ($t.Length -gt 40) { $t = $t.Substring(0, 40) }
    $key = "toast_$t"
    $base = $key; $i = 2
    while ($newStrings.ContainsKey($key) -and $newStrings[$key] -ne $norm) {
        $key = "${base}_$i"; $i++
    }
    $newStrings[$key] = $norm
    $keyByText[$norm] = $key
    return $key
}

$javaFiles = Get-ChildItem $javaDir -Filter "*.java" -Recurse
$replaceCount = 0

# Toast.makeText(context, "message", ...)
$toastPattern = 'Toast\.makeText\(([^,]+),\s*"((?:\\.|[^"\\])*)"\s*,'
# setTitle("...")
$titlePattern = 'setTitle\("((?:\\.|[^"\\])*)"\)'
# setMessage("...")
$msgPattern = 'setMessage\("((?:\\.|[^"\\])*)"\)'
# setText("...") - only simple literals in common UI files
$setTextPattern = '\.setText\("((?:\\.|[^"\\])*)"\)'

foreach ($file in $javaFiles) {
    $utf8 = New-Object System.Text.UTF8Encoding $false
    $content = [System.IO.File]::ReadAllText($file.FullName, $utf8)
    $orig = $content

    $content = [regex]::Replace($content, $toastPattern, {
        param($m)
        $ctx = $m.Groups[1].Value
        $text = $m.Groups[2].Value -replace '\\"', '"' -replace '\\n', "`n"
        if ($text.Length -lt 2) { return $m.Value }
        $key = Get-Key $text
        $script:replaceCount++
        return "Toast.makeText($ctx, getString(R.string.$key),"
    })

    # Only replace setTitle/setMessage if file has getString or extends Fragment/Activity
    if ($content -match 'getString\(R\.string') {
        $content = [regex]::Replace($content, $titlePattern, {
            param($m)
            $text = $m.Groups[1].Value -replace '\\"', '"'
            $key = Get-Key $text
            $script:replaceCount++
            return "setTitle(getString(R.string.$key))"
        })
        $content = [regex]::Replace($content, $msgPattern, {
            param($m)
            $text = $m.Groups[1].Value -replace '\\"', '"'
            $key = Get-Key $text
            $script:replaceCount++
            return "setMessage(getString(R.string.$key))"
        })
    }

    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8)
    }
}

# Append new toast strings to strings_ui.xml
if ($newStrings.Count -gt 0) {
    $uiPath = $stringsUi
    if (-not (Test-Path $uiPath)) {
        $uiPath = Join-Path $PSScriptRoot "..\app\src\main\res\values\strings_ui.xml"
    }
    $existing = if (Test-Path $uiPath) { Get-Content $uiPath -Raw -Encoding UTF8 } else { "<resources>`n</resources>" }
    $insert = ""
    foreach ($kv in ($newStrings.GetEnumerator() | Sort-Object Key)) {
        $esc = $kv.Value.Replace('&','&amp;').Replace('<','&lt;').Replace('>','&gt;').Replace('"','&quot;').Replace("'","\'")
        $insert += "    <string name=`"$($kv.Key)`">$esc</string>`n"
    }
    $existing = $existing -replace '</resources>', "$insert</resources>"
    [System.IO.File]::WriteAllText($uiPath, $existing, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "Added $($newStrings.Count) Java strings to strings_ui.xml"
}

Write-Host "Java replacements: $replaceCount"
Write-Host "Done."
