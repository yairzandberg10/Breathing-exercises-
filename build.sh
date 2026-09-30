#!/usr/bin/env bash
# Builds a signed APK with the plain Android build tools (no Gradle / Android Studio needed).
# Requires: JDK 17+, aapt2, dx (dalvik-exchange), zipalign, apksigner, platform android-23 jar.
# On Ubuntu/Debian: apt install android-sdk-build-tools android-sdk-platform-23 apksigner zipalign
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-}"

ANDROID_JAR="${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}"
BT="${BUILD_TOOLS:-/usr/lib/android-sdk/build-tools/debian}"
DX="$BT/dx"
OUT=build
APP=app

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/res"

echo "▶ compiling resources"
aapt2 compile --dir "$APP/res" -o "$OUT/res/res.zip"
aapt2 link -I "$ANDROID_JAR" --manifest "$APP/AndroidManifest.xml" -A "$APP/assets" \
  --java "$OUT/gen" -o "$OUT/unsigned.apk" "$OUT/res/res.zip" --min-sdk-version 23 --target-sdk-version 30 \
  --version-code 1 --version-name 1.0

echo "▶ compiling java"
javac -nowarn -Xlint:-options --release 8 -encoding UTF-8 -cp "$ANDROID_JAR" -d "$OUT/classes" \
  $(find "$APP/src" "$OUT/gen" -name '*.java')

echo "▶ dexing"
"$DX" --dex --min-sdk-version=23 --output="$OUT/classes.dex" "$OUT/classes"
(cd "$OUT" && zip -q -j unsigned.apk classes.dex)

echo "▶ aligning + signing"
zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
KS="${KEYSTORE:-tools/debug.keystore}"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias iron \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Iron Breath, O=Yair" >/dev/null
fi
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$OUT/IronBreath.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/IronBreath.apk"
ls -la "$OUT/IronBreath.apk"
