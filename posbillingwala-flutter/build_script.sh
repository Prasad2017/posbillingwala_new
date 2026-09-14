#!/usr/bin/env bash
# POS Billingwala — interactive release builder
# Outputs:
#   release/android/  → APK and/or AAB
#   release/web/      → web build (zip + extracted folder)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

RELEASE_DIR="$ROOT_DIR/release"
ANDROID_OUT="$RELEASE_DIR/android"
WEB_OUT="$RELEASE_DIR/web"

APP_NAME="pos_billingwala_v2"
VERSION_NAME="$(grep -E '^version:' pubspec.yaml | head -1 | awk '{print $2}' | cut -d'+' -f1)"
VERSION_CODE="$(grep -E '^version:' pubspec.yaml | head -1 | awk '{print $2}' | cut -d'+' -f2)"
STAMP="$(date +%Y%m%d_%H%M%S)"
ARTIFACT_PREFIX="${APP_NAME}_${VERSION_NAME}+${VERSION_CODE}_${STAMP}"

BUILD_APK=0
BUILD_AAB=0
BUILD_WEB=0

# Colors (safe when not a TTY)
if [[ -t 1 ]]; then
  C_RESET='\033[0m'
  C_BOLD='\033[1m'
  C_GREEN='\033[32m'
  C_YELLOW='\033[33m'
  C_CYAN='\033[36m'
  C_RED='\033[31m'
else
  C_RESET='' C_BOLD='' C_GREEN='' C_YELLOW='' C_CYAN='' C_RED=''
fi

log()  { echo -e "${C_CYAN}→${C_RESET} $*"; }
ok()   { echo -e "${C_GREEN}✓${C_RESET} $*"; }
warn() { echo -e "${C_YELLOW}!${C_RESET} $*"; }
err()  { echo -e "${C_RED}✗${C_RESET} $*" >&2; }

usage() {
  cat <<EOF
${C_BOLD}Usage:${C_RESET} ./build_script.sh [options]

Interactive menu is shown when no options are passed.

Options:
  --apk          Build Android APK
  --aab          Build Android App Bundle (AAB)
  --android      Build both APK and AAB
  --web          Build Web release
  --all          Build APK + AAB + Web
  -h, --help     Show this help

Outputs are copied to:
  release/android/
  release/web/
EOF
}

parse_args() {
  if [[ $# -eq 0 ]]; then
    return 1
  fi
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --apk) BUILD_APK=1 ;;
      --aab) BUILD_AAB=1 ;;
      --android) BUILD_APK=1; BUILD_AAB=1 ;;
      --web) BUILD_WEB=1 ;;
      --all) BUILD_APK=1; BUILD_AAB=1; BUILD_WEB=1 ;;
      -h|--help) usage; exit 0 ;;
      *) err "Unknown option: $1"; usage; exit 1 ;;
    esac
    shift
  done
  return 0
}

interactive_menu() {
  echo ""
  echo -e "${C_BOLD}POS Billingwala — Release Builder${C_RESET}"
  echo "Version: ${VERSION_NAME}+${VERSION_CODE}"
  echo ""
  echo "What do you want to build?"
  echo "  1) Android APK"
  echo "  2) Android AAB (Play Store)"
  echo "  3) Android APK + AAB"
  echo "  4) Web"
  echo "  5) Android APK + Web"
  echo "  6) Android AAB + Web"
  echo "  7) Android APK + AAB + Web (all)"
  echo "  0) Cancel"
  echo ""
  read -r -p "Enter choice [0-7]: " choice

  case "$choice" in
    1) BUILD_APK=1 ;;
    2) BUILD_AAB=1 ;;
    3) BUILD_APK=1; BUILD_AAB=1 ;;
    4) BUILD_WEB=1 ;;
    5) BUILD_APK=1; BUILD_WEB=1 ;;
    6) BUILD_AAB=1; BUILD_WEB=1 ;;
    7) BUILD_APK=1; BUILD_AAB=1; BUILD_WEB=1 ;;
    0) echo "Cancelled."; exit 0 ;;
    *) err "Invalid choice."; exit 1 ;;
  esac
}

need_flutter() {
  if ! command -v flutter >/dev/null 2>&1; then
    err "flutter not found on PATH. Install Flutter or open a terminal where flutter works."
    exit 1
  fi
}

