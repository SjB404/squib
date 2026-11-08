# squib

A pixel squid that lives on your phone and your computer. He walks around, fidgets, does activities, and reacts to whatever app you have open.

- **Phone app:** [apk/Squib.apk](apk/Squib.apk) (1.20.0). Installs over earlier versions.
- **Windows app:** [desktop/Squib.exe](desktop/Squib.exe) (1.20.0). Run it from anywhere, nothing to install.
- **His lines:** `squib.json`. Both apps download it from here, so edits show up without a new app.
- **Everything else** (all modes, activities, settings, line groups, Windows details): [docs/REFERENCE.md](docs/REFERENCE.md).

## Building

- **Phone:** `./gradlew assembleDebug` with the Android SDK, or `tools/build-apk.sh` without it (needs Java 17+, python3, curl, unzip). Both sign with `signing/squib-debug.keystore`, so new builds install over the old one.
- **Windows:** `desktop\build.cmd`. Uses the C# compiler that comes with Windows.

Both apps run the same code: the phone app's Java, turned into C# when the Windows app is built.
