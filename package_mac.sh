#!/bin/bash
# Mr Speed PRO - Local macOS DMG & PKG Packager
echo "=== Building Mr Speed PRO macOS Installers ==="

# 1. Run Gradle to build app distributable on Mac
./gradlew app:createDistributable

# Locate built app bundle
APP_DIR=$(find app/build/compose/binaries/main/app -maxdepth 1 -mindepth 1 -type d | head -n 1)
if [ ! -d "$APP_DIR" ]; then
  echo "Error: App build directory not found."
  exit 1
fi

ACTUAL_APP=$(find "$APP_DIR" -name "*.app" -maxdepth 2 | head -n 1)
if [ -z "$ACTUAL_APP" ]; then
  echo "Error: MrSpeed.app bundle not found."
  exit 1
fi

echo "Found App Bundle: $ACTUAL_APP"

OUTPUT_DIR="app/build/compose/binaries/main/macos-output"
mkdir -p "$OUTPUT_DIR"

echo "Creating macOS Disk Image (.dmg)..."
hdiutil create -volname "Mr Speed PRO" -srcfolder "$ACTUAL_APP" -ov -format UDZO "$OUTPUT_DIR/MrSpeed-1.2.3.dmg"

echo "Creating macOS Installer Package (.pkg)..."
pkgbuild --root "$ACTUAL_APP" --identifier "com.mr.speed.test" --version "1.2.3" --install-location "/Applications/MrSpeed.app" "$OUTPUT_DIR/MrSpeed-1.2.3.pkg"

echo "✅ Success! macOS installers generated in $OUTPUT_DIR:"
ls -la "$OUTPUT_DIR"
