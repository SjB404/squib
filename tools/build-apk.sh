#!/usr/bin/env bash
# Builds apk/Squib.apk WITHOUT Gradle or the Android SDK. Only needs Java 17+, python3, curl, unzip.
# Use this where Google's download servers are blocked (e.g. Claude cloud sessions).
# With the Android SDK installed, use Gradle instead:  ./gradlew assembleDebug
#
# Note: dex2jar (used here) can't handle Java lambdas or method references, so the app code
# uses anonymous classes. Keep it that way so both builds keep working.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${SQUIB_TOOLS:-$HOME/.cache/squib-tools}"
mkdir -p "$CACHE"

get() { # url file
  [ -s "$CACHE/$2" ] || curl -sSfL -o "$CACHE/$2" "$1"
}
get https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar android.jar
get https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar apktool.jar
get https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar uber-apk-signer.jar
if [ ! -d "$CACHE/dex-tools-v2.4" ]; then
  get https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip dex2jar.zip
  unzip -q -o "$CACHE/dex2jar.zip" -d "$CACHE"
fi

cd "$ROOT"
python3 tools/gen_lines.py

CODE=$(grep -oP 'versionCode\s+\K[0-9]+' app/build.gradle)
NAME=$(grep -oP 'versionName\s+"\K[^"]+' app/build.gradle)
echo "building squib $NAME ($CODE)"

W="$(mktemp -d)"
trap 'rm -rf "$W"' EXIT

# 1. compile. Resource ids are pinned by public.xml below, so R can be written by hand.
mkdir -p "$W/classes"
cat > "$W/R.java" <<'EOF'
package com.squib.buddy;
public final class R {
    public static final class drawable { public static final int ic_notif = 0x7f010000; }
    public static final class mipmap { public static final int ic_launcher = 0x7f020000; }
}
EOF
javac --release 8 -encoding UTF-8 -nowarn -Xlint:-options -cp "$CACHE/android.jar" -d "$W/classes" \
  app/src/main/java/com/squib/buddy/*.java "$W/R.java"
(cd "$W/classes" && jar cf ../app.jar .)

# 2. java bytecode -> dex
sh "$CACHE/dex-tools-v2.4/d2j-jar2dex.sh" -f -o "$W/classes.dex" "$W/app.jar" >/dev/null 2>&1
mkdir -p "$W/apk"

# 3. resources + manifest, packaged by apktool
cp -r app/src/main/res "$W/apk/res"
mkdir -p "$W/apk/res/values"
cat > "$W/apk/res/values/public.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <public type="drawable" name="ic_notif" id="0x7f010000" />
    <public type="mipmap" name="ic_launcher" id="0x7f020000" />
</resources>
EOF
sed 's#<manifest xmlns:android="http://schemas.android.com/apk/res/android">#<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.squib.buddy">#' \
  app/src/main/AndroidManifest.xml > "$W/apk/AndroidManifest.xml"
cat > "$W/apk/apktool.yml" <<EOF
version: 2.10.0
apkFileName: squib.apk
isFrameworkApk: false
usesFramework:
  ids:
  - 1
  tag: null
sdkInfo:
  minSdkVersion: 26
  targetSdkVersion: 34
packageInfo:
  forcedPackageId: 127
  renameManifestPackage: null
versionInfo:
  versionCode: $CODE
  versionName: $NAME
resourcesAreCompressed: false
sharedLibrary: false
sparseResources: false
doNotCompress:
- resources.arsc
- png
EOF
java -jar "$CACHE/apktool.jar" b "$W/apk" -o "$W/unsigned.apk" > "$W/apktool.log" 2>&1 || { cat "$W/apktool.log"; exit 1; }
[ -s "$W/unsigned.apk" ] || { cat "$W/apktool.log"; echo "packaging failed"; exit 1; }
(cd "$W" && zip -q unsigned.apk classes.dex)

# 4. sign with the same key as every earlier build, so it installs over the phone's copy
java -jar "$CACHE/uber-apk-signer.jar" -a "$W/unsigned.apk" -o "$W/signed" \
  --ks signing/squib-debug.keystore --ksAlias androiddebugkey --ksPass android --ksKeyPass android > "$W/sign.log" 2>&1 \
  || { cat "$W/sign.log"; exit 1; }
ls "$W"/signed/*.apk > /dev/null 2>&1 || { cat "$W/sign.log"; echo "signing failed"; exit 1; }
mkdir -p apk
cp "$W"/signed/*.apk apk/Squib.apk
echo "wrote apk/Squib.apk ($(du -k apk/Squib.apk | cut -f1) KB)"
