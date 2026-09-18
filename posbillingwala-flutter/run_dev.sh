#!/usr/bin/env bash
# POS Billingwala — interactive Flutter run
# 1) flutter clean  2) flutter pub get  3) choose Chrome or Android

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ -t 1 ]]; then
  C_RESET='\033[0m'
  C_BOLD='\033[1m'
  C_GREEN='\033[32m'
  C_CYAN='\033[36m'
  C_RED='\033[31m'
else
  C_RESET='' C_BOLD='' C_GREEN='' C_CYAN='' C_RED=''
fi

log()  { echo -e "${C_CYAN}→${C_RESET} $*"; }
ok()   { echo -e "${C_GREEN}✓${C_RESET} $*"; }
err()  { echo -e "${C_RED}✗${C_RESET} $*" >&2; }

if ! command -v flutter >/dev/null 2>&1; then
  err "flutter not found on PATH. Install Flutter or open a terminal where flutter works."
  exit 1
fi

echo ""
echo -e "${C_BOLD}POS Billingwala — Dev Run${C_RESET}"
echo "Project: $ROOT_DIR"
echo ""

log "flutter clean..."
flutter clean
ok "Clean done"

echo ""
log "flutter pub get..."
flutter pub get
ok "Dependencies ready"

echo ""
echo "Where do you want to run?"
echo "  1) Chrome (web)"
echo "  2) Android (device / emulator)"
echo "  0) Cancel"
echo ""
read -r -p "Enter choice [0-2]: " choice

case "$choice" in
  1)
    echo ""
    log "Starting on Chrome..."
    exec flutter run -d chrome
    ;;
  2)
    echo ""
    log "Starting on Android..."
    exec flutter run -d android
    ;;
  0)
    echo "Cancelled."
    exit 0
    ;;
  *)
    err "Invalid choice."
    exit 1
    ;;
esac
