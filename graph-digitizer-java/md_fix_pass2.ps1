$files = Get-ChildItem -Recurse -Filter '*.md' -File | Where-Object { $_.FullName -notmatch '\\target\\' -and $_.FullName -notmatch '\\.git\\' }
foreach($f in $files){
  $path = $f.FullName
  $text = [System.IO.File]::ReadAllText($path)
  $arr = $text -split '\r?\n'
  $out = New-Object System.Collections.Generic.List[string]
  function peekNextNonEmpty([int]$start){
    for($k=$start; $k -lt $arr.Count; $k++){
      if(($arr[$k].Trim()).Length -gt 0){ return $arr[$k].Trim() }
    }
    return ''
  }
  for($i=0; $i -lt $arr.Count; $i++){
    $line = $arr[$i]
    if($line -match '^(#{1,6}\s.*)'){
      if($out.Count -gt 0 -and ($out[$out.Count-1].Trim()).Length -ne 0){ $out.Add('') }
      $out.Add($line)
      $next = if($i+1 -lt $arr.Count) { $arr[$i+1] } else { '' }
      if($next -and ($next.Trim().Length -gt 0) -and ($next -notmatch '^(#{1,6}\s|```|\s*\|)')){ $out.Add('') }
      continue
    }
    if($line -match '^\s*```\s*$'){
      $next = peekNextNonEmpty ($i+1)
      $lang = 'text'
      if($next -match '^<\?xml|^<project|^<dependencies|^<[^>]+>'){ $lang='xml' }
      elseif($next -match '^\{\s*$|^\{') { $lang='json' }
      elseif($next -match '^(public|class|package|import)\b|\bSystem\.out\.|\bprivate\b|\bvoid\b'){ $lang='java' }
      elseif($next -match '^\$|^expo?r?t?\s|^mvn\b|^git\b|^npm\b|^docker\b|^java\b|^python\b|^node\b|^#!/'){ $lang='bash' }
      elseif($next -match '^<svg|^<path|^<rect'){ $lang='xml' }
      $newline = '```' + $lang
      if($out.Count -gt 0 -and ($out[$out.Count-1].Trim()).Length -ne 0){ $out.Add('') }
      $out.Add($newline)
      continue
    }
    if($line -match '^\s*```'){
      if($out.Count -gt 0 -and ($out[$out.Count-1].Trim()).Length -ne 0){ $out.Add('') }
      $out.Add($line)
      continue
    }
    if($line -match '^\s*\|'){
      $l = $line -replace '\s*\|\s*',' | '
      $l = $l -replace '\s+$',''
      $out.Add($l)
      continue
    }
    if($line -match '^\s*```\s*$'){
      $out.Add($line)
      if($i+1 -lt $arr.Count){ $n=$arr[$i+1]; if($n -and ($n.Trim().Length -ne 0)){ $out.Add('') } }
      continue
    }
    $out.Add($line)
  }
  $new = ($out -join "`n").TrimEnd() + "`n"
    if($new -ne $text){ [System.IO.File]::WriteAllText($path, $new, [System.Text.Encoding]::UTF8); Write-Output "Updated: $path" }
}
