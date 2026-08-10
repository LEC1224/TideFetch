# TideFetch corresponding-source kit

This directory records and packages the source materials associated with a
specific TideFetch binary release. It is designed to make the source available
next to the APK and to preserve the scripts and configuration needed to build,
modify, and install the covered software.

It is not a declaration that an arbitrary APK is compliant. Run the audit for
every release and publish the resulting archive beside the exact binary it
describes.

## What the release source archive contains

- the exact TideFetch Git tree for the release;
- the youtubedl-android 0.18.1 source snapshot;
- yt-dlp 2026.07.04 source for TideFetch's active, checksum-pinned runtime and
  yt-dlp 2025.11.12 source for the dormant resource inside the wrapper AAR;
- FFmpeg 7.1.1 source;
- Python 3.12.11 source;
- QuickJS 2025-04-26 source;
- Mutagen 1.47.0 and PyCryptodome 3.23.0 source;
- the pinned Termux package build system, patches, and native dependency source
  caches used to describe the Android builds;
- source manifests, SHA-256 checksums, license texts, notices, and build notes.

## Creating the package

Prerequisites on Windows are Git, PowerShell 7, WSL2 with Ubuntu, `curl`,
`git`, `jq`, and enough free space for the complete native dependency set.

```powershell
wsl.exe bash compliance/download-termux-sources.sh
./gradlew.bat exportReleaseDependencySources
pwsh -File compliance/make-source-package.ps1 -GitRef HEAD -Version 0.1.0-alpha.3 `
  -ApkPath app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

The first command downloads the source archive or Git checkout declared by
each relevant Termux recipe in the FFmpeg/Python build dependency closure. The
Gradle task exports source artifacts for the resolved Android/Kotlin dependency
graph. The final command verifies every fixed source snapshot, exports the
application source, adds both source caches, and creates a ZIP and SHA-256 file
under `dist/`. The generated release metadata binds the source package to the
exact Git commit and signed APK SHA-256.

The generated ZIP must be retained for as long as the corresponding binary is
offered. Put it on the same GitHub release as the APK and link it prominently
from every other binary download location.

## Important historical limitation

TideFetch 0.1.x uses the published youtubedl-android 0.18.1 AARs. Inspection
shows that their native files were built from FFmpeg 7.1.1, Python 3.12.11, and
QuickJS 2025-04-26 using the Termux toolchain and Android NDK r28c/API 24. The
embedded FFmpeg configure string matches the pinned Termux recipe.

However, the upstream maintainer stated that these historical binaries were
assembled manually on multiple devices/emulators and that the complete exact
assembly workflow was not retained. The source kit therefore supplies matching
editable upstream source, Termux recipes/patches, dependency source, and an
assembly recipe, but it does not claim byte-for-byte reproduction of the
published AAR native payload.

For the strongest compliance posture, do not make a production binary release
from those opaque AARs. Rebuild the native payload from this pinned source set,
verify it, and make the rebuilt artifacts the inputs to the Android application.
Legal compliance ultimately requires qualified review for the distribution
method and jurisdiction.
