param(
    [string]$GitRef = "HEAD",
    [Parameter(Mandatory = $true)]
    [string]$Version
)

$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$cache = Join-Path $repo "compliance\cache"
$dist = Join-Path $repo "dist"
$stage = Join-Path $repo "compliance\work\package-$Version"
$lock = Get-Content (Join-Path $PSScriptRoot "sources.lock.json") -Raw | ConvertFrom-Json

New-Item -ItemType Directory -Force -Path $cache, $dist, $stage | Out-Null

foreach ($source in $lock.sources) {
    $cacheName = if ($source.localCacheFile) { $source.localCacheFile } else { $source.file }
    $path = Join-Path $cache $cacheName
    if (-not (Test-Path -LiteralPath $path)) {
        & curl.exe -fL --retry 3 --output $path $source.url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $($source.url)" }
    }
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant()
    if ($actual -ne $source.sha256.ToLowerInvariant()) {
        throw "SHA-256 mismatch for $cacheName. Expected $($source.sha256), got $actual"
    }
}

$nativeCache = Join-Path $cache "termux-native-sources"
if (-not (Test-Path -LiteralPath (Join-Path $nativeCache "SHA256SUMS"))) {
    throw "Termux dependency source cache is missing. Run: wsl.exe bash compliance/download-termux-sources.sh"
}
$gradleSources = Join-Path $cache "gradle-sources"
if (-not (Test-Path -LiteralPath (Join-Path $gradleSources "ARTIFACTS.tsv"))) {
    throw "Gradle source artifacts are missing. Run: gradlew exportReleaseDependencySources"
}

$packageRoot = Join-Path $stage "TideFetch-$Version-corresponding-source"
$appSource = Join-Path $packageRoot "tidefetch"
$upstream = Join-Path $packageRoot "upstream-sources"
New-Item -ItemType Directory -Force -Path $appSource, $upstream | Out-Null

$archive = Join-Path $stage "tidefetch.tar"
& git -C $repo archive --format=tar --output=$archive $GitRef
if ($LASTEXITCODE -ne 0) { throw "git archive failed for $GitRef" }
& tar -xf $archive -C $appSource
if ($LASTEXITCODE -ne 0) { throw "Could not extract application source" }

foreach ($source in $lock.sources) {
    $cacheName = if ($source.localCacheFile) { $source.localCacheFile } else { $source.file }
    Copy-Item -LiteralPath (Join-Path $cache $cacheName) -Destination (Join-Path $upstream $source.file)
}
Copy-Item -LiteralPath $nativeCache -Destination (Join-Path $upstream "termux-native-sources") -Recurse
Copy-Item -LiteralPath $gradleSources -Destination (Join-Path $upstream "gradle-dependency-sources") -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "SOURCE_PROVENANCE.md") -Destination $packageRoot
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "NATIVE_REBUILD.md") -Destination $packageRoot
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "sources.lock.json") -Destination $packageRoot

$hashLines = Get-ChildItem -LiteralPath $packageRoot -Recurse -File | Sort-Object FullName | ForEach-Object {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash.ToLowerInvariant()
    $relative = [IO.Path]::GetRelativePath($packageRoot, $_.FullName).Replace('\', '/')
    "$hash  $relative"
}
$hashLines | Set-Content -LiteralPath (Join-Path $packageRoot "SHA256SUMS") -Encoding utf8NoBOM

$output = Join-Path $dist "TideFetch-$Version-corresponding-source.zip"
Compress-Archive -LiteralPath $packageRoot -DestinationPath $output -CompressionLevel Optimal -Force
$outputHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $output).Hash.ToLowerInvariant()
"$outputHash  $(Split-Path $output -Leaf)" | Set-Content -LiteralPath "$output.sha256" -Encoding ascii
Write-Output $output
Write-Output "SHA256: $outputHash"
