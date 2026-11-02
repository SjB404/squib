# squib

Android desktop-pet app: a pixel squid drawn over other apps. Plain Java, no libraries, no AndroidX.
Package `com.squib.buddy`, minSdk 26, targetSdk 34.

## Layout

- `squib.json`: every voice line, plus settings and app lists. The app downloads it from GitHub, so line changes don't need a new build.
- `app/src/main/java/com/squib/buddy/`
  - `SquibView.java`: drawing, movement, faces, touch, speech bubbles, chatter timing. Originally recovered from a decompiled build, so many local variables have generic names (`d2`, `f3`).
  - `Fun.java`: activities (kite, fishing, magic...), quick fidgets, Minecraft gear, seasonal hats, scroll check-ins. Activity ids, names, where they can happen and how likely they are are tables at the top.
  - `Art.java`: all prop pixel art as rows of characters, and the colour for each character.
  - `Props.java`: props drawn by code so one animation covers many versions: balls (size, 7 patterns, colours, turned for rolling), fidget spinners, coins, and the puzzle cube (keeps every sticker, so solves are real). Colour pairs (`PAIRS`) used to recolour toys.
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
- The user keeps click-through on almost all the time, so taps pass straight through squib. Don't build features that only work by tapping, petting or dragging him; use timers, app modes, the screen turning on, or the buttons in his settings / notification instead.
- Bedtime (midnight to 5am by default, `Fun.bedtimeStart/End`): he sleeps, and wakes now and then to nag (`SquibView.bedtime()` and the late check in `wellbeing()`). New activities must not start during bedtime.
- Minecraft gear is diamond only, never netherite.
- Draw original pixel art only. No characters or logos from games, films or brands (so no creepers, for example).
- For a release: bump `versionCode` and `versionName` in `app/build.gradle`, rebuild, replace `apk/Squib.apk`, update the version and any changed behaviour in `README.md`.
- Before shipping new animations, render them with `tools/sim/run.sh sheets NAME` and look at the pictures, and run `tools/sim/run.sh stress 8`. `SIM_OPTS="-Dreal=1" tools/sim/run.sh stress 12` runs with the real timings and reports the most activities in any hour and fidgets in any 10 minutes.
- Activities are at most 1 an hour (1 to 2 hours apart). Fidgets are frequent (1.5 to 5 minutes after the last one ends) and some go on for minutes (the user asked for this: fidget a lot, do activities less often). The gaps are in `squib.json` settings.
- Lengths: activities and the bigger fidgets use `Fun.span(lo, hi)` so they run anywhere from seconds to minutes. Don't make everything a few seconds long.
- Voice lines are dry, sarcastic and a bit edgy, not cute or corny (the user asked for this). No puns for the sake of puns, no "you're doing great". Keep it mean-funny, not hateful, and no jokes about self-harm.
- Every change the user asks for also adds at least one new activity or fidget (the user asked for this).
- Versions should be procedural: a new colour, pattern or shape is data fed to one animation (see `Props.java`, `Fun.tint`, `Fun.ball`), not a new hand-drawn animation.
- A new activity or fidget needs: its id, an entry in `NAMES`, `WHERE` and `WEIGHT` (fidgets go after `LAST_ACTIVITY`), a case in `start`, `update` and `draw`, and if it has versions, a list in `Fun.variantNames` and `choose` / `chooseW` in `start` so the picker in settings can ask for one. `Fun.label` gives the picker name if the id doesn't read well.
- `tools/sim/run.sh picks` starts every activity and every version through the picker, from the floor and from a cloud, and fails if one doesn't start, end or go back to its spot. `SIM_OPTS="-Dwant=N" tools/sim/run.sh sheets NAME` renders version N.
