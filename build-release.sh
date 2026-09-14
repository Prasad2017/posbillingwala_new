#!/usr/bin/env bash
# Build signed release APK and/or AAB into releases/<App>/app-name-YYYY-MM-DD-HHMM.*
# If PosBillingwala.jks is missing, builds debug APK instead (no AAB).
#
# Usage (interactive):  ./build-release.sh
# Usage (args):         ./build-release.sh <app> <apk|aab|both>
#   app: pos | flutter | owner | dealer | admin | all
#
# Keystore: PosBillingwala.jks (repo root)
# Alias:    posbillingwala
# Password: PosBillingwala  (override with STORE_PASSWORD / KEY_PASSWORD env)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
KEYSTORE="${ROOT}/PosBillingwala.jks"
# Relative to each native app/ module (WithTable/app, Owner/app, ...)
STORE_REL="../../PosBillingwala.jks"
STORE_PASSWORD="${STORE_PASSWORD:-PosBillingwala}"
KEY_PASSWORD="${KEY_PASSWORD:-PosBillingwala}"
KEY_ALIAS="${KEY_ALIAS:-posbillingwala}"

if [[ -f "$KEYSTORE" ]]; then
  HAS_KEYSTORE=1
  BUILD_MODE="release"
  SIGN_PROPS=(
    "-PRELEASE_STORE_FILE=${STORE_REL}"
    "-PRELEASE_STORE_PASSWORD=${STORE_PASSWORD}"
    "-PRELEASE_KEY_ALIAS=${KEY_ALIAS}"
    "-PRELEASE_KEY_PASSWORD=${KEY_PASSWORD}"
  )
else
  HAS_KEYSTORE=0
  BUILD_MODE="debug"
  SIGN_PROPS=()
fi

resolve_app() {
  case "$(echo "$1" | tr '[:upper:]' '[:lower:]')" in
    pos|withtable|billingwala|1)
      echo "POS|WithTable|POS-Billingwala"
      ;;
    flutter|v2|pos_v2|flutter_pos|6)
      echo "POS-Flutter|pos_billingwala_v2|POS-Billingwala-Flutter"
      ;;
    owner|2)
      echo "Owner|Owner|Owner"
      ;;
    dealer|3)
      echo "Dealer|Dealer|Dealer"
      ;;
    admin|4)
      echo "Admin|Admin|Admin"
      ;;
    *)
      return 1
      ;;
  esac
}

prompt_app() {
  echo ""
  echo "Select app:"
  echo "  1) POS (WithTable native)"
  echo "  2) Owner"
  echo "  3) Dealer"
  echo "  4) Admin"
  echo "  5) All (native apps)"
  echo "  6) POS Flutter v2"
  read -r -p "App [1-6]: " choice
  case "$choice" in
    1) echo "pos" ;;
    2) echo "owner" ;;
    3) echo "dealer" ;;
    4) echo "admin" ;;
    5) echo "all" ;;
    6) echo "flutter" ;;
    *)
      echo "Invalid app choice: $choice" >&2
      exit 1
      ;;
  esac
}

prompt_format() {
  echo ""
  echo "Select output:"
  echo "  1) apk"
  echo "  2) aab"
  echo "  3) both"
  read -r -p "Format [1-3]: " choice
  case "$choice" in
    1|apk) echo "apk" ;;
    2|aab) echo "aab" ;;
    3|both) echo "both" ;;
    *)
      echo "Invalid format choice: $choice" >&2
      exit 1
      ;;
  esac
}

read_version() {
  local gradle_file="$1"
  local version_name version_code
  version_name=$(grep -E "versionName" "$gradle_file" | head -1 | sed -E 's/.*versionName[[:space:]]+"([^"]+)".*/\1/')
  version_code=$(grep -E "versionCode" "$gradle_file" | head -1 | sed -E 's/.*versionCode[[:space:]]+([0-9]+).*/\1/')
  echo "${version_name}|${version_code}"
}

read_flutter_version() {
  local pubspec="$1"
  local version_line version_name version_code
  version_line=$(grep -E "^version:" "$pubspec" | head -1 | sed -E 's/^version:[[:space:]]*//')
  version_name="${version_line%%+*}"
  version_code="${version_line##*+}"
  echo "${version_name}|${version_code}"
}

ensure_flutter_key_properties() {
  local android_dir="$1"
  local props="${android_dir}/key.properties"
  cat > "$props" <<EOF
storePassword=${STORE_PASSWORD}
keyPassword=${KEY_PASSWORD}
keyAlias=${KEY_ALIAS}
storeFile=../../PosBillingwala.jks
EOF
}

