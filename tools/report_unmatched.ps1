param([string[]]$Paths)
$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$cssPath = 'E:\My-Speed-Test-App\web\css\mr-speed-theme.css'

$css = [System.IO.File]::ReadAllText($cssPath, [System.Text.Encoding]::UTF8)
$css = [regex]::Replace($css, '/\*.*?\*/', ' ', 'Singleline')
function Norm-Decl { param([string]$d)
  $d = ($d -replace '\s+',' ').Trim()
  if ($d -match '^(.*?)\s*:\s*(.*)$') { return ("{0}: {1}" -f $matches[1].Trim(), $matches[2].Trim()) }
  return $d
}
$ruleMap = @{}
foreach ($m in [regex]::Matches($css, '([^{}]+)\{([^{}]*)\}', 'Singleline')) {
  $sel = $m.Groups[1].Value.Trim(); $body = $m.Groups[2].Value
  if ($body -notmatch '!important') { continue }
  if ($sel -notmatch '^\.([a-zA-Z0-9_-]+)$') { continue }
  $cls = $matches[1]
  $decls = @()
  foreach ($d in ($body -split ';')) { $d = ($d -replace '\s*!important\s*$','').Trim(); if ($d -ne '') { $decls += (Norm-Decl $d) } }
  if ($decls.Count -eq 0) { continue }
  if ($ruleMap.ContainsKey($cls)) { continue }
  $ruleMap[$cls] = $decls
}
Write-Output "Parsed: $($ruleMap.Count)"

$missing = @{}   # decl -> count
$styles = @{}    # full style string -> count
$expanded = @()
foreach ($p in ($Paths -split ',')) {
  $p = $p.Trim()
  if ($p -eq '') { continue }
  $base = if ([System.IO.Path]::IsPathRooted($p)) { $p } else { "E:\My-Speed-Test-App\web\$p" }
  if ($base -match '\*') {
    $dir = Split-Path $base -Parent
    $pat = Split-Path $base -Leaf
    $expanded += (Get-ChildItem -Path $dir -Filter $pat -File | ForEach-Object { $_.FullName })
  } else { $expanded += $base }
}
foreach ($full in $expanded) {
  $text = [System.IO.File]::ReadAllText($full, [System.Text.Encoding]::UTF8)
  foreach ($m in [regex]::Matches($text, 'style="([^"]*)"')) {
    $raw = $m.Groups[1].Value
    if ($raw -match '<\?|\?>') { continue }
    $decls = @()
    foreach ($d in ($raw -split ';')) { $d = ($d -replace '\s*!important\s*$','').Trim(); if ($d -ne '') { $decls += (Norm-Decl $d) } }
    if ($decls.Count -eq 0) { continue }
    # check full coverage
    $covered = $false
    foreach ($cls in $ruleMap.Keys) {
      $dl = $ruleMap[$cls]
      if (@($decls | Where-Object { $dl -notcontains $_ }).Count -eq 0) { $covered = $true; break }
    }
    if ($covered) { continue }
    $styles[$raw] = $styles[$raw] + 1
    foreach ($d in $decls) {
      $has = $false
      foreach ($cls in $ruleMap.Keys) { if ($ruleMap[$cls] -contains $d) { $has = $true; break } }
      if (-not $has) { $missing[$d] = $missing[$d] + 1 }
    }
  }
}
Write-Output "`n=== UNCOVERED STYLE PATTERNS ==="
$styles.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object { "{0,3}x  {1}" -f $_.Value, $_.Key }
Write-Output "`n=== MISSING SINGLE DECLARATIONS (need new classes) ==="
$missing.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object { "{0,3}x  {1}" -f $_.Value, $_.Key }
