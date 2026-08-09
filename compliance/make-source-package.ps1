param(
    [string]$GitRef = "HEAD",
    [Parameter(Mandatory = $true)]
    [string]$Version,
    [Parameter(Mandatory = $true)]
    [string]$ApkPath
)

$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$cache = Join-Path $repo "compliance\cache"
$dist = Join-Path $repo "dist"
$workRoot = Join-Path $repo "compliance\work"
$stage = Join-Path $workRoot "package-$Version"
$lock = Get-Content (Join-Path $PSScriptRoot "sources.lock.json") -Raw | ConvertFrom-Json

if (-not (Test-Path -LiteralPath $ApkPath)) { throw "APK not found: $ApkPath" }
$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$resolvedCommit = (& git -C $repo rev-parse "$GitRef^{commit}").Trim()
if ($LASTEXITCODE -ne 0 -or $resolvedCommit -notmatch '^[0-9a-f]{40}$') {
    throw "Could not resolve Git commit: $GitRef"
}

New-Item -ItemType Directory -Force -Path $cache, $dist, $workRoot | Out-Null
if (Test-Path -LiteralPath $stage) {
    $resolvedWorkRoot = (Resolve-Path -LiteralPath $workRoot).Path
    $resolvedStage = (Resolve-Path -LiteralPath $stage).Path
    if ([IO.Path]::GetDirectoryName($resolvedStage) -ne $resolvedWorkRoot) {
        throw "Refusing to clean unexpected staging path: $resolvedStage"
    }
    Remove-Item -LiteralPath $resolvedStage -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stage | Out-Null

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

$apkHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedApk).Hash.ToLowerInvariant()
@(
    "version=$Version"
    "gitCommit=$resolvedCommit"
    "binaryFile=$(Split-Path $resolvedApk -Leaf)"
    "binarySha256=$apkHash"
) | Set-Content -LiteralPath (Join-Path $packageRoot "RELEASE-METADATA.txt") -Encoding ascii

$hashLines = Get-ChildItem -LiteralPath $packageRoot -Recurse -File | Sort-Object FullName | ForEach-Object {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash.ToLowerInvariant()
    $relative = [IO.Path]::GetRelativePath($packageRoot, $_.FullName).Replace('\', '/')
    "$hash  $relative"
}
$manifestText = ($hashLines -join "`n") + "`n"
[IO.File]::WriteAllText(
    (Join-Path $packageRoot "SHA256SUMS"),
    $manifestText,
    [Text.UTF8Encoding]::new($false)
)

$output = Join-Path $dist "TideFetch-$Version-corresponding-source.zip"
Compress-Archive -LiteralPath $packageRoot -DestinationPath $output -CompressionLevel Optimal -Force
$outputHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $output).Hash.ToLowerInvariant()
"$outputHash  $(Split-Path $output -Leaf)" | Set-Content -LiteralPath "$output.sha256" -Encoding ascii
Write-Output $output
Write-Output "SHA256: $outputHash"
