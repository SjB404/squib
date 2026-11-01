# squib

Android desktop-pet app: a pixel squid drawn over other apps. Plain Java, no libraries, no AndroidX.
Package `com.squib.buddy`, minSdk 26, targetSdk 34.

## Layout

- `squib.json`: every voice line, plus settings and app lists. The app downloads it from GitHub, so line changes don't need a new build.
- `app/src/main/java/com/squib/buddy/`
  - `SquibView.java`: drawing, movement, faces, touch, speech bubbles, chatter timing. Originally recovered from a decompiled build, so many local variables have generic names (`d2`, `f3`).
  - `Fun.java`: activities (kite, fishing, magic...), quick fidgets, Minecraft gear, seasonal hats, scroll check-ins. Activity ids, names, where they can happen and how likely they are are tables at the top.
  - `Art.java`: all prop pixel art as rows of characters, and the colour for each character.
  - `Lines.java`: GENERATED from `squib.json` by `tools/gen_lines.py`. Don't edit by hand.
  - `Pick.java`: picks lines without repeats (shuffled deck per group, remembers recent lines across restarts).
  - `Remote.java`: downloads and applies `squib.json`.
  - `SquibService.java`, `MainActivity.java`, `Apps.java`, `PixelFont.java`, `BootReceiver.java`, `Prefs.java`.
- `signing/squib-debug.keystore`: the key every build has been signed with (password `android`, alias `androiddebugkey`). Always sign with it, or the new APK won't install over the phone's copy.
- `apk/Squib.apk`: the current release, committed so the user can download it from GitHub.
- `tools/build-apk.sh`: builds `apk/Squib.apk` without Gradle or the Android SDK (for cloud sessions where Google's servers are blocked).
- `tools/sim/`: runs squib on a computer with stand-in Android classes. `tools/sim/run.sh sheets [NAMES]` renders pictures of activities to `build/sim/`; `tools/sim/run.sh stress 8` simulates 8 hours of random use and fails on any crash.

## Building

- With the Android SDK: `./gradlew assembleDebug` (output in `app/build/outputs/apk/debug/`). Copy the result to `apk/Squib.apk` for a release.
- Without it: `tools/build-apk.sh`.

## Rules

- Lines go in `squib.json`, then run `python3 tools/gen_lines.py`. Plain keyboard characters only: the pixel font can't show emoji, accents or curly quotes. `{mins}` only works in SCROLL_LONG.
- A new line group needs a matching name in code (an activity name in `Fun.NAMES`, or a `Lines.get("NAME")` / `maybeSay(v, "NAME", ...)` call). Unknown groups in the JSON are ignored, which keeps old app versions working with a newer file.
- No Java lambdas or method references: `tools/build-apk.sh` uses dex2jar, which can't handle them. Use anonymous classes. Keep Java 8 syntax.
- Don't turn on code shrinking (`minifyEnabled`): `Lines`, `Remote` and `Fun.rows()` look up fields by name.
- Don't add an accessibility service, or SMS / notification-listener permissions. Play Protect blocks sideloaded apps that ask for them. (An earlier keyboard-dodge feature was removed for this reason.)
- Minecraft gear is diamond only, never netherite.
- Draw original pixel art only. No characters or logos from games, films or brands (so no creepers, for example).
- For a release: bump `versionCode` and `versionName` in `app/build.gradle`, rebuild, replace `apk/Squib.apk`, update the version and any changed behaviour in `README.md`.
- Before shipping new animations, render them with `tools/sim/run.sh sheets NAME` and look at the pictures, and run `tools/sim/run.sh stress 8`.
