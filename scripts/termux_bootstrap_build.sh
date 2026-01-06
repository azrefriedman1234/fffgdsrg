#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

echo "== PasiflonetMobile: bootstrap + build =="

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

mkdir -p app/libs

TD_URL="https://jitpack.io/com/github/tdlibx/td/1.8.56/td-1.8.56.aar"
TD_DST="app/libs/td-1.8.56.aar"

echo "[1/3] Download TDLib AAR..."
if [ ! -f "$TD_DST" ]; then
  curl -L --fail "$TD_URL" -o "$TD_DST"
else
  echo "TDLib AAR already exists: $TD_DST"
fi

echo "[2/3] Ensure gradlew is executable..."
chmod +x ./gradlew

echo "[3/3] Build Debug APK..."
./gradlew :app:assembleDebug --no-daemon
echo "✅ Done. APK path: app/build/outputs/apk/debug/app-debug.apk"
