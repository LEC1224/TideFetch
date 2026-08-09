# TideFetch

**Bring every clip ashore.**

TideFetch is a polished, local-first Android front end for `yt-dlp`. Paste a supported media URL, choose an output, and save the result to Android's shared media library without having to use a terminal.

> TideFetch is intended only for media you own or are permitted to download. It does not bypass DRM and is not affiliated with YouTube, Meta, X, or any other media platform.

## Project status

TideFetch is an **alpha preview**. The project builds, its JVM tests and Android lint checks pass, and the application has received initial device testing. Alpha binaries are signed and published together with their corresponding-source package; they are not yet production recommendations because the historical native AAR assembly was not reproducible byte-for-byte.

## Highlights

- Android 10+ (`minSdk 29`) interface built with Kotlin, Jetpack Compose, and Material 3.
- Detects an `http://` or `https://` URL already on the clipboard when the app opens and places it in the URL field.
- Works with sites supported by the bundled `yt-dlp` extractor, including many YouTube, Facebook, and X/Twitter links.
- Resolution presets plus an **Original** option. The default video choice prefers the original resolution in an MP4 container with H.264-compatible video when the source and device toolchain allow it.
- H.264-preferred and source-codec MP4, source-quality WebM, and M4A/MP3/WAV audio-only output choices.
- A clear progress state and an expandable, copyable activity log for diagnosing extractor, network, merge, and conversion errors.
- Light and dark blue themes, with **System default** selected initially and a saved user override.
- Publishes completed video and audio through `MediaStore`, so files appear in gallery/media apps under a TideFetch collection rather than remaining in private app storage.

## Build

Requirements:

- JDK 17
- Android SDK Platform 35 installed, with accepted SDK licenses
- Internet access on the first build so Gradle can resolve dependencies

From the repository root:

```sh
./gradlew assembleDebug
./gradlew test
./gradlew bundleRelease
```

On Windows PowerShell, use the corresponding `./gradlew.bat` commands. The debug build creates smaller ABI-specific APKs plus a convenient universal APK below `app/build/outputs/apk/debug/`; most current phones use `arm64-v8a`, while the `x86_64` build is intended for emulators. The release app bundle is written below `app/build/outputs/bundle/release/`. Prefer the bundle for Play-style distribution because the embedded Python and FFmpeg payload make a universal APK unusually large. Open the project in a recent Android Studio release if you prefer IDE builds.

The first on-device use initializes the Python/`yt-dlp` payload. Downloads require network access and can consume substantial storage and data, especially when separate high-resolution video and audio streams must be merged.

Release APKs are signed when an ignored `keystore.properties` file is present at
the repository root with the following fields:

```properties
storeFile=/absolute/path/to/tidefetch-release.jks
storePassword=your-store-password
keyAlias=tidefetch-release
keyPassword=your-key-password
```

Without that local file, release builds remain unsigned so public CI and
contributors can still build the project. Never commit a keystore, password, or
`keystore.properties`. Back up the permanent release key and recovery
credentials securely: losing the key prevents compatible updates to APKs signed
with it. Retain the matching R8 mapping from
`app/build/outputs/mapping/release/` for every published version.

## How it works

TideFetch follows a small, single-activity architecture:

1. The Compose UI collects the URL, theme, resolution, and container/audio choice.
2. A screen-level view model owns immutable UI state, validates input, and coordinates one active job.
3. A foreground download service keeps active work visible to Android, while the download layer translates each preset into conservative `yt-dlp` arguments and streams progress and diagnostic output back to the UI.
4. `yt-dlp` and FFmpeg write into app-controlled temporary storage. After success, the media publisher copies the finished item into Android `MediaStore` and then removes the temporary working files.
5. Theme preference is persisted locally and clipboard access is limited to startup autofill and explicit paste actions.

This split keeps platform storage, downloader execution, and presentation concerns separate and testable. Android scoped storage is used; TideFetch does not need broad file-system access to add its own downloads to the media library.

## Output behavior and limitations

- **Original** means the best source streams selected by the current extractor, subject to the chosen container. A platform may not offer every resolution or codec.
- MP4/H.264 is a compatibility preference, not a guarantee that every source has a native H.264 stream. Merging compatible streams is inexpensive; converting an incompatible codec requires FFmpeg transcoding, which is slower, battery-intensive, and may fail on memory- or storage-constrained devices. TideFetch does not invent quality that is absent from the source.
- MP3 and WAV are audio extractions. WAV files are uncompressed and can be very large.
- Some sites require an authenticated browser session, cookies, a subscription, a region, or an age-verified account. TideFetch does not currently expose cookie/login import.
- DRM-protected streams are unsupported. Site layout and API changes can temporarily break extractors until `yt-dlp` is updated.
- The Android wrapper (`0.18.1`) embeds yt-dlp `2025.11.12`, Python 3.12.11, QuickJS 2025-04-26, FFmpeg 7.1.1, Mutagen 1.47.0, and PyCryptodome 3.23.0. Newer desktop yt-dlp releases are not automatically substituted because their Python requirements and unsigned runtime-update path need an Android-specific audit. Verify target sites before shipping; update the wrapper/Python payload as one reviewed toolchain when a compatible release is available.
- Android may show a system clipboard-access notification. Clipboard autofill intentionally accepts only web URLs.
- A completed file may take a moment to appear in a particular gallery app while that app refreshes its media index.

Always respect copyright, the source site's terms, local law, and the media owner's permissions. Availability through an extractor does not itself grant permission to download or redistribute a work.

## Updating the downloader toolchain

TideFetch pins `youtubedl-android` and its FFmpeg artifact to `0.18.1` for reproducible builds. To update:

1. Change all youtubedl-android module versions together in the Gradle dependency declaration or version catalog.
2. Read the upstream release notes and verify the supported Android ABIs, minimum SDK, packaged `yt-dlp` version, and FFmpeg configuration.
3. Rebuild from a clean checkout, run `./gradlew test assembleDebug`, and exercise one direct download, one split-stream merge, MP3 extraction, WAV extraction, cancellation, and `MediaStore` publication on Android 10 and a current Android version.
4. Refresh `THIRD_PARTY_NOTICES.md` and ship the exact license texts and corresponding source/offers required by the binaries in the APK.

Do not update only one of the library/FFmpeg artifacts: mismatched native payloads can initialize successfully but fail during post-processing. For a distributed release, audit the resolved dependency graph and the actual native binaries rather than relying solely on artifact names.

## Third-party software

The downloader stack includes GPL-covered software and has source-distribution obligations. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [SOURCE_OFFER.md](SOURCE_OFFER.md), and the [corresponding-source kit](compliance/README.md) before redistributing an APK. Every binary release must remain accompanied by its version-matched source ZIP. These files are a practical inventory, not legal advice.

See also the [privacy policy](PRIVACY.md), [changelog](CHANGELOG.md), and complete [GPL-3.0 license](LICENSE).
