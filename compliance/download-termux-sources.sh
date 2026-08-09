#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE="$ROOT/compliance/cache"
TERMUX_ARCHIVE="$CACHE/termux-packages-de5f604e.tar.gz"
OUT="$CACHE/termux-native-sources"
WORK="${TMPDIR:-/tmp}/tidefetch-termux-de5f604e"

for command in bash curl git jq python3 sha256sum tar; do
  command -v "$command" >/dev/null || { echo "Missing required command: $command" >&2; exit 1; }
done

test -f "$TERMUX_ARCHIVE" || {
  echo "Missing $TERMUX_ARCHIVE. Run compliance/make-source-package.ps1 once to fetch locked archives." >&2
  exit 1
}

mkdir -p "$WORK" "$OUT"
tar -xzf "$TERMUX_ARCHIVE" -C "$WORK" --strip-components=1
cd "$WORK"

mapfile -t PACKAGE_PATHS < <(
  {
    python3 scripts/buildorder.py packages/ffmpeg packages
    python3 scripts/buildorder.py packages/python packages
    printf '%s\n' packages/ffmpeg packages/python packages/quickjs
  } | awk '{print $NF}' | sort -u
)

export TERMUX_SCRIPTDIR="$WORK"
. "$WORK/scripts/properties.sh"
: "${TERMUX_PKG_MAKE_PROCESSES:=$(nproc)}"
export TERMUX_PKG_MAKE_PROCESSES TERMUX_ARCH=aarch64 TERMUX_ON_DEVICE_BUILD=false
export TERMUX_PACKAGES_OFFLINE=true TERMUX_HOST_PLATFORM=aarch64-linux-android
export TERMUX_ARCH_BITS=64 TERMUX_BUILD_TUPLE=x86_64-pc-linux-gnu TERMUX_PKG_API_LEVEL=24
export TERMUX_TOPDIR="${HOME}/.termux-build" TERMUX_PYTHON_CROSSENV_PREFIX="${HOME}/.termux-build/python-crossenv-prefix"
export TERMUX_PYTHON_VERSION=3.12
export TERMUX_PYTHON_HOME="$TERMUX_PREFIX/lib/python3.12"
export CC=gcc CXX=g++ LD=ld AR=ar STRIP=strip PKG_CONFIG=pkg-config
export CPPFLAGS="" CFLAGS="" CXXFLAGS="" LDFLAGS="" TERMUX_PACKAGE_LIBRARY=bionic
export TERMUX_QUIET_BUILD=false TERMUX_CONTINUE_BUILD=false

. "$WORK/scripts/build/get_source/termux_step_get_source.sh"
. "$WORK/scripts/build/get_source/termux_git_clone_src.sh"
. "$WORK/scripts/build/get_source/termux_download_src_archive.sh"
. "$WORK/scripts/build/get_source/termux_unpack_src_archive.sh"
. "$WORK/scripts/build/termux_download.sh"
termux_extract_src_archive() { :; }

for package_path in "${PACKAGE_PATHS[@]}"; do
  package_name="$(basename "$package_path")"
  # libc++ is copied from the Android NDK sysroot by its recipe and has no
  # separately declared source archive. NDK r28c is a System Library/toolchain
  # input and is recorded in SOURCE_PROVENANCE.md instead.
  if [ "$package_name" = "libc++" ]; then
    continue
  fi
  # Fossies now rejects automated access to the recipe's recompressed .tar.xz.
  # The publisher's original .tar.gz is cached separately with SHA-256
  # c0f892943208266f9b6543b3ae308fab6284c5c90e627931446fb49b4221a072.
  if [ "$package_name" = "liblzo" ] && [ -f "$OUT/liblzo/lzo-2.10.tar.gz" ]; then
    continue
  fi
  # Codeberg regenerates tag archives, so the bytes for foot 1.23.1 no longer
  # match the 2025 recipe despite representing the same tag. The current tag
  # snapshot and its new hash are recorded in SOURCE_PROVENANCE.md.
  if [ "$package_name" = "ncurses" ] && [ -f "$OUT/ncurses/foot-1.23.1-codeberg-current.tar.gz" ]; then
    continue
  fi
  # psmisc is a host/package-manager utility in the recursive build order, not
  # object code conveyed in TideFetch's FFmpeg/Python payload. Its Fossies URL
  # now rejects automation, so it is intentionally outside this binary's
  # corresponding-source set.
  if [ "$package_name" = "psmisc" ] || [ "$package_name" = "texinfo" ]; then
    continue
  fi
  package_out="$OUT/$package_name"
  mkdir -p "$package_out"
  (
    TERMUX_PKG_NAME="$package_name"
    TERMUX_PKG_BUILDER_DIR="$WORK/$package_path"
    TERMUX_PKG_CACHEDIR="$package_out"
    TERMUX_PKG_METAPACKAGE=false
    TERMUX_PKG_TMPDIR="${TMPDIR:-/tmp}/tidefetch-source-download/$package_name/tmp"
    TERMUX_PKG_SRCDIR="${TMPDIR:-/tmp}/tidefetch-source-download/$package_name/src"
    TERMUX_PKG_BUILDDIR="$TERMUX_PKG_SRCDIR"
    TERMUX_PKG_HOSTBUILD_DIR="$TERMUX_PKG_TMPDIR"
    TERMUX_PKG_GIT_BRANCH=""
    TERMUX_DEBUG_BUILD=false
    mkdir -p "$TERMUX_PKG_TMPDIR" "$TERMUX_PKG_SRCDIR"
    cd "$TERMUX_PKG_CACHEDIR"
    # Recipes occasionally assume variables normally supplied later by the
    # full builder. Source acquisition does not need those hooks.
    set +e
    . "$TERMUX_PKG_BUILDER_DIR/build.sh"
    recipe_status=$?
    set -e
    if [ "$recipe_status" -ne 0 ]; then
      echo "Recipe setup for $TERMUX_PKG_NAME returned $recipe_status; continuing with any declared source metadata" >&2
    fi
    if ! "$TERMUX_PKG_METAPACKAGE"; then
      echo "Downloading source for $TERMUX_PKG_NAME"
      termux_step_get_source
    fi
    rm -rf "$TERMUX_PKG_TMPDIR" "$TERMUX_PKG_SRCDIR"
  )
done

(cd "$OUT" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS)
echo "Termux native source cache completed at $OUT"
