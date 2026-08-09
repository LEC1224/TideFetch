# Changelog

All notable TideFetch changes are documented here.

## Unreleased

- No changes yet.

## 0.1.0-alpha.2 — 2026-08-09

- Added opt-in Gradle release signing through an ignored local
  `keystore.properties` file and created the permanent TideFetch signing key.
- Added a locked corresponding-source workflow for the GPL Android downloader,
  FFmpeg, codec, Python, and JavaScript runtime stack.
- Recorded inspected native versions, build configuration, source hashes,
  upstream provenance, known historical reproducibility limitations, and native
  rebuild instructions.
- Corrected the embedded Python version in documentation from 3.8 to 3.12.11.
- Published signed ABI-specific APKs with the version-matched source package.

## 0.1.0-alpha.1 — 2026-08-09

Initial public source preview:

- Material 3 interface with system, light, and dark blue themes.
- Clipboard and Android share-sheet URL intake.
- MP4, WebM, M4A, MP3, and WAV output choices with resolution presets.
- User-initiated foreground downloads powered by embedded yt-dlp and FFmpeg.
- MediaStore publication to the camera roll or music library.
- Progress, cancellation, friendly errors, and sanitized technical logs.
- Adaptive launcher icon and accessibility-focused layouts.

This preview does not include a release APK. Native-binary distribution remains
blocked on permanent signing and completion of the corresponding-source and
third-party license package.
