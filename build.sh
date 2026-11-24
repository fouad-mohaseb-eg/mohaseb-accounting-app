#!/bin/bash
# Manual build pipeline for محاسبة محمد عبدالله (no Gradle/Android Studio needed).
# Needs: JDK 17+, Android SDK platform android-34 and build-tools 34.0.0.
# Set SDK to your sdk path, then: ./build.sh
set -e
SDK=${SDK:-/opt/android-sdk}
BT=$SDK/build-tools/34.0.0
AJ=$SDK/platforms/android-34/android.jar
cd "$(dirname "$0")"
mkdir -p build/classes build/dex
mkdir -p build/gen
$BT/aapt2 compile --dir src/res -o build/res.zip
$BT/aapt2 link -I $AJ --manifest src/AndroidManifest.xml --min-sdk-version 24 --target-sdk-version 34 -o build/app-unsigned.apk --java build/gen build/res.zip
javac -encoding UTF-8 -classpath $AJ -d build/classes src/com/fouad/acc/*.java build/gen/com/fouad/acc/R.java
$BT/d8 --lib $AJ --min-api 24 --output build/dex build/classes/com/fouad/acc/*.class
(cd build/dex && zip -q -r ../app-unsigned.apk classes.dex)
$BT/zipalign -f 4 build/app-unsigned.apk build/app-aligned.apk
# sign with your keystore:
# keytool -genkeypair -keystore keystore.jks -alias app -keyalg RSA -keysize 2048 -validity 10000
$BT/apksigner sign --ks keystore.jks build/app-aligned.apk
$BT/apksigner verify build/app-aligned.apk && echo OK
