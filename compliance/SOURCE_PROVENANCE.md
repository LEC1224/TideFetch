# Source provenance for TideFetch 0.1.x

## Android application

The TideFetch release tag is the authoritative application source. The Gradle
wrapper, build scripts, ProGuard rules, manifest, resources, tests, and Kotlin
sources are included in that tree. Local signing secrets and keystores are
deliberately excluded; they are not required to build or modify the program.

## youtubedl-android payload

- Maven coordinates: `io.github.junkfood02.youtubedl-android:library:0.18.1`
  and `io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1`
- Upstream tag: `0.18.1`
- Tag commit: `d725d5c9a18c3a99a13ee0308bf78275dc310760`
- License declared by the artifacts: GPL-3.0

The AAR was inspected rather than inferred from its filename. Its relevant
payload is:

| Component | Inspected version/evidence |
|---|---|
| Active yt-dlp override | `2026.07.04`, release Git head `fdec00e0bf530dc6c3cc7b1dd780e95d9ae460e9`, app resource SHA-256 `495be29ff4d9d4e9be7eabdfef225221e5d5282e77f2f505abc6dca80349f3fd` |
| Wrapper yt-dlp resource | `2025.11.12`, release Git head `335653be82d5ef999cfc2879d005397402eebec1` (packaged but replaced before wrapper initialization) |
| Python | 3.12.11 |
| QuickJS | 2025-04-26 |
| Mutagen | 1.47.0 |
| PyCryptodome | 3.23.0 (`Cryptodome` namespace) |
| FFmpeg | 7.1.1, GPLv3-or-later configuration |
| Android native target | NDK r28c, API 24; arm64-v8a, armeabi-v7a, x86_64 in TideFetch |

The FFmpeg binary reports `--enable-gpl --enable-version3` and enables external
libraries including x264, x265, AOM, dav1d, LAME, Opus, rav1e, SRT, SVT-AV1,
Theora, vpx, WebP, Xvid, and many supporting libraries. The complete build
recipe and patches are preserved in the pinned Termux snapshot at commit
`de5f604e60ba4a9d0d989c8d56e0c307fe6e3a0c`. Its FFmpeg recipe is version
7.1.1 revision 6 and its configure flags match the binary inspection.

The Python recipe is preserved at the same Termux snapshot and resolves Python
3.12.11. The separately locked archives preserve the application-level Python
packages that were manually inserted into the embedded Python ZIP.

One mirror exception is recorded in the native cache: the Termux liblzo recipe
names a Fossies-recompressed `lzo-2.10.tar.xz` that now rejects automated
downloads. The cache contains the publisher's original `lzo-2.10.tar.gz`
instead, SHA-256
`c0f892943208266f9b6543b3ae308fab6284c5c90e627931446fb49b4221a072`.
It contains the same upstream LZO 2.10 source release.

The ncurses recipe also incorporates foot terminal definitions. Codeberg now
regenerates the `foot` 1.23.1 tag archive with SHA-256
`b3fa774983abb5f95aecca4557d146091d8666bc65fb310d4d3e38327357ade9`
instead of the recipe's historical archive hash. The cache retains that current
tag snapshot alongside the four other ncurses source archives that still match
their recipe hashes; the pinned Termux recipe preserves the original expected
hash.

## Upstream assembly history

The native payload entered youtubedl-android through these upstream changes:

- FFmpeg bump: `b40e6784187ab9ea3bdbbdbb7d028e77ea45f5d0`
- Python bump: `8908be4439b01ed560369457bcca76ba22a6a354`
- QuickJS addition: `4e2bb8b4dbe15d13f57cf37c72e155b9f359d3fb`
- yt-dlp 2025.11.12 wrapper bump: `0611bd9bdb55093ebbeeedc03bc7325e3fb1f94f`

TideFetch adds the audited 2026.07.04 zipapp at the application layer rather
than mutating or remotely updating the published wrapper AAR. On initialization
the app verifies the bundled SHA-256 and atomically installs it into the
wrapper's private runtime path before `YoutubeDL.init` runs. No runtime network
update is performed.

The upstream build notes use the Termux package builder with:

```text
TERMUX_PREFIX=/data/youtubedl-android/usr
TERMUX_ANDROID_HOME=/data/youtubedl-android/home
TERMUX_ARCH=aarch64 | arm | x86_64 | i686
```

They then extract the generated Debian packages and ZIP the required `usr/lib`
tree. `NATIVE_REBUILD.md` records the reconstructed procedure and its limits.

## Verification policy

`sources.lock.json` is the machine-readable authority for fixed downloads.
`make-source-package.ps1` refuses to package a fixed archive whose SHA-256 does
not match. The generated package also contains SHA-256 manifests for its own
contents plus release metadata identifying the exact Git commit and release APK
SHA-256.
