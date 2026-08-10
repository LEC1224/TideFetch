# Rebuilding the Android native toolchain

This is a reconstruction of the native build inputs used by
youtubedl-android 0.18.1. It is suitable for producing a modifiable replacement
payload; it is not a promise that the result is byte-identical to the historical
Maven AAR.

## 1. Prepare the pinned Termux tree

Extract `termux-packages-de5f604e.tar.gz` and use commit
`de5f604e60ba4a9d0d989c8d56e0c307fe6e3a0c`. Install the prerequisites listed
by that repository. Its container scripts acquire Android NDK r28c.

For each architecture (`aarch64`, `arm`, `x86_64`, `i686`), set:

```bash
export TERMUX_ARCH=aarch64
export TERMUX_PREFIX=/data/youtubedl-android/usr
export TERMUX_ANDROID_HOME=/data/youtubedl-android/home
./build-package.sh ffmpeg
./build-package.sh python
./build-package.sh quickjs
```

TideFetch publishes only arm64-v8a, armeabi-v7a, and x86_64. The i686 command is
listed solely because the upstream 0.18.1 AAR also contained legacy x86.

## 2. Assemble FFmpeg

Extract the non-development Debian packages produced for FFmpeg and its runtime
dependencies into one staging prefix, preserving symbolic links. Package the
staged `usr/lib` tree as `libffmpeg.zip.so`. Package the matching FFmpeg and
FFprobe executables as `libffmpeg.so` and `libffprobe.so` using the wrapper's
expected names.

Before accepting a rebuild, run `ffmpeg -version` and confirm that its reported
configuration and linked-library inventory match the intended source recipes.

## 3. Assemble Python and Python packages

Extract Python 3.12.11 and its runtime dependency packages into the same style
of staging prefix. Package the Python executable as `libpython.so` and its
runtime tree as `libpython.zip.so`. Add the pure Python Mutagen 1.47.0 package
and build PyCryptodome 3.23.0 for each target ABI against that Python runtime.

## 4. Assemble QuickJS and yt-dlp

Build QuickJS 2025-04-26 with the same Termux prefix and ABI settings and package
the executable as `libqjs.so`. TideFetch installs the checksum-pinned yt-dlp
2026.07.04 zipapp whose `yt_dlp/version.py` identifies release Git head
`fdec00e0bf530dc6c3cc7b1dd780e95d9ae460e9`. The unmodified wrapper AAR also
contains its older 2025.11.12 resource; preserve both source snapshots when
distributing the historical AAR.

## 5. Build replacement AARs

Put the rebuilt files under the corresponding ABI directories in the
youtubedl-android 0.18.1 source tree, build the `library` and `ffmpeg` modules,
publish them to a controlled Maven repository, and change TideFetch to use those
artifacts. Record SHA-256 hashes and the exact build-container digest.

Do not claim bit reproducibility until a clean build has been completed for all
published ABIs and compared against a second clean build.
