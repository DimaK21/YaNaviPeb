#!/usr/bin/env bash
# Builds build/navdump.apk without Gradle (aapt2 + javac + d8 + apksigner).
set -euo pipefail
cd "$(dirname "$0")"

SDK="$HOME/Library/Android/sdk"
BT="$SDK/build-tools/34.0.0"
AJ="$SDK/platforms/android-34/android.jar"

rm -rf build && mkdir -p build/classes build/dex

[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore \
  -storepass android -keypass android -alias debug -keyalg RSA -keysize 2048 \
  -validity 10000 -dname "CN=navspike" >/dev/null 2>&1

"$BT/aapt2" link -o build/app-unsigned.apk -I "$AJ" --manifest AndroidManifest.xml \
  --min-sdk-version 26 --target-sdk-version 30
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -Xlint:-options \
  -d build/classes src/dev/navspike/*.java
"$BT/d8" --min-api 26 --lib "$AJ" --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../app-unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/app-unsigned.apk build/app-aligned.apk
"$BT/apksigner" sign --ks debug.keystore --ks-pass pass:android \
  --out build/navdump.apk build/app-aligned.apk

echo "OK: $(pwd)/build/navdump.apk"
