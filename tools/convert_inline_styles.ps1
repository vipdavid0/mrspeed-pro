#
# convert_inline_styles.ps1
# -------------------
# Migrates inline `style="..."` attributes to atomic utility classes that already
# exist in css/mr-speed-theme.css (all of which use !important).
#
# Safety rules:
#  * Only ATOMIC (single-declaration) existing classes are reused.
#  * Style attributes containing PHP code are left untouched.
#  * Style attributes living inside a PHP string literal keep the PHP string
#    valid: the injected class attribute always uses a quote character that
#    differs from the enclosing PHP string's quote.
#  * Missing declarations get a generated `u-<prop>-<value>` class appended to
#    the stylesheet. If the element also carries a component class or an id, the
#    generated rule is written as a higher-specificity compound selector so it
#    wins over the component's own !important declarations.
#  * Every modified file is validated with `php -l`; on failure the file is
#    rolled back to its original content.
#
param(
  [string[]]$Paths,
  [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$cssPath = 'E:\My-Speed-Test-App\web\css\mr-speed-theme.css'
$webRoot = 'E:\My-Speed-Test-App\web'

function Norm-Decl {
  param([string]$d)
  $d = ($d -replace '\s+',' ').Trim()
  if ($d -match '^(.*?)\s*:\s*(.*)$') { return ("{0}: {1}" -f $matches[1].Trim(), $matches[2].Trim()) }
  return $d
}

# ---------- Build CSS class map ----------
$css = [System.IO.File]::ReadAllText($cssPath, [System.Text.Encoding]::UTF8)
$cssClean = [regex]::Replace($css, '/\*.*?\*/', ' ', 'Singleline')
$atomic = @{}
$usedNames = @{}
foreach ($m in [regex]::Matches($cssClean, '([^{}]+)\{([^{}]*)\}', 'Singleline')) {
  $sel = $m.Groups[1].Value.Trim(); $body = $m.Groups[2].Value
  if ($sel -notmatch '^\.([a-zA-Z0-9_-]+)$') { continue }
  $cls = $matches[1]
  $usedNames[$cls] = $true
  if ($body -notmatch '!important') { continue }
  $decls = @()
  foreach ($d in ($body -split ';')) { $d = ($d -replace '\s*!important\s*$','').Trim(); if ($d -ne '') { $decls += (Norm-Decl $d) } }
  if ($decls.Count -ne 1) { continue }   # atomic only
  if ($atomic.ContainsKey($cls)) { continue }
  $atomic[$cls] = $decls[0]
}
$declToClass = @{}
foreach ($k in $atomic.Keys) {
  if (-not $declToClass.ContainsKey($atomic[$k])) { $declToClass[$atomic[$k]] = @() }
  $declToClass[$atomic[$k]] += $k
}
foreach ($k in @($declToClass.Keys)) {
  $declToClass[$k] = @($declToClass[$k] | Sort-Object @{E={$_.Length}}, @{E={if($_ -like 'u-*'){1}else{0}}}, @{E={$_}})
}
Write-Output "Atomic utilities: $($declToClass.Count) declarations / $($atomic.Count) classes"

function New-ClassName {
  param([string]$decl)
  $prop = $decl; $val = ''
  if ($decl -match '^(.*?):\s*(.*)$') { $prop = $matches[1]; $val = $matches[2] }
  $p = ($prop -replace '[^a-zA-Z0-9]','')
  $v = ($val -replace '[^a-zA-Z0-9]','')
  if ($v.Length -gt 20) { $v = $v.Substring(0,20) }
  $base = "u-$p-$v"
  if ($v -eq 'italic') { $base = 'u-italic-c' }
  $name = $base; $n = 2
  while ($script:usedNames.ContainsKey($name)) { $name = "$base-$n"; $n++ }
  $script:usedNames[$name] = $true
  return $name
}

# ---------- PHP string-literal spans (so we never break PHP quoting) ----------
function Get-PhpStringSpans {
  param([string]$text)
  $spans = New-Object System.Collections.Generic.List[object]
  foreach ($reg in [regex]::Matches($text, '<\?(?:php|=)?(.*?)(?:\?>|$)', 'Singleline')) {
    $code = $reg.Groups[1].Value
    $base = $reg.Groups[1].Index
    $i = 0
    while ($i -lt $code.Length) {
      $c = $code[$i]
      if ($c -eq "'" -or $c -eq '"' -or $c -eq '`') {
        $q = $c; $j = $i + 1
        while ($j -lt $code.Length) {
          if ($code[$j] -eq '\') { $j += 2; continue }
          if ($code[$j] -eq $q) { break }
          $j++
        }
        $spans.Add([pscustomobject]@{ Start=($base + $i); End=($base + [Math]::Min($j, $code.Length-1)); Quote=$q })
        $i = $j + 1; continue
      }
      if ($c -eq '/' -and $i + 1 -lt $code.Length -and $code[$i+1] -eq '/') { while ($i -lt $code.Length -and $code[$i] -ne "`n") { $i++ }; continue }
      if ($c -eq '#') { while ($i -lt $code.Length -and $code[$i] -ne "`n") { $i++ }; continue }
      if ($c -eq '/' -and $i + 1 -lt $code.Length -and $code[$i+1] -eq '*') { $e = $code.IndexOf('*/', $i+2); if ($e -lt 0) { break }; $i = $e + 2; continue }
      $i++
    }
  }
  return $spans
}

function Get-PhpQuoteAt {
  param($spans, [int]$index)
  foreach ($s in $spans) {
    if ($index -gt $s.Start -and $index -lt $s.End) { return $s.Quote }
  }
  return $null
}

# ---------- Find style="..." spans, PHP-aware ----------
function Find-StyleAttrs {
  param([string]$text)
  $res = New-Object System.Collections.Generic.List[object]
  $i = 0; $n = $text.Length
  while ($i -lt $n) {
    $idx = $text.IndexOf('style=', $i)
    if ($idx -lt 0) { break }
    $j = $idx + 6
    while ($j -lt $n -and ($text[$j] -eq ' ' -or $text[$j] -eq "`t")) { $j++ }
    if ($j -ge $n -or ($text[$j] -ne '"' -and $text[$j] -ne "'")) { $i = $idx + 6; continue }
    $q = $text[$j]
    $k = $j + 1; $hasPhp = $false
    while ($k -lt $n) {
      if ($text[$k] -eq '<' -and $k + 1 -lt $n -and $text[$k+1] -eq '?') {
        $end = $text.IndexOf('?>', $k + 2); if ($end -lt 0) { $k = $n; break }
        $hasPhp = $true; $k = $end + 2; continue
      }
      if ($text[$k] -eq $q) { break }
      $k++
    }
    if ($k -ge $n) { break }
    $res.Add([pscustomobject]@{ Start=$idx; ContentStart=$j+1; ContentEnd=$k; Quote=$q; HasPhp=$hasPhp; AttrEnd=$k+1 })
    $i = $k + 1
  }
  return $res
}

# ---------- Locate an attribute inside a tag prefix, quote- and PHP-aware ----------
function Find-Attr {
  param([string]$tag, [string]$name)
  $i = 0; $n = $tag.Length
  while ($i -lt $n) {
    $idx = $tag.IndexOf($name, $i)
    if ($idx -lt 0) { return $null }
    # must be a standalone attribute name
    $before = if ($idx -gt 0) { $tag[$idx-1] } else { ' ' }
    if ($before -notmatch '\s') { $i = $idx + $name.Length; continue }
    $j = $idx + $name.Length
    if ($j -ge $n) { return $null }
    # skip whitespace, then expect '='
    while ($j -lt $n -and ($tag[$j] -eq ' ' -or $tag[$j] -eq "`t")) { $j++ }
    if ($j -ge $n -or $tag[$j] -ne '=') { $i = $idx + $name.Length; continue }
    $j++
    while ($j -lt $n -and ($tag[$j] -eq ' ' -or $tag[$j] -eq "`t")) { $j++ }
    if ($j -ge $n) { return $null }
    if ($tag[$j] -eq '"' -or $tag[$j] -eq "'") {
      $q = $tag[$j]
      $k = $j + 1
      while ($k -lt $n) {
        if ($tag[$k] -eq '<' -and $k + 1 -lt $n -and $tag[$k+1] -eq '?') {
          $e = $tag.IndexOf('?>', $k + 2); if ($e -lt 0) { $k = $n; break }
          $k = $e + 2; continue
        }
        if ($tag[$k] -eq $q) { break }
        $k++
      }
      if ($k -ge $n) { return $null }
      return [pscustomobject]@{
        Start = $idx
        Length = ($k + 1 - $idx)
        Quote = $q
        Value = $tag.Substring($j + 1, $k - $j - 1)
      }
    } else {
      # unquoted value
      $k = $j
      while ($k -lt $n -and $tag[$k] -notmatch '\s') { $k++ }
      return [pscustomobject]@{
        Start = $idx
        Length = ($k - $idx)
        Quote = ''
        Value = $tag.Substring($j, $k - $j)
      }
    }
  }
  return $null
}

$newRules = New-Object System.Collections.Generic.List[string]
$newRuleKeys = @{}
$total = 0
$skipped = 0

$expanded = @()
foreach ($p in ($Paths -split ',')) {
  $p = $p.Trim(); if ($p -eq '') { continue }
  $base = if ([System.IO.Path]::IsPathRooted($p)) { $p } else { "$webRoot\$p" }
  if ($base -match '\*') {
    $dir = Split-Path $base -Parent; $pat = Split-Path $base -Leaf
    $expanded += (Get-ChildItem -Path $dir -Filter $pat -File -Recurse | ForEach-Object { $_.FullName })
  } else { $expanded += $base }
}

foreach ($full in $expanded) {
  $text = [System.IO.File]::ReadAllText($full, [System.Text.Encoding]::UTF8)
  $orig = $text
  $count = 0
  $out = New-Object System.Text.StringBuilder
  $pos = 0
  $phpSpans = Get-PhpStringSpans $text

  foreach ($sa in (Find-StyleAttrs $text)) {
    if ($sa.HasPhp) { $skipped++; continue }   # PHP-interpolated: leave alone
    $raw = $text.Substring($sa.ContentStart, $sa.ContentEnd - $sa.ContentStart)
    $decls = @()
    foreach ($d in ($raw -split ';')) { $d = ($d -replace '\s*!important\s*$','').Trim(); if ($d -ne '') { $decls += (Norm-Decl $d) } }
    if ($decls.Count -eq 0) { $skipped++; continue }
    if (-not ($decls | Where-Object { $_ -match ':' })) { $skipped++; continue }

    # --- locate enclosing tag start ---
    # scan backwards for the '<' that opens the real tag (skip '<?' PHP openers)
    $lt = -1
    $probe = $sa.Start
    while ($probe -gt 0) {
      $probe = $text.LastIndexOf('<', $probe - 1)
      if ($probe -lt 0) { break }
      $nxt = if ($probe + 1 -lt $text.Length) { $text[$probe+1] } else { ' ' }
      if ($nxt -eq '?' -or $nxt -eq '!') { continue }   # <?php, <?=, <!--
      $lt = $probe; break
    }
    if ($lt -lt 0) { $skipped++; continue }
    $prefix = $text.Substring($lt, $sa.Start - $lt)
    # the prefix must be a single HTML tag: starts with <tagname and contains no
    # other '<' outside of a PHP block
    if ($prefix -notmatch '^<[a-zA-Z][a-zA-Z0-9-]*') { $skipped++; continue }
    $prefixNoPhp = [regex]::Replace($prefix, '<\?.*?\?>', ' ')
    # no further '<' may appear after the tag name (that would mean we landed inside markup)
    if ($prefixNoPhp.Length -gt 1 -and $prefixNoPhp.Substring(1) -match '<') { $skipped++; continue }
    $phpOpen = ([regex]::Matches($prefix, '<\?')).Count
    $phpClose = ([regex]::Matches($prefix, '\?>')).Count
    if ($phpOpen -gt $phpClose) { $skipped++; continue }

    # --- locate the class / id attributes with a quote-aware scanner ---
    $clsAttr = Find-Attr $prefix 'class'
    $idAttr  = Find-Attr $prefix 'id'
    $existingClasses = @()
    $classQuote = '"'
    $hasDynamicClass = $false
    if ($clsAttr) {
      $classQuote = $clsAttr.Quote
      $existingClasses = @($clsAttr.Value -split '\s+' | Where-Object { $_ -ne '' })
      if ($existingClasses | Where-Object { $_ -match '[<>=?]' }) { $hasDynamicClass = $true }
    }
    $component = @($existingClasses | Where-Object { $_ -notin $declToClass.Keys -and $_ -notmatch '[<>=?]' })
    $tagId = if ($idAttr) { $idAttr.Value } else { '' }

    # quote to use for an injected class attribute:
    #  - inside a PHP string -> must differ from the PHP string's quote
    #  - otherwise -> match the original style attribute's quote
    $phpQuote = Get-PhpQuoteAt $phpSpans $sa.Start
    if ($phpQuote) {
      $injectQuote = if ($phpQuote -eq '"') { "'" } else { '"' }
    } else {
      $injectQuote = $sa.Quote
    }
    if ($tagClassMatch.Success) { $injectQuote = $classQuote }

    $classes = @()
    $needsScoped = ($component.Count -gt 0 -or $tagId -ne '')
    foreach ($d in $decls) {
      if ($declToClass.ContainsKey($d)) {
        $classes += ($declToClass[$d] | Select-Object -First 1)
      } else {
        $nm = New-ClassName $d
        $declToClass[$d] = @($nm)
        $atomic[$nm] = $d
        $classes += $nm
        if ($needsScoped) {
          $scope = if ($tagId -ne '') { "#$tagId" } else { "." + $component[0] }
          $sel = "$scope.$nm"
        } else { $sel = ".$nm" }
        if (-not $newRuleKeys.ContainsKey($sel)) {
          $newRuleKeys[$sel] = $true
          $newRules.Add("$sel { $d !important; }")
        }
      }
    }
    $classes = @($classes | Sort-Object -Unique)

    [void]$out.Append($text.Substring($pos, $lt - $pos))

    $classList = ($classes -join ' ')
    if ($clsAttr) {
      if ($hasDynamicClass) {
        # keep the original class attribute verbatim and append our utilities
        $newPrefix = $prefix.Remove($clsAttr.Start, $clsAttr.Length).Insert($clsAttr.Start, 'class=' + $classQuote + $clsAttr.Value + ' ' + $classList + $classQuote)
      } else {
        $merged = @(($existingClasses + $classes) | Sort-Object -Unique)
        $newPrefix = $prefix.Remove($clsAttr.Start, $clsAttr.Length).Insert($clsAttr.Start, 'class=' + $classQuote + ($merged -join ' ') + $classQuote)
      }
    } else {
      $newPrefix = $prefix -replace '^<([a-zA-Z0-9]+)', ('<$1 class=' + $injectQuote + $classList + $injectQuote)
    }
    [void]$out.Append($newPrefix)
    $pos = $sa.AttrEnd
    $count++
  }
  [void]$out.Append($text.Substring($pos))

  $newText = $out.ToString()
  if ($newText -eq $orig) { continue }

  # --- validate with php -l, roll back on failure ---
  $ok = $true
  if ($full -match '\.php$') {
    $tmp = [System.IO.Path]::GetTempFileName() + '.php'
    [System.IO.File]::WriteAllText($tmp, $newText, $utf8NoBom)
    $lint = (& php -l $tmp 2>&1) -join ''
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    if ($lint -notmatch 'No syntax errors') { $ok = $false; Write-Output ("ROLLBACK (php -l failed): {0}`n    {1}" -f $full.Replace("$webRoot\",''), $lint.Trim()) }
  }
  if (-not $ok) { continue }

  if (-not $DryRun) { [System.IO.File]::WriteAllText($full, $newText, $utf8NoBom) }
  Write-Output ("{0,-45} converted={1}" -f $full.Replace("$webRoot\",''), $count)
  $total += $count
}

if ($newRules.Count -gt 0) {
  Write-Output "`n--- New CSS rules generated: $($newRules.Count) ---"
  if (-not $DryRun) {
    $block = "`r`n/* --- AUTO-GENERATED UTILITIES (inline style migration) ---`r`n" + ($newRules -join "`r`n") + "`r`n"
    [System.IO.File]::AppendAllText($cssPath, $block, $utf8NoBom)
    Write-Output "Appended to $cssPath"
  }
}
Write-Output "TOTAL converted: $total   (skipped: $skipped)"