prepare_dirs() {
  mkdir -p "$ANDROID_OUT" "$WEB_OUT"
}

build_apk() {
  log "Building Android APK (release)..."
  flutter build apk --release

  local src
  src="$(find "$ROOT_DIR/build/app/outputs/flutter-apk" -name '*.apk' -type f | head -1)"
  if [[ -z "${src:-}" ]]; then
    # Fallback for split/legacy layouts
    src="$(find "$ROOT_DIR/build/app/outputs/apk" -name '*.apk' -type f | head -1 || true)"
  fi
  if [[ -z "${src:-}" || ! -f "$src" ]]; then
    err "APK not found under build/app/outputs/"
    exit 1
  fi

  local dest="$ANDROID_OUT/${ARTIFACT_PREFIX}.apk"
  cp -f "$src" "$dest"
  ok "APK → release/android/$(basename "$dest")"
}

build_aab() {
  log "Building Android App Bundle (release)..."
  flutter build appbundle --release

  local src
  src="$(find "$ROOT_DIR/build/app/outputs/bundle" -name '*.aab' -type f | head -1)"
  if [[ -z "${src:-}" || ! -f "$src" ]]; then
    err "AAB not found under build/app/outputs/bundle/"
    exit 1
  fi

  local dest="$ANDROID_OUT/${ARTIFACT_PREFIX}.aab"
  cp -f "$src" "$dest"
  ok "AAB → release/android/$(basename "$dest")"
}

build_web() {
  log "Building Web (release)..."
  flutter build web --release

  local web_src="$ROOT_DIR/build/web"
  if [[ ! -d "$web_src" ]]; then
    err "Web build folder missing: build/web"
    exit 1
  fi

  local web_folder="$WEB_OUT/${ARTIFACT_PREFIX}"
  local web_zip="$WEB_OUT/${ARTIFACT_PREFIX}.zip"

  rm -rf "$web_folder"
  mkdir -p "$web_folder"
  # Prefer rsync if available; otherwise cp -R
  if command -v rsync >/dev/null 2>&1; then
    rsync -a --delete "$web_src"/ "$web_folder"/
  else
    cp -R "$web_src"/. "$web_folder"/
  fi

  # Zip for easy deploy/upload
  (
    cd "$WEB_OUT"
    if command -v zip >/dev/null 2>&1; then
      rm -f "$(basename "$web_zip")"
      zip -qr "$(basename "$web_zip")" "$(basename "$web_folder")"
    else
      warn "zip not found — skipped web zip; folder is still available."
    fi
  )

  ok "Web folder → release/web/$(basename "$web_folder")/"
  if [[ -f "$web_zip" ]]; then
    ok "Web zip    → release/web/$(basename "$web_zip")"
  fi
}

print_summary() {
  echo ""
  echo -e "${C_BOLD}Done.${C_RESET} Artifacts:"
  if [[ $BUILD_APK -eq 1 || $BUILD_AAB -eq 1 ]]; then
    echo "  Android: $ANDROID_OUT"
    ls -1 "$ANDROID_OUT"/${ARTIFACT_PREFIX}.* 2>/dev/null || true
  fi
  if [[ $BUILD_WEB -eq 1 ]]; then
    echo "  Web:     $WEB_OUT"
    ls -1d "$WEB_OUT"/${ARTIFACT_PREFIX}* 2>/dev/null || true
  fi
  echo ""
  if [[ ! -f "$ROOT_DIR/android/key.properties" ]] && [[ $BUILD_APK -eq 1 || $BUILD_AAB -eq 1 ]]; then
    warn "android/key.properties not found — release may be signed with debug keys."
    warn "Copy android/key.properties.example → android/key.properties for Play Store signing."
  fi
}

# --- main ---
need_flutter

if ! parse_args "$@"; then
  interactive_menu
fi

if [[ $BUILD_APK -eq 0 && $BUILD_AAB -eq 0 && $BUILD_WEB -eq 0 ]]; then
  err "Nothing selected to build."
  exit 1
fi

echo ""
log "Project: $ROOT_DIR"
log "Version: ${VERSION_NAME}+${VERSION_CODE}"
prepare_dirs

log "flutter pub get..."
flutter pub get

[[ $BUILD_APK -eq 1 ]] && build_apk
[[ $BUILD_AAB -eq 1 ]] && build_aab
[[ $BUILD_WEB -eq 1 ]] && build_web

print_summary
