$files = Get-ChildItem -Path . -Include *.java,*.yml -Recurse -File
foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    if ($content -match 'Â¡|Â¿|Ã¡|Ã©|Ã­|Ã³|Ãº|Ã±|Ã‘|Ã§|Ã£|Ã¢|Ãª|Ãµ|Ã“|Ãš|Ã¼|Ã¶|Ã¤') {
        $content = $content.Replace('Â¡', '¡').Replace('Â¿', '¿').Replace('Ã¡', 'á').Replace('Ã©', 'é').Replace('Ã­', 'í').Replace('Ã³', 'ó').Replace('Ãº', 'ú').Replace('Ã±', 'ñ').Replace('Ã‘', 'Ñ').Replace('Ã§', 'ç').Replace('Ã£', 'ã').Replace('Ã¢', 'â').Replace('Ãª', 'ê').Replace('Ãµ', 'õ').Replace('Ã“', 'Ó').Replace('Ãš', 'Ú').Replace('Ã¼', 'ü').Replace('Ã¶', 'ö').Replace('Ã¤', 'ä')
        $utf8NoBom = New-Object System.Text.UTF8Encoding($False)
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
        Write-Host "Fixed $($f.FullName)"
    }
}