build_flutter() {
  local format="$1"
  local module_path="${ROOT}/pos_billingwala_v2"
  local android_dir="${module_path}/android"
  local stamp out_dir version_name version_code
  local want_apk=0 want_aab=0 suffix=""
  local outputs=()

  stamp="$(date +%Y-%m-%d-%H%M)"
  out_dir="${ROOT}/releases/POS-Flutter"
  mkdir -p "$out_dir"

  shopt -s nullglob
  old_builds=("${out_dir}"/*.apk "${out_dir}"/*.aab "${out_dir}"/*.idsig)
  if [[ ${#old_builds[@]} -gt 0 ]]; then
    echo "Removing ${#old_builds[@]} old build(s) from releases/POS-Flutter/"
    rm -f "${old_builds[@]}"
  fi
  shopt -u nullglob

  IFS='|' read -r version_name version_code <<<"$(read_flutter_version "${module_path}/pubspec.yaml")"

  case "$format" in
    apk)  want_apk=1 ;;
    aab)  want_aab=1 ;;
    both) want_apk=1; want_aab=1 ;;
    *)
      echo "Unknown format: $format (use apk|aab|both)" >&2
      exit 1
      ;;
  esac

  if [[ "$BUILD_MODE" == "debug" ]]; then
    if [[ "$want_aab" -eq 1 ]]; then
      echo "WARNING: AAB needs a signed release; keystore missing - skipping AAB, building debug APK only."
    fi
    want_apk=1
    want_aab=0
    suffix="-debug"
  else
    ensure_flutter_key_properties "$android_dir"
  fi

  local base_name="POS-Billingwala-Flutter-${version_name}-v${version_code}-${stamp}${suffix}"

  echo ""
  echo "========================================"
  echo " Building POS Flutter v2 (${BUILD_MODE} / ${format})"
  echo " Module:  pos_billingwala_v2"
  echo " Output:  releases/POS-Flutter/${base_name}.*"
  echo "========================================"

  pushd "$module_path" >/dev/null
  flutter pub get

  if [[ "$BUILD_MODE" == "debug" ]]; then
    flutter build apk --debug
  else
    if [[ "$want_apk" -eq 1 ]]; then
      flutter build apk --release
    fi
    if [[ "$want_aab" -eq 1 ]]; then
      flutter build appbundle --release
    fi
  fi
  popd >/dev/null

  if [[ "$want_apk" -eq 1 ]]; then
    local apk_src
    if [[ "$BUILD_MODE" == "debug" ]]; then
      apk_src="$(find "${module_path}/build/app/outputs/flutter-apk" -name "app-debug.apk" 2>/dev/null | head -1)"
    else
      apk_src="$(find "${module_path}/build/app/outputs/flutter-apk" -name "app-release.apk" 2>/dev/null | head -1)"
    fi
    if [[ -z "$apk_src" ]]; then
      echo "APK not found after Flutter build" >&2
      exit 1
    fi
    local apk_dest="${out_dir}/${base_name}.apk"
    cp -f "$apk_src" "$apk_dest"
    outputs+=("$apk_dest")
  fi

  if [[ "$want_aab" -eq 1 ]]; then
    local aab_src
    aab_src="$(find "${module_path}/build/app/outputs/bundle/release" -name "*.aab" 2>/dev/null | head -1)"
    if [[ -z "$aab_src" ]]; then
      echo "AAB not found after Flutter build" >&2
      exit 1
    fi
    local aab_dest="${out_dir}/${base_name}.aab"
    cp -f "$aab_src" "$aab_dest"
    outputs+=("$aab_dest")
  fi

  echo ""
  echo "Saved:"
  for f in "${outputs[@]}"; do
    echo "  $f"
  done
}

build_one() {
  local app_key="$1"
  local format="$2"
  local mapped release_folder module_dir display_name stamp out_dir gradle_file version_name version_code
  local tasks=() outputs=()
  local want_apk=0 want_aab=0 suffix=""

  local app_lower
  app_lower="$(echo "$app_key" | tr '[:upper:]' '[:lower:]')"
  if [[ "$app_lower" == "flutter" || "$app_lower" == "v2" || "$app_lower" == "pos_v2" || "$app_lower" == "flutter_pos" ]]; then
    build_flutter "$format"
    return
  fi

  mapped="$(resolve_app "$app_key")" || {
    echo "Unknown app: $app_key (use pos|flutter|owner|dealer|admin|all)" >&2
    exit 1
  }

  IFS='|' read -r release_folder module_dir display_name <<<"$mapped"
  module_path="${ROOT}/${module_dir}"
  gradle_file="${module_path}/app/build.gradle"
  stamp="$(date +%Y-%m-%d-%H%M)"
  out_dir="${ROOT}/releases/${release_folder}"
  mkdir -p "$out_dir"

  shopt -s nullglob
  old_builds=("${out_dir}"/*.apk "${out_dir}"/*.aab "${out_dir}"/*.idsig)
  if [[ ${#old_builds[@]} -gt 0 ]]; then
    echo "Removing ${#old_builds[@]} old build(s) from releases/${release_folder}/"
    rm -f "${old_builds[@]}"
  fi
  shopt -u nullglob

  IFS='|' read -r version_name version_code <<<"$(read_version "$gradle_file")"

  case "$format" in
    apk)  want_apk=1 ;;
    aab)  want_aab=1 ;;
    both) want_apk=1; want_aab=1 ;;
    *)
      echo "Unknown format: $format (use apk|aab|both)" >&2
      exit 1
      ;;
  esac

  if [[ "$BUILD_MODE" == "debug" ]]; then
    if [[ "$want_aab" -eq 1 ]]; then
      echo "WARNING: AAB needs a signed release; keystore missing - skipping AAB, building debug APK only."
    fi
    want_apk=1
    want_aab=0
    tasks=("assembleDebug")
    suffix="-debug"
  else
    [[ "$want_apk" -eq 1 ]] && tasks+=("assembleRelease")
    [[ "$want_aab" -eq 1 ]] && tasks+=("bundleRelease")
  fi

  base_name="${display_name}-${version_name}-v${version_code}-${stamp}${suffix}"

  echo ""
  echo "========================================"
  echo " Building ${display_name} (${BUILD_MODE} / ${format})"
  echo " Module:  ${module_dir}"
  echo " Output:  releases/${release_folder}/${base_name}.*"
  echo "========================================"

  pushd "$module_path" >/dev/null

  if [[ -x "./gradlew" ]]; then
    if [[ ${#SIGN_PROPS[@]} -gt 0 ]]; then
      ./gradlew --no-daemon "${tasks[@]}" "${SIGN_PROPS[@]}"
    else
      ./gradlew --no-daemon "${tasks[@]}"
    fi
  elif [[ -f "./gradlew.bat" ]]; then
    if [[ ${#SIGN_PROPS[@]} -gt 0 ]]; then
      cmd.exe //c "gradlew.bat --no-daemon ${tasks[*]} ${SIGN_PROPS[*]}"
    else
      cmd.exe //c "gradlew.bat --no-daemon ${tasks[*]}"
    fi
  else
    echo "gradlew not found in ${module_path}" >&2
    exit 1
  fi

  popd >/dev/null

  if [[ "$want_apk" -eq 1 ]]; then
    local apk_src
    apk_src="$(find "${module_path}/app/build/outputs/apk/${BUILD_MODE}" -name "*.apk" ! -name "*.apk.idsig" 2>/dev/null | head -1)"
    if [[ -z "$apk_src" ]]; then
      echo "APK not found after build" >&2
      exit 1
    fi
    local apk_dest="${out_dir}/${base_name}.apk"
    cp -f "$apk_src" "$apk_dest"
    outputs+=("$apk_dest")
  fi

  if [[ "$want_aab" -eq 1 ]]; then
    local aab_src
    aab_src="$(find "${module_path}/app/build/outputs/bundle/release" -name "*.aab" | head -1)"
    if [[ -z "$aab_src" ]]; then
      echo "AAB not found after build" >&2
      exit 1
    fi
    local aab_dest="${out_dir}/${base_name}.aab"
    cp -f "$aab_src" "$aab_dest"
    outputs+=("$aab_dest")
  fi

  echo ""
  echo "Saved:"
  for f in "${outputs[@]}"; do
    echo "  $f"
  done
}

# ---- main ----
if [[ "$HAS_KEYSTORE" -eq 0 ]]; then
  echo ""
  echo "Keystore not found: $KEYSTORE"
  echo "Falling back to DEBUG APK (unsigned / debug-signed)."
else
  echo ""
  echo "Keystore found - building signed RELEASE."
fi

APP_ARG="${1:-}"
FORMAT_ARG="${2:-}"

if [[ -z "$APP_ARG" ]]; then
  APP_ARG="$(prompt_app)"
fi
if [[ -z "$FORMAT_ARG" ]]; then
  FORMAT_ARG="$(prompt_format)"
fi

FORMAT_ARG="$(echo "$FORMAT_ARG" | tr '[:upper:]' '[:lower:]')"
case "$FORMAT_ARG" in
  apk|aab|both) ;;
  *)
    echo "Format must be apk, aab, or both" >&2
    exit 1
    ;;
esac

APP_LOWER="$(echo "$APP_ARG" | tr '[:upper:]' '[:lower:]')"
if [[ "$APP_LOWER" == "all" ]]; then
  for a in pos owner dealer admin; do
    build_one "$a" "$FORMAT_ARG"
  done
else
  build_one "$APP_LOWER" "$FORMAT_ARG"
fi

echo ""
echo "Done. Mode=${BUILD_MODE}"
