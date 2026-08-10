# TideFetch third-party notices

TideFetch embeds third-party downloader and media software. Copyright remains
with each project's contributors. The repository's `THIRD_PARTY_NOTICES.md`
contains the extended compliance checklist for distributors.

## youtubedl-android 0.18.1

- Source: https://github.com/yausername/youtubedl-android
- License: GNU General Public License, version 3 (`GPL-3.0`)

The app links to and packages youtubedl-android. Distribution of a combined APK
may require the complete corresponding source, notices, build materials, and the
full GPL-3.0 license to be made available under GPL-compatible terms.

## yt-dlp

- Source: https://github.com/yt-dlp/yt-dlp
- Main project license: The Unlicense
- Bundled component notices:
  https://github.com/yt-dlp/yt-dlp/blob/master/THIRD_PARTY_LICENSES.txt

Release payloads may include components under MIT and ISC terms in addition to
the main project's Unlicense. TideFetch 0.1.0-alpha.3 actively installs the
checksum-pinned yt-dlp 2026.07.04 app resource and also carries the wrapper's
dormant 2025.11.12 resource. The matching source snapshots and bundled notices
for both are included in the version-specific corresponding-source package.

## FFmpeg

- Source: https://git.ffmpeg.org/ffmpeg.git
- Legal and compliance guidance: https://ffmpeg.org/legal.html

The embedded FFmpeg is version 7.1.1 and reports `--enable-gpl`,
`--enable-version3`, `--enable-libx264`, and `--enable-libx265` among its build
options. This conveyed build is GPLv3-or-later. Its matching source, Termux
recipes/patches, enabled-library source, notices, and rebuild documentation are
provided through the version-specific TideFetch source offer packaged with this
application.

## Android application libraries

TideFetch also uses Kotlin, AndroidX, Jetpack Compose, Material components,
DataStore, Lifecycle, and Kotlin coroutines. These are generally available
under permissive licenses such as Apache License 2.0. The exact resolved release
dependency graph and the license metadata retained in the APK are authoritative.

This inventory is not legal advice and does not replace the complete license
texts required for a release.
