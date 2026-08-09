# TideFetch third-party notices

TideFetch uses third-party software. Copyright remains with each project's contributors. This summary is provided for developer convenience; it is not legal advice, does not replace the complete license texts, and does not grant a license to TideFetch itself.

Before distributing an APK or app bundle, retain the notices packaged by every dependency, generate an inventory from the resolved release build, and include the complete license text and source-code offer/material required by each applicable license.

## Downloader and media toolchain

### youtubedl-android 0.18.1

- Project: <https://github.com/yausername/youtubedl-android>
- License: GNU General Public License, version 3 (`GPL-3.0`)
- Source: <https://github.com/yausername/youtubedl-android>

The app links to and packages modules from youtubedl-android. Distribution of a combined APK may require the combined work to comply with GPL-3.0, including preservation of notices, provision of the full GPL text, and availability of the complete corresponding source and applicable installation information. A public repository link alone is not necessarily sufficient for every distribution method or modified build. Review GPL-3.0 and obtain qualified advice for a production release.

### yt-dlp

- Project: <https://github.com/yt-dlp/yt-dlp>
- Main project license: The Unlicense
- License: <https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE>
- Bundled-component notices: <https://github.com/yt-dlp/yt-dlp/blob/master/THIRD_PARTY_LICENSES.txt>

The main yt-dlp source is released under the Unlicense, but release payloads can contain components under other licenses, including MIT and ISC terms. Preserve the notices that accompany the exact yt-dlp payload embedded by youtubedl-android.

### FFmpeg

- Project: <https://ffmpeg.org/>
- Source: <https://git.ffmpeg.org/ffmpeg.git>
- Legal and compliance guidance: <https://ffmpeg.org/legal.html>

FFmpeg is primarily licensed under LGPL-2.1-or-later. Enabling optional GPL code changes the resulting FFmpeg build to GPL-2.0-or-later; external libraries can add further terms. In particular, a build that enables GPL components such as `libx264` is GPL-covered. The artifact name alone does not prove which configuration was used.

For release distribution, inspect the exact prebuilt FFmpeg artifact used by version 0.18.1, retain its build configuration and notices, and satisfy the effective LGPL/GPL obligations. Depending on that build and how it is conveyed, this can include relinkability requirements, corresponding source for FFmpeg and enabled libraries, a written/source offer, attribution, and full license texts. Do not describe the binary as merely "portable" or "free to use" without this audit.

## Android application libraries

The app also uses Kotlin, AndroidX, Jetpack Compose, Material components, lifecycle libraries, coroutines, and their transitive dependencies. These are generally distributed under permissive licenses such as Apache License 2.0, but the resolved release dependency report is authoritative. Preserve their copyright notices and license texts as required.

Useful upstream references:

- Android Open Source Project licenses: <https://source.android.com/docs/setup/about/licenses>
- Jetpack/AndroidX source: <https://android.googlesource.com/platform/frameworks/support/>
- Kotlin: <https://github.com/JetBrains/kotlin>
- Kotlin coroutines: <https://github.com/Kotlin/kotlinx.coroutines>

## Release checklist

1. Build the exact release artifact that will be distributed.
2. Record its Gradle dependency graph and inventory packaged native libraries/assets.
3. Extract and review the youtubedl-android, yt-dlp, and FFmpeg notices and versions from that artifact.
4. Include all required license texts and prominent notices in the distribution and in an accessible in-app or accompanying location.
5. Publish or offer the complete corresponding source and build/install materials wherever GPL/LGPL terms require it.
6. Repeat the audit whenever a downloader, FFmpeg, codec, or transitive dependency changes.

Separate from open-source licensing, distributors are responsible for reviewing applicable platform policies, service terms, copyright rules, privacy disclosures, export rules, and local law.
