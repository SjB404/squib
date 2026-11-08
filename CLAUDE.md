# squib

Desktop-pet app for Android and Windows: a pixel squid drawn over other apps. One engine, written once in plain Java (no libraries, no AndroidX); the Windows app runs the same code, turned into C# when it's built.
Android: package `com.squib.buddy`, minSdk 26, targetSdk 34. Windows: .NET Framework 4 / C# 5, built with the csc.exe that ships with Windows.

## Layout

- `squib.json`: every voice line, plus settings and app lists. The app downloads it from GitHub, so line changes don't need a new build.
- `app/src/main/java/com/squib/buddy/`
  - `SquibView.java`: drawing, movement, faces, touch, speech bubbles, chatter timing. Originally recovered from a decompiled build, so many local variables have generic names (`d2`, `f3`).
  - `Fun.java`: activities (kite, fishing, magic...), quick fidgets, Minecraft gear, seasonal hats, scroll check-ins. Activity ids, names, where they can happen and how likely they are are tables at the top.
  - `Art.java`: all prop pixel art as rows of characters, and the colour for each character. `FUNNY_*` are the pictures he shows you from his phone (16 x 12, '.' is the background colour, set in `Fun.FUNNY_BG`). Original art only: no meme characters or film characters.
  - `Props.java`: props drawn by code so one animation covers many versions: balls (size, 7 patterns, colours, turned for rolling), fidget spinners, coins, and the puzzle cube (keeps every sticker, so solves are real). Colour pairs (`PAIRS`) used to recolour toys.
  - `Lines.java`: GENERATED from `squib.json` by `tools/gen_lines.py`. Don't edit by hand.
  - `Pick.java`: picks lines without repeats (shuffled deck per group, remembers recent lines across restarts).
  - `Remote.java`: downloads and applies `squib.json`.
  - `SquibService.java`, `MainActivity.java`, `Apps.java`, `PixelFont.java`, `BootReceiver.java`, `Prefs.java`.
  - `Trace.java`: the log (squib-log.txt in the app's files, exported from settings). Log anything new he notices or decides with `Trace.t/d/i/w/e`, especially the reason behind a decision, so a log the user sends can explain a bug.
- `signing/squib-debug.keystore`: the key every build has been signed with (password `android`, alias `androiddebugkey`). Always sign with it, or the new APK won't install over the phone's copy.
- `apk/Squib.apk`: the current release, committed so the user can download it from GitHub.
- `tools/build-apk.sh`: builds `apk/Squib.apk` without Gradle or the Android SDK (for cloud sessions where Google's servers are blocked).
- `desktop/`: the Windows app.
  - `port/Port.cs`: turns the engine Java (`SquibView`, `Fun`, `Art`, `Props`, `Pick`, `PixelFont`, `Prefs`) into C# in `desktop/gen`, plus `Lines` from `squib.json` and `Version` from `app/build.gradle`. `port/patches.txt`: fixes the rules can't make, and the desktop hooks (floor, eyes following the cursor, greeting wording). `gen/` is regenerated on every build and not committed: never edit it.
  - `src/`: the Windows-only parts. `Java.cs` / `Android.cs` stand in for the Java and Android classes the engine uses (same behaviour: Java rounding, Random, string hashes). `Engine.cs`: desktop versions of `Apps`, `Trace` (log in `%APPDATA%\Squib\squib-log.txt`), `Lines.get/set`, and the hooks `SquibView.deskPlace()` / `deskLook()`. `Host.cs`: the window, mouse, napping when away, focus timer, pause, menu pages. `Menu.cs`: the pixel menu. `Tray.cs`: tray icon. `Modes.cs`: app modes from the window in front (programs, and browser tab titles). `Remote.cs`: downloads `squib.json`. `Program.cs`: startup, Minecraft hotbar (`GameWatch`), start with Windows. `Sim.cs`: `Squib.exe --sim`.
  - `Squib.exe`: the current Windows release, committed like the APK.
- `tools/sim/`: runs squib on a computer with stand-in Android classes. `tools/sim/run.sh sheets [NAMES]` renders pictures of activities to `build/sim/`; `tools/sim/run.sh stress 8` simulates 8 hours of random use and fails on any crash.

## Building

- With the Android SDK: `./gradlew assembleDebug` (output in `app/build/outputs/apk/debug/`). Copy the result to `apk/Squib.apk` for a release.
- Without it: `tools/build-apk.sh`.
- Windows: `desktop\build.cmd` (runs the port, then builds `desktop\Squib.exe`). `desktop\Squib.exe --sim sheets|picks|stress 8` does what `tools/sim/run.sh` does, without Java (results in `desktop\build\sim`).

## Design language

Everything drawn should look like it belongs to him:

- One pixel size: his grid pixel (`S`). Props, bubbles and anything held or worn are built from whole grid pixels, not the smaller `T` pixels. (The speech bubble, music notes, Zs and hearts are the older exceptions.)
- Flat, solid colours. No see-through overlays, glows, gradients or shading on him or his props.
- Few colours per thing, simple blocky shapes, no outlines unless the thing is dark itself.
- Black (`SquibView.EYE`, same as his eyes) for things he wears, like the headphones: it shows on his coral body on any app, light or dark.
- Square corners; a "rounded" look comes from leaving a corner block out, not from smaller pixels.

## Rules

- Lines go in `squib.json`, then run `python3 tools/gen_lines.py`. Plain keyboard characters only: the pixel font can't show emoji, accents or curly quotes. `{mins}` only works in SCROLL_LONG.
- A new line group needs a matching name in code (an activity name in `Fun.NAMES`, or a `Lines.get("NAME")` / `maybeSay(v, "NAME", ...)` call). Unknown groups in the JSON are ignored, which keeps old app versions working with a newer file.
- No Java lambdas or method references: `tools/build-apk.sh` uses dex2jar, which can't handle them. Use anonymous classes. Keep Java 8 syntax.
- Don't turn on code shrinking (`minifyEnabled`): `Lines`, `Remote` and `Fun.rows()` look up fields by name.
- Don't add an accessibility service, or SMS / notification-listener permissions. Play Protect blocks sideloaded apps that ask for them. (An earlier keyboard-dodge feature was removed for this reason.)
- The user keeps click-through on almost all the time, so taps pass straight through squib. Don't build features that only work by tapping, petting or dragging him; use timers, app modes, the screen turning on, or the buttons in his settings / notification instead.
- Bedtime (midnight to 8am by default, set by the user in his settings: prefs `bedStart` / `bedEnd`, minutes after midnight, copied into `Fun.bedtimeStart/End` by `SquibView.reloadPrefs()`; `Remote` ignores `bedtime*` keys in squib.json): he sleeps, and wakes now and then to nag (`SquibView.bedtime()` and the late check in `wellbeing()`). Nagging runs from an hour before bedtime to 5 hours into it (`Fun.nagPhase()`), then he just sleeps. New activities must not start during bedtime. The sim uses the real clock, so run sheets with e.g. `TZ=America/Los_Angeles` if it's bedtime where you are.
- Minecraft gear is diamond only, never netherite.
- Draw original pixel art only. No characters or logos from games, films or brands (so no creepers, for example).
- For a release: bump `versionCode` and `versionName` in `app/build.gradle` (the Windows app takes its version from there too), rebuild both apps, replace `apk/Squib.apk` and `desktop/Squib.exe`, update the version and any changed behaviour in `README.md`.
- Both apps run the same engine. A change to what squib does goes in the Java once; then build the Windows app too (`desktop\build.cmd`) and fix anything the port reports, usually a patch in `desktop/port/patches.txt` whose text no longer matches `desktop/gen`. Only platform things (the window or overlay, input, menus, tray or notification, app detection) go in `desktop/src` or the Android-only classes.
- Keep the engine Java easy to port: no lambdas, method references or anonymous classes (`Pick`'s one is patched), and plain Java 8. The port handles locals named like methods, jagged arrays, int-to-char constants and switch ends; anything else it can't, make a patch.
- On Windows, app modes never move him (`deskPlace()`): he stays on the taskbar or Minecraft's hotbar and the mode only changes what he does. The phone's per-mode spots (clouds up top, the video corner) had him jumping around, because the window in front changes all the time on a computer. A trick that needs a cloud gets one just above him. `Squib.exe --sim modes` checks this. Clicking him must never take focus (`WS_EX_NOACTIVATE` plus `MA_NOACTIVATE`). The mouse reaches the engine only through `Touch` (`desktop/src/Engine.cs`), which sends the touch-down before anything else: WPF raises a mouse move when the window takes capture, and that used to arrive first, teleporting him and leaving him stuck in DRAG. `--sim modes` checks clicks too.
- Things don't happen the same way on both platforms: on Android, app modes go by the app's package; on Windows by the program in front and, for browsers, the tab's title (X, YouTube and Reddit are usually browser tabs there). Windows lists are in `desktop/src/Modes.cs` and `squib.json` `desktopApps`; Android's in `Apps.java` and `apps`.
- Windows-only line groups (FOCUS_START, FOCUS_DONE, FOCUS_STOP, CURSOR, ZOOM, GOODBYE) live in `squib.json` like the rest; the phone ignores them. Same tone rules.
- The Windows app stores stats in `%APPDATA%\Squib\prefs.txt` (first run copies the first Windows Squib's `memory.txt`, leaving it as it was). Squib on Windows must never take focus (`WS_EX_NOACTIVATE`) or block clicks outside his own pixels.
- Before shipping new animations, render them with `tools/sim/run.sh sheets NAME` and look at the pictures, and run `tools/sim/run.sh stress 8`. `SIM_OPTS="-Dreal=1" tools/sim/run.sh stress 12` runs with the real timings and reports the most activities in any hour and fidgets in any 10 minutes.
- Activities are at most 1 an hour (1 to 2 hours apart). Fidgets are frequent (1.5 to 5 minutes after the last one ends) and some go on for minutes (the user asked for this: fidget a lot, do activities less often). The gaps are in `squib.json` settings.
- Lengths: activities and the bigger fidgets use `Fun.span(lo, hi)` so they run anywhere from seconds to minutes. Don't make everything a few seconds long.
- The user doesn't care about squids: keep squid talk rare (a few facts, nothing more). He's a squid in looks and mechanics (ink, shrimp), not in what he talks about.
- Voice lines are neutral and dry (the user asked for this in 1.13, replacing the earlier sarcastic tone): plain observations about the phone, the time, what he's doing. No put-downs aimed at the user, no lecturing about screen time or sleep (just state the time), no moping ("they always leave"), not cute or corny, no puns. The exceptions the user asked for in 1.20.0: WOKE_GRUMPY (booped or petted awake) and the RANT fidget are openly annoyed, mild swearing allowed, but about things, never at the user.
- When he's annoyed (`Face.ANNOYED`, from `annoyedFor`) he shows the anger mark (`SquibView.drawAnger()`), so set `annoyedFor` for a moment of real annoyance and he gets it for free.
- Every change the user asks for also adds at least one new activity or fidget (the user asked for this). Not for small fixes.
- Versions: small fixes bump only the last number (1.17.0 to 1.17.1); new features bump the middle one.
- Versions should be procedural: a new colour, pattern or shape is data fed to one animation (see `Props.java`, `Fun.tint`, `Fun.ball`), not a new hand-drawn animation.
- A new activity or fidget needs: its id, an entry in `NAMES`, `WHERE` and `WEIGHT` (fidgets go after `LAST_ACTIVITY`), a case in `start`, `update` and `draw`, and if it has versions, a list in `Fun.variantNames` and `choose` / `chooseW` in `start` so the picker in settings can ask for one. `Fun.label` gives the picker name if the id doesn't read well.
- `tools/sim/run.sh picks` starts every activity and every version through the picker, from the floor and from a cloud, and fails if one doesn't start, end or go back to its spot. `SIM_OPTS="-Dwant=N" tools/sim/run.sh sheets NAME` renders version N.
