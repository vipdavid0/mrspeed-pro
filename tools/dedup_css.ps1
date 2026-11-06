param([string]$CssPath = 'E:\My-Speed-Test-App\web\css\mr-speed-theme.css')
$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$lines = [System.IO.File]::ReadAllLines($CssPath, [System.Text.Encoding]::UTF8)
$seen = @{}
$keep = New-Object System.Collections.Generic.List[string]
$removed = 0
$conflicts = @()

for ($i = 0; $i -lt $lines.Count; $i++) {
  $line = $lines[$i]
  if ($line -match '^\.([a-zA-Z0-9_-]+)\s*\{(.*)\}\s*$') {
    $cls = $matches[1]
    $body = ($matches[2] -replace '\s+',' ').Trim()
    if ($seen.ContainsKey($cls)) {
      $prev = $seen[$cls]
      if ($prev -eq $body) {
        Write-Output ("REMOVED duplicate L{0}: .{1} {{ {2} }}" -f ($i+1), $cls, $body)
        $removed++
        continue
      } else {
        $conflicts += "CONFLICT .{0}  first: [{1}]  now: [{2}] (L{3})" -f $cls, $prev, $body, ($i+1)
      }
    } else {
      $seen[$cls] = $body
    }
  }
  $keep.Add($line)
}

if ($conflicts.Count -gt 0) {
  Write-Output "`n=== CONFLICTS (left in place, need manual review) ==="
  $conflicts | ForEach-Object { Write-Output $_ }
}

[System.IO.File]::WriteAllLines($CssPath, $keep, $utf8NoBom)
Write-Output "`nRemoved $removed duplicate definitions. New line count: $($keep.Count)"
