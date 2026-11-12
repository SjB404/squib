# squib: full reference

Everything squib does, on both apps. The short version is in [README.md](../README.md).

## Updating his lines

Both apps download `squib.json` from this repo when they start and every 6 hours after that. They keep a copy (on the phone, or in `%APPDATA%\Squib` on Windows), so they still work offline. To check right away: on the phone, open squib's settings and tap "check now"; on Windows, right-click him and click the "lines: ..." row in his menu. GitHub can take up to about 5 minutes to serve a new version of the file.

The same lines are also built into both apps (`tools/gen_lines.py` copies them into the phone app, `desktop/build.cmd` into the Windows app), so he has them on first start and offline.

## Building

One command per app. Each one builds from the same code and the same `squib.json`.

**Phone (Android).** Two ways, same result:

- **With Android Studio or the Android SDK:** `./gradlew assembleDebug`. The APK lands in `app/build/outputs/apk/debug/`.
- **Without the Android SDK:** `tools/build-apk.sh`. Needs Java 17+, python3, curl and unzip. Writes `apk/Squib.apk`.

Both sign with `signing/squib-debug.keystore`, the key every Squib build has used, so a new build installs over the one on your phone without losing his stats. It's a public debug key: fine for sideloading, not for the Play Store.

**Windows:** `desktop\build.cmd`. Needs nothing installed: it uses the C# compiler that comes with Windows (.NET Framework 4). Writes `desktop\Squib.exe`. If Squib is running from that file, quit him first (`desktop\Squib.exe --quit`, or "goodbye" in his menu), or Windows won't let the new one be written.

**Trying things without a phone:**

- `tools/sim/run.sh` (Java 17+): `sheets` draws a picture of every activity into `build/sim/`, `stress 8` simulates 8 hours of random use and fails on any crash, and `picks` runs every activity and version the picker offers.
- `desktop\Squib.exe --sim sheets`, `--sim stress 8` and `--sim picks` do the same with the Windows build (no Java needed), writing to `desktop\build\sim\` (`sim.txt` has the results). `--sim sheets MENU` also draws his Windows menu, and `--sim modes` checks that switching windows and clicking him never moves him off the taskbar.

## One squid, two apps

Everything squib does (walking, talking, all the activities and fidgets, his props and their versions, Minecraft gear, hats, bedtime, app modes, the line picker) is the phone app's Java in `app/src/main/java/com/squib/buddy`. The Windows app doesn't have its own copy: `desktop\build.cmd` first runs `desktop/port`, which turns that Java into C# in `desktop/gen` (`SquibView`, `Fun`, `Art`, `Props`, `Pick`, `PixelFont`, plus `Lines` from `squib.json` and the version number from `app/build.gradle`), then builds it together with the Windows parts in `desktop/src`. So:

- **A change to squib, made once in the Java, shows up in both apps** the next time each is built. Bump the version in `app/build.gradle` and both apps get it.
- **What's only on one platform stays out of the shared code.** Android's side: the overlay service, settings screen, notification and usage access (`SquibService`, `MainActivity`, `Apps`, `Remote`, `Pause`). Windows' side: the window, mouse, menu, tray icon, app modes for Windows, Minecraft's hotbar, Discord's message bar and the focus timer (`desktop/src`). Small differences inside the shared code (where his floor is, his eyes following the cursor, the greeting's wording) are hooks listed in `desktop/port/patches.txt`.
- `desktop/src/Java.cs` and `desktop/src/Android.cs` stand in for the bits of Java and Android the engine uses, with the same behaviour (Java's rounding, its random number generator, its string hashes, Android's drawing).
- **If the port fails,** it says why. Usually a patch in `desktop/port/patches.txt` no longer matches because the Java around it changed: update the patch's text to match the new `desktop/gen` output. Never edit `desktop/gen` by hand: it's rewritten on every build (and not committed).

The port understands the plain Java squib is written in. Keep to it: Java 8, no lambdas or method references (the phone's build tool can't handle them either), and avoid anonymous classes (the one in `Pick` is a patch).

## squib.json

```json
{
  "version": 1,
  "settings": { "scrollCheckMinutes": 15, ... },
  "lines": { "BOOP": ["boop!", "hey!"], ... },
  "apps":  { "WATCH": ["com.netflix.mediaclient", ...], ... },
  "minecraft": ["com.mojang.minecraftpe", ...]
}
```

### settings

| setting | default | what it does |
|---|---|---|
| `scrollCheckMinutes` | 15 | minutes between scroll check-ins |
| `activityGapMin` / `activityGapMax` | 3600 / 7200 | seconds between his activities (a random time in between). 1 to 2 hours, so at most 1 an hour |
| `fidgetGapMin` / `fidgetGapMax` | 90 / 300 | seconds between fidgets, counted from when the last one ends. 1.5 to 5 minutes, so he fidgets a lot |
| `snowEvery` | 0.9 | seconds between snowflakes in December. `0` turns snow off |

### lines

There are 1330 lines in 157 groups. His tone is neutral and dry: plain observations, not rude, not preachy, not cute. The exceptions are being woken up (WOKE_GRUMPY) and rants (RANT), where he is openly annoyed, but at things, never at you. Each group **replaces** the built-in lines for that group. A group that's missing or empty keeps the built-in lines. Groups the app doesn't know about are ignored, so older versions of the app still work with a newer file.

**No repeats:** each group works like a shuffled deck. Every line in it gets said once before any line comes around again. He also remembers the last 400 lines he said (saved on the phone, so restarting him doesn't reset it), and when a deck gets reshuffled, the most recent ones go to the bottom.

| group | when he says it |
|---|---|
| THOUGHTS, MUTTERS, JOKES, FACTS, STATS | random chatter. The **talk** button says a fact 30% of the time, a joke 35%, a thought 35%. Facts are mostly general weird facts, only a few about squids |
| MORNING, AFTERNOON, EVENING, NIGHT | random chatter, depending on time of day |
| OPENERS | sometimes stuck in front of chatter ("hm. ", "ok so, ") |
| PARTY, WINTER, SPOOKY | extra chatter on special days, in December, and Oct 20 to 31 |
| BOOP | when tapped (clicked, on Windows) |
| SPAM | when tapped or clicked 5 times fast |
| GRABBED | when picked up |
| PET | when petted |
| WAKE | when you turn the screen back on after a while (on Windows: when you come back after 10+ minutes away) |
| FED, HUNGRY | after eating, and when he hasn't eaten in hours |
| BREAK | after 50 minutes of phone (or computer) use |
| LATE | bedtime nagging, the hour before bedtime and its first hour (11pm to 1am by default) |
| LATE_NIGHT | bedtime nagging after that, up to 5 hours into bedtime (1am to 5am by default) |
| BEDTIME | when he goes to sleep for the night |
| WOKE_NAG | when he wakes up at night to tell you to go to bed, or when the screen comes back on (or you come back to the computer) at night |
| WOKE_GRUMPY | when you boop or pet him awake, any time of day (1.20.0). He's annoyed for a few seconds, with the anger mark |
| RANT_START, RANT, RANT_END | the RANT fidget: an opener ("you know what bugs me?"), the thing that annoys him, then sometimes "anyway." |
| WATCH_ENTER, WATCH, WATCH_LONG | video apps (WATCH_LONG: after 2 hours) |
| MUSIC_ENTER, MUSIC | music apps |
| SCROLL, SCROLL_LONG | scrolling apps. SCROLL_LONG is said every `scrollCheckMinutes` (15) of scrolling, going through the lines in order and starting over at the end. `{mins}` becomes how long you've been scrolling |
| CHAT_ENTER | chat apps |
| GAME_ENTER, GAME, MINECRAFT_ENTER | games |
| NAV_ENTER, NAV | map apps |
| READ_ENTER, READ | reading apps |
| MINECRAFT | in Minecraft, when he switches items or starts mining |
| ORE, TORCH | in Minecraft, when he mines a block of ore or places a torch |
| one group per activity (BALL, KITE, MAGIC, ...) | when he starts that activity (not every time). See the activities table |
| one group per fidget (STRETCH, SPINNER, HICCUP, ...) | sometimes, when he does that fidget |
| FOCUS_START, FOCUS_DONE, FOCUS_STOP | Windows only: the 25-minute focus timer starts, finishes, or is stopped |
| CURSOR, ZOOM | Windows only: the cursor hangs around near him, or flies across the screen |
| GOODBYE | Windows only: "goodbye" in his menu |
| JUGGLE_DROP, FISHING_BITE, FISH_SHRIMP, FISH_FISH, FISH_BOOT, RAINBOW, BALLOON_POP, BALLOON_BYE, MAGIC_TADA, STAR_WISH, PAINT_DONE, GUM_POP, MARSHMALLOW_FIRE, MARSHMALLOW_YUM, BOOMERANG_CATCH, BOOMERANG_BONK, GARDEN_DONE, BOWLING_STRIKE, BOWLING_MISS, FRIEND_BYE, BLOCKS_FALL, BLOCKS_DONE, MAIL_READ, CUBE_DONE, CUBE_AGAIN, CUBE_GIVEUP, CUBE_OOPS, COIN_HEADS, COIN_TAILS, COIN_DROP, DICE_SIX, DICE_ONE, TOP_FALL, FUNNY_CAT, FUNNY_PIGEON, FUNNY_TOAST, FUNNY_CAR, FUNNY_DOG, FUNNY_RACCOON, FUNNY_PLANT | moments during activities. MAIL_READ is what the letter says |

These get filled in when he says the line:
`{time}` the current time, `{mins}` minutes scrolled (SCROLL_LONG only), `{day}` how many days he's lived on your phone, `{fed}` shrimp eaten, `{boops}`, `{pets}`.

Stick to plain keyboard characters. The pixel font has no emoji or accented letters, so those show up as `?`. `tools/gen_lines.py` checks for this.

### minecraft

Apps where he gets his Minecraft gear. Anything listed here also counts as a game.

### apps

Adds apps to a type. These get added on top of the built-in list. Types: `WATCH`, `MUSIC`, `SCROLL`, `CHAT`, `GAME`, `NAV`, `READ`, `HIDE`. To find an app's name, open its Play Store page: it's the part after `id=` in the link.

### desktopApps

The same for Windows, where things are programs or browser tabs instead of apps. Optional; the phone app ignores it.

```json
"desktopApps": {
  "WATCH": ["vlc", "title:Netflix"],
  "SCROLL": ["title:Hacker News"]
}
```

A plain name is a program, as Task Manager's Details tab shows it, without `.exe` (any case). `title:` is a bit of a browser tab's title (any case). These go on top of the built-in lists in [App modes on Windows](#app-modes-on-windows), and a `title:` here is checked before the built-in ones.

## Pausing

On the phone, his notification has **hide**, **feed** and **pause**. Some apps refuse taps while anything is drawn over them, even with click-through on. **pause** stops him completely (nothing left on screen) and leaves a "squib is paused" notification with **start** and **stop**.

- **come back after a pause** (on by default): he starts again by himself after **pause for** minutes (5 by default, 1 minute to 3 hours, both in his settings). The notification says what time.
- Off: he waits until you press **start** in that notification, or start him from the app.
- **stop** in the paused notification cancels the pause and removes the notification, so he stays off.
- If Android won't let him start in the background, the notification changes to say so, and **start** still works.

On Windows, **pause...** in his menu hides him for 5, 15 or 30 minutes, 1 or 3 hours, or until you open Squib.exe again, and the tray icon has **pause for 15 minutes** / **unpause**. Clicking the tray icon also brings him back.

## Modes

He switches mode based on the app you have open. This needs usage access turned on, and the "app modes" switch in his settings. On Windows he goes by the window in front (the program, or the page in a browser) instead: see [App modes on Windows](#app-modes-on-windows). Switching into a mode (other than HIDE) has a 30% chance of a `<MODE>_ENTER` line, at most once per 10 minutes per mode.

Bedtime nagging and the 50-minute break reminder happen in every mode except HIDE.

## Bedtime

From midnight to 8am by default he goes to sleep, in every mode (with his popcorn if you're watching something, on his cloud if he's up top). No activities, fidgets or chatter while it's bedtime. In Minecraft he stops swinging and mining.

**Setting the times:** "goes to sleep at" and "wakes up at" in his settings, in half-hour steps. Setting them the same turns bedtime off. This is set on the phone; `squib.json` doesn't change it (versions before 1.15.3 read `bedtimeStart` / `bedtimeEnd` from its settings).

He tells you it's late from an hour before bedtime until 5 hours into it (11pm to 5am with the default times), or until bedtime ends if that's sooner. After that he just sleeps, so he doesn't nag in the morning while you're getting up. He wakes up now and then to say it (a WOKE_NAG line, or a LATE / LATE_NIGHT one): about every 30 to 50 minutes in the hour before bedtime, every 20 to 30 minutes in its first hour, then about every 11 to 19 minutes. He goes back to sleep about 15 seconds later.

Every time he tells you it's late (the LATE, LATE_NIGHT and WOKE_NAG lines, including the screen-on one below and the nag button) the phone gives two light ticks. On phones with a proper haptic motor that's the crisp "tick" effect, not a buzz; older phones get the closest short tap they can do.

Turning the screen back on after it's been off for 10+ minutes also wakes him, with a WOKE_NAG line (or, after the nagging hours, a plain WAKE hello with no tick). A shorter screen-off doesn't. Tapping or petting him wakes him too (not with click-through on), and he's annoyed about it (1.20.0, see [Woken up](#woken-up)). When woken like that he stays up for 15 to 25 seconds, then lies down again.

Nothing else wakes him at night. Switching apps or unlocking the phone moves him to the new app's spot but he stays asleep, and says nothing about the app. While he's awake at bedtime he stays where he is (no walking around or hopping), just looks around.

**Testing it:** the **nag** button in his settings makes him do the late nag right now, at any hour, with the phone tick. Outside the nagging hours it's a LATE line; at bedtime, if he's asleep, he wakes up for it like he does on his own.

### everything else (HOME)

Your home screen and any app not listed below. Walks along the bottom of the screen, wanders, hops, looks around, and does activities and fidgets (see below).

- Talks every 35 seconds to 3 minutes: thoughts, mutters, jokes, facts, stats, time-of-day lines, and HUNGRY when he hasn't eaten in 2 hours.

### watch (WATCH)

Walks to the bottom-right corner and sits down with popcorn. Kernels pop out now and then.

- Says a WATCH line every 3 to 6 minutes.
- After 2 hours: one WATCH_LONG line.
- After 90 minutes he may fall asleep. Tap to wake him.

**Apps:** Netflix `com.netflix.mediaclient`, YouTube `com.google.android.youtube`, YouTube Kids `com.google.android.apps.youtube.kids`, Disney+ `com.disney.disneyplus`, Prime Video `com.amazon.avod.thirdpartyclient`, Max `com.wbd.stream`, HBO `com.hbo.hbonow`, Hulu `com.hulu.plus`, Twitch `tv.twitch.android.app`, Crunchyroll `com.crunchyroll.crunchyroid`, Plex `com.plexapp.android`, VLC `org.videolan.vlc`, Paramount+ `com.cbs.app`, Peacock `com.peacocktv.peacockandroid`, Google TV `com.google.android.videos`, MX Player `com.mxtech.videoplayer.ad`, Apple TV `com.apple.atve.androidtv.appletv`, BBC iPlayer `com.bbc.iplayer.android`, Channel 4 `com.channel4.ondemand`, Pluto TV `tv.pluto.android`, Tubi `com.tubitv`, Kick `com.kick.mobile`

**Also:** apps not on any list that Android files under: Android category video.

### Log (1.18.1)

Everything he sees and does is written to a log in the app's own storage (about 2 MB, plus the one before it): screen on, off and unlock, every app change, every music check once a second (what Android says, the audio mode, and what kinds of sound are listed as playing), the headphones going on and off and why (or why not), activities starting and ending, every line he says, touches, settings, downloads of `squib.json`, and crashes with their details. **export log** in his settings saves it as a text file (with a header: versions, phone, permissions, settings); **clear log** empties it. The size is shown at the bottom of his settings. Levels: T trace (every check), D detail, I events, W problems, E errors.

### music (MUSIC)

Walks along the bottom. When standing still he bops to a beat and music notes float up. Dancing and guitar happen much more often here.

- Says a MUSIC line every 4 to 8 minutes.

**Headphones (1.16.0):** when music is actually playing in Spotify, SoundCloud or Apple Music, he puts on a pair of black headphones (1.18.0: black like his eyes, the band along the top of his head, cups sticking out at the sides) and listens along: nods at the song's own speed, sometimes shuts his eyes, sometimes holds one side of the headphones, walks around less, and now and then says a LISTEN line. If you leave the app and the music keeps playing, he keeps them on in other apps (1.17.0), with fewer notes, and keeps doing his fidgets and activities with them on, nodding along. Android tells any app whether something is playing, without a permission, but not which app it comes from (it also has to see a music, video or game player in Android's list of what's playing, 1.18.2, because on some phones the yes/no check says "playing" for a few seconds after the screen comes on). So since 1.19.0 the proof is the app on screen: they only go on after about 1.5 seconds of music while Spotify, SoundCloud or Apple Music is open, never anywhere else, never while his own settings are open, and not if the sound started in a video, scrolling, game or other music app (it counts as that app's sound until it stops). They come off when nothing has played for about 6 seconds (right away when you unlock the phone if it was paused while locked), or when a video app, a scrolling app (TikTok, Instagram...), another music or podcast app (YouTube Music, Deezer...), a call or the camera comes up. The bottom line of his settings shows what Android says about music right now, for checking. Lines: LISTEN_ON (putting them on), LISTEN, LISTEN_OFF (music stopped).

**Apps:** Spotify `com.spotify.music`, YouTube Music `com.google.android.apps.youtube.music`, Apple Music `com.apple.android.music`, SoundCloud `com.soundcloud.android`, Deezer `deezer.android.app`, Tidal `com.aspiro.tidal`, Amazon Music `com.amazon.mp3`, Pandora `com.pandora.android`, Shazam `com.shazam.android`, Audible `com.audible.application`, Podcast Addict `com.bambuna.podcastaddict`, Pocket Casts `au.com.shiftyjelly.pocketcasts`

**Also:** apps not on any list that Android files under: Android category audio.

### scrolling (SCROLL)

Gets his own little phone out and scrolls along with you: screen facing him (you see the back, in a random case colour), thumb flicks, sometimes stopping on a post, now and then a reaction face. Sessions last from under a minute to several minutes, with short breaks in between (when he walks around or fidgets). Every so often he holds up a big picture of something funny he found and says something about it (see PHONE below). Scrolling along doesn't count as an activity, so it doesn't push back his other activities.

- Talks every 35 seconds to 3 minutes: 30% of the time a SCROLL line, otherwise normal chatter.
- Every 15 minutes of scrolling (`scrollCheckMinutes`), no limit: a SCROLL_LONG check-in (needs break reminders on). The count resets after 5 minutes away from scrolling apps.

**Apps:** TikTok `com.zhiliaoapp.musically`, TikTok (Asia) `com.ss.android.ugc.trill`, Instagram `com.instagram.android`, Threads `com.instagram.barcelona`, Snapchat `com.snapchat.android`, Reddit `com.reddit.frontpage`, X `com.twitter.android`, Facebook `com.facebook.katana`, Tumblr `com.tumblr`, Pinterest `com.pinterest`, BeReal `com.bereal.ft`, LinkedIn `com.linkedin.android`, VSCO `com.vsco.cam`, Bluesky `bsky.app`, Lemon8 `com.lemon8.android`

**Also:** apps not on any list that Android files under: Android category social.

### chat (CHAT)

Sits on a cloud near the top right (a bit lower than in other apps, so there's room over his head) and texts on his own phone (1.17.0): thumbs tapping, phone held up facing him (you see the back of it, in a random case colour). Message bubbles go back and forth over his head like a text chat in a film, built from the same size blocks as he is, with words as solid bars: his on the right in one colour per session, theirs on the left in white, newest at the bottom pushing the older ones up until they fade. Sometimes a photo or a heart instead of words, a "..." bubble while they type, double texts from either side, long waits for a reply, and a reaction to what they sent (happy, surprised, now and then annoyed). Sessions last from 20 seconds to several minutes, with breaks for fidgets in between, and don't count as activities.

- Never chatters on his own here.

**Apps:** WhatsApp `com.whatsapp`, WhatsApp Business `com.whatsapp.w4b`, Discord `com.discord`, Telegram `org.telegram.messenger`, Messenger `com.facebook.orca`, Signal `org.thoughtcrime.securesms`, Google Messages `com.google.android.apps.messaging`, Samsung Messages `com.samsung.android.messaging`, Slack `com.Slack`, Teams `com.microsoft.teams`, Viber `com.viber.voip`, LINE `jp.naver.line.android`, KakaoTalk `com.kakao.talk`, Gmail `com.google.android.gm`, Outlook `com.microsoft.office.outlook`, Beeper `com.beeper.android`, `com.beeper.chat`

### games (GAME)

Sits on a cloud in the top-left corner.

- Says a GAME line every 3 to 6 minutes.
- Minecraft uses MINECRAFT_ENTER instead of GAME_ENTER.
- In Minecraft he gets diamond gear. See [Minecraft](#minecraft-1) below.

**Apps:** Minecraft `com.mojang.minecraftpe`, Roblox `com.roblox.client`, Clash Royale `com.supercell.clashroyale`, Brawl Stars `com.supercell.brawlstars`, Clash of Clans `com.supercell.clashofclans`, Genshin Impact `com.miHoYo.GenshinImpact`, Honkai: Star Rail `com.HoYoverse.hkrpgoversea`, PUBG Mobile `com.tencent.ig`, Call of Duty Mobile `com.activision.callofduty.shooter`, Among Us `com.innersloth.spacemafia`, Subway Surfers `com.kiloo.subwaysurf`, Candy Crush `com.king.candycrushsaga`, Pokemon GO `com.nianticlabs.pokemongo`, Chess.com `com.chess`, Free Fire `com.dts.freefireth`, EA FC Mobile `com.ea.gp.fifamobile`, Mobile Legends `com.mobile.legends`, Fortnite `com.epicgames.fortnite`, Arknights `com.YoStarEN.Arknights`

**Also:** apps not on any list that Android files under: Android category game, or apps marked as a game.

### maps (NAV)

Sits on a cloud in the top-right corner.

- Says a NAV line every 5 to 10 minutes.

**Apps:** Google Maps `com.google.android.apps.maps`, Waze `com.waze`, Citymapper `com.citymapper.app.release`, Uber `com.ubercab`, Lyft `me.lyft.android`

**Also:** apps not on any list that Android files under: Android category maps.

### reading (READ)

Sits on a cloud in the top-right corner.

- Says a READ line every 4 to 8 minutes.

**Apps:** Kindle `com.amazon.kindle`, Play Books `com.google.android.apps.books`, Wattpad `com.wattpad.wattpad`, Medium `com.medium.reader`, Duolingo `com.duolingo`, Google News `com.google.android.apps.magazines`

**Also:** apps not on any list that Android files under: Android category news.

### hides (HIDE)

Disappears completely, including speech and bedtime nagging.

**Apps:** Pixel Camera `com.google.android.GoogleCamera`, Samsung Camera `com.sec.android.app.camera`, Camera `com.android.camera`, Camera `com.android.camera2`, OnePlus Camera `com.oneplus.camera`, Motorola Camera `com.motorola.camera3`, Google Phone `com.google.android.dialer`, Samsung call screen `com.samsung.android.incallui`, call screen `com.android.incallui`, phone calls `com.android.server.telecom`, Zoom `us.zoom.videomeetings`

**Also:** apps not on any list that Android files under: any app with "camera" or "incallui" in its name.

Apps added in the `apps` section of `squib.json` go on top of these lists.

## Activities

Every 1 to 2 hours (`activityGapMin` / `activityGapMax`), when he's standing around, he picks something to do, so at most 1 an hour. The wait carries over if he's restarted, so restarting him doesn't bring the next one sooner. He won't pick any of his last 3 activities. About half the time he says a line from that activity's group when he starts. The **trick** button in his settings makes him do one right away.

**Picking one:** **pick a trick** and **pick a fidget** in his settings list everything he can do. Ones marked `...` have versions: pick one, or "random". If it needs a different spot (fishing needs a cloud, skating needs the floor), he pops over there for it and pops back after.

**Versions:** balls, spinners, coins and the puzzle cube are drawn by code, not stored as fixed pictures, so one animation covers every version. A ball can be any size from 4 to 7 pixels, in 7 patterns (plain, striped, two-tone, spotty, beach, checkered, banded) and any two colours, and the pattern turns as it rolls.

**Lengths:** most activities and the bigger fidgets run for a random length, from a few seconds to several minutes. Short ones are more common, but each longer stretch is still fairly likely: he can read a book for 4 minutes, sit on a puzzle cube for 6, or play with a spinner for 5.

| activity | what happens | where |
|---|---|---|
| BALL | a ball drops in, he chases it and kicks it around 3 to 6 times. Random size, pattern and colours | bottom of the screen only |
| PLANE | winds up and throws a paper plane that glides off screen. His eyes follow it | anywhere |
| BUBBLES | blows bubbles with a wand. They float up and fade | anywhere |
| BOOK | holds up a book and reads, turning pages now and then | anywhere, more often in reading apps |
| YOYO | plays with a yo-yo (random colour) | anywhere |
| DANCE | dances: arms swapping, hopping on the beat, music notes | anywhere, much more often in music apps |
| WAVE | waves at you | anywhere |
| BUTTERFLY | a butterfly flies in and circles his head. He hops trying to catch it, then it flies away | anywhere |
| NAP | takes a 20 to 45 second nap. Tap him to wake him | anywhere, more often at night |
| TEA | drinks a mug of tea. Steam rises, and he takes sips | anywhere, more often in reading apps |
| GUITAR | strums a little guitar, music notes float up | anywhere, much more often in music apps |
| JUGGLE | juggles 3, 4 or 5 balls in a loop over his head, each with its own pattern and colour (sometimes a matching set). Sometimes drops them all at the end | anywhere |
| KITE | flies a kite high above him on a long string, with a bow tail | bottom of the screen only |
| FISHING | casts a line down from his cloud and waits. Gets a bite, reels in a shrimp (eats it), a fish (throws it back) or an old boot (annoyed) | only when sitting on a cloud |
| RAIN | a rain cloud appears over him. He gets grumpy, opens an umbrella, and when the rain stops there's a rainbow | anywhere |
| SKATE | rides a skateboard back and forth across the screen with a kickflip after each run | bottom of the screen only |
| ICECREAM | eats a strawberry ice cream in three licks. It drips | anywhere |
| BALLOON | holds a balloon (one of 9 colours) and bounces. At the end it pops, or he lets go and it floats away | anywhere |
| MAGIC | puts a top hat on the floor, taps it with a wand, and pulls out a shrimp, a dove, a tiny squid or flowers. Then the hat vanishes | anywhere |
| STARGAZE | looks through a telescope while shooting stars go by | bottom of the screen, 9pm to 5am only |
| SNEEZE | "ah... ah..." then sneezes ink and shoots backwards | anywhere |
| JUMPROPE | skips rope, hopping each time it goes under him | bottom of the screen only |
| PAINT | paints a picture on an easel (a shrimp, the sun, a heart, himself or a landscape), then shows it off | anywhere |
| SELFIE | takes three selfies with a tiny phone, with flash | anywhere |
| PARTY | throws confetti. Only on special days (see below) | anywhere |
| GUM | blows a bubblegum bubble that grows over his face and pops all over him | anywhere |
| CAMPFIRE | roasts a marshmallow over a little campfire. Sometimes it catches fire and he blows it out. Then he eats it | bottom of the screen only |
| DRUMS | plays a drum with two sticks, music notes | anywhere, much more often in music apps |
| HULA | hula hoop round his middle, in two random colours | anywhere |
| BOOMERANG | throws a boomerang that loops out and comes back. He catches it, or it bonks him on the head | anywhere |
| GARDEN | plants a seed and waters it until a flower grows. The flower stays on the floor for 10 minutes (up to 3) | bottom of the screen only |
| BOWLING | rolls a ball (random pattern and colours) at three pins. Strike, or a miss that leaves some standing | bottom of the screen only |
| KARATE | karate-chops a plank on two bricks in half | bottom of the screen only |
| WORKOUT | lifts a dumbbell, 4 reps each arm, with sweat | anywhere |
| FRIEND | a little teal squid walks in, they say hi and hop, then it walks off | bottom of the screen only |
| BLOCKS | stacks four toy blocks (random colours) into a wobbly tower. Sometimes it falls over | anywhere |
| MAIL | a letter floats down, he catches it and reads it out (MAIL_READ) | anywhere |
| MEDITATE | sits with his eyes closed and floats up a little | anywhere |
| PHONE | gets his phone out, scrolls for a bit, and shows you something funny he found: a cat crammed in a box, a pigeon judging you, toast with a face, terrible parking, a dog in sunglasses, a raccoon in a bin, or a dead plant captioned 'thriving'. The cat, dog and car come in different colours. In scrolling apps he does this on his own (see scrolling) | anywhere |
| CUBE | twists a puzzle cube, usually for minutes. The cube keeps track of every sticker, so a solve is real: it gets scrambled, then he undoes the twists, stopping to think now and then. After a solve he often scrambles it and goes again. Versions: solves it (holds it up), speed solve, a 2x2 cube, gives up (works at it for a while, then shakes it), or messes up a solved one | anywhere |

He doesn't do activities while watching videos (he has popcorn), in hidden apps, or in Minecraft. Picking him up, petting him, or feeding him stops whatever he's doing. While your finger is on him (holding or petting) he stays where he is, and for a few seconds after (1.19.0).

### Woken up

Since 1.20.0, booping or petting him while he's asleep (bedtime, a nap, or napping while you're away from the computer) wakes him up annoyed: an annoyed face, the anger mark, a little jolt, and a WOKE_GRUMPY line ("it's 2:03 am. i was sleeping."). It lasts about 3.5 seconds; petting him during it gives no hearts until he's over it.

**The anger mark:** four small red corners pointing in, one of his pixels thick (5 x 5 pixels in all), just above the top right corner of his head. It throbs (the corners jump apart and back) and shows whenever he looks annoyed: woken up, too many boops, ranting, a missed paper toss, a bad dice roll and so on.

### Fidgets

Between activities, 1.5 to 5 minutes after the last fidget ends (`fidgetGapMin` / `fidgetGapMax`), he does something small: STRETCH (arms up), SPIN, TRIP (stumbles and sits down for a second, bottom of the screen only), SCRATCH (scratches his head, thinking), YAWN, SPINNER, HICCUP (2 to 5 of them), SHAKE (shakes himself off like a wet dog), WHISTLE, COIN, STRESSBALL, DICE, PEN, CARDS, BUBBLEWRAP, TOSS, SPRING, TOP, RANT or PINWHEEL. He only says something about 1 time in 8 (RANT always talks). SPINNER, COIN, STRESSBALL, DICE, PEN, CARDS, BUBBLEWRAP, SPRING, TOP and PINWHEEL can go on for minutes. Since 1.19.0, TEXT isn't a random fidget any more: he only texts in messaging apps, or when you pick it in his settings.

- **SPINNER:** a fidget spinner with three arms, two arms, four arms, a ring, or a star, in random colours. He flicks it, it spins fast enough to blur, then slows down, and he flicks it again, from a few seconds up to about 5 minutes. Now and then he tosses it up while it spins, or moves it to his other hand. Between flicks he sometimes just stares at nothing.
- **COIN:** flips a gold, silver or copper coin, once or up to about 15 times in a row. It spins in the air and he catches it (heads or tails), or now and then misses and it bounces on the floor, which ends it.
- **STRESSBALL:** squeezes a stress ball (any ball pattern and colours), squashing it flatter the harder he squeezes, with an annoyed face. Sometimes tosses it over his head to the other hand.
- **DICE:** shakes a die (white, red, black, blue, green, yellow or purple) and throws it on the floor in front of him. It tumbles and bounces, then he looks at what he rolled: happy at a six, annoyed at a one. Up to about 10 rolls in a row.
- **PEN:** clicks a pen in any of the 11 colours (grey or matching clip), in quick bursts with pauses. The button sinks in when the tip comes out. Now and then he twirls it twice round his fingers, sometimes passing it to the other hand. A few seconds to a few minutes.
- **CARDS:** shuffles a small deck (card backs in a random bright colour). Riffle: half the deck in each hand, the cards drop in turn onto a pile in front of him, then he splits it and goes again. Card spring: the cards arc from one hand to the other over his head, then back. Now and then the spring goes wrong and the cards spray onto the floor, and he looks annoyed. A few seconds to a few minutes.
- **BUBBLEWRAP:** pops a sheet of bubble wrap held against his front: 5 to 9 bubbles across, 2 or 3 down, clear or tinted blue, pink, green or yellow. One at a time in random order (with the odd pause), row by row, or a few one at a time and then he wrings the sheet and the rest go off at once. When a sheet is done he gets another out, from one sheet up to a few minutes. Lines: BUBBLEWRAP, BUBBLEWRAP_DONE.
- **TEXT:** the same texting as in chat apps, from a few seconds to a couple of minutes, only when picked in his settings (1.19.0). He doesn't say anything when it starts: the message bubbles are his talking.
- **TOSS:** scrunches up a piece of paper and throws it at a bin on the floor a little way off (bin in a random colour). It goes in (happy) or lands next to it (annoyed), 1 to about 8 throws. Bottom of the screen only. Lines: TOSS, TOSS_IN, TOSS_MISS.
- **SPRING (1.19.0):** pours a spring toy from one hand to the other, the rings arching over his head, then back, since it's upside down now. Colours are picked each time: one colour, two colours striped, a run of colours, or gold or silver. Now and then it slips halfway over, the rings fall to the floor, and he looks annoyed. A few seconds to a few minutes. Lines: SPRING.
- **TOP:** winds up a spinning top (any of the 11 colours) and lets it go on the floor in front of him (behind him if he's at the edge of the screen). Its stripes turn as it spins, it drifts about, bumping off him and the edges, and it wobbles more and more as it slows down, then tips onto its side. He watches it the whole time. Spins last from 3 seconds to about 40, and he often picks it up and spins it again, up to 8 times.
- **RANT (1.20.0):** gets worked up about something: a RANT_START opener ("you know what pisses me off?"), then a RANT line about everyday annoyances (chargers that only work at one angle, ads louder than the video, never about you) while he waves his arms and stamps, with the anger mark. Then he calms down, sometimes with a RANT_END line ("anyway."). Not when chatty is off.
- **PINWHEEL (1.21.0):** holds up a pinwheel on a stick (any of the 11 colours, blades in its light and dark shades) and blows on it now and then. It spins up with each blow and slows down in between. A few seconds to a couple of minutes.
## Minecraft

In Minecraft (and the launchers in the `minecraft` list) he gets diamond gear. Only diamond, never netherite.

- Holds one of: diamond sword, diamond pickaxe, diamond axe, diamond shovel, or a diamond. He switches to a different one every 20 to 50 seconds.
- Wears a diamond helmet most of the time (decided each time you open Minecraft).
- Every few seconds he does something with it:
  - **swing:** raises the item and strikes, with a little swoosh
  - **mine:** chops repeatedly, with bits of dirt and stone flying (pickaxe, axe, shovel)
  - **show off:** holds it up high, with sparkles (always for the diamond)
  - **mine ore:** a block of diamond ore appears next to him. He mines it (it cracks more with each hit), it breaks, and he holds up the diamond
  - **torch:** places a torch on the ground. It flickers for 25 to 40 seconds. Up to 3 at a time
- Says MINECRAFT lines sometimes when switching items or starting to mine.
- The trick button in Minecraft makes him mine ore or place a torch.

## Special days and seasons

- **Party days:** January 1st, and his day 7, 30, 50, then every 50 days from day 100, and every 365 days. He wears a party hat all day, throws confetti, and says PARTY lines.
- **December:** winter hat, snow falling around him (not on clouds or while watching), and WINTER lines.
- **October 20 to 31:** witch hat and SPOOKY lines.
- Hats come off in Minecraft (he wears the diamond helmet instead).

## Squib on Windows

The same squid as on the phone. Everything above works the same, because it's the same code: activities, fidgets, versions and the pickers, Minecraft gear, special days and hats, bedtime, app modes, his lines and how they're picked. His stats and settings belong to each device; the phone and the computer don't share them.

Run `desktop\Squib.exe` (from anywhere). Only one Squib runs at a time.

### Where he lives

He walks along the top of the taskbar (the bottom of the main screen's free area, wherever the taskbar is), on top of every window. His window never takes focus, so he doesn't interrupt typing or games, and clicks go straight through the empty space around him. While Minecraft is open he stands on its hotbar instead (see [Minecraft on Windows](#minecraft-on-windows)), and while Discord is in front, on its message bar (see [Discord on Windows](#discord-on-windows)).

### Mouse

| do this | and he |
|---|---|
| click him | boop: a BOOP line or a heart. Asleep: wakes up annoyed (WOKE_GRUMPY, anger mark) |
| double-click him | jets off in a cloud of ink |
| click him 5 times fast | gets annoyed (SPAM) and inks |
| drag him | picks him up (GRABBED). Let go to drop or throw him; he drifts down |
| wiggle the cursor over him, or hold the button down on him | petting: hearts, a blush and a PET line |
| click his speech bubble | closes it |
| right-click him | his menu |

His eyes follow the cursor while he's standing around. If the cursor stays near him for a few seconds he may say a CURSOR line, and if it flies across the screen, a ZOOM line (only on the desktop or other HOME windows, when he's not busy, and at most every 25 to 105 seconds).

### His menu

Right-click him (or click the tray icon). Click anywhere else, or press Esc, to close it.

- **header:** his day count, shrimp eaten, and the version (always the same as the phone app's)
- **feed a shrimp:** a shrimp floats down; he walks over and eats it (FED)
- **talk:** a fact, a joke or a thought
- **do a trick:** an activity right now. In Minecraft: he mines a block of ore or places a torch
- **pick a trick... / pick a fidget...:** everything he can do, like the pickers on the phone. Ones marked `...` have versions: pick one, or "random"
- **start 25-min focus:** says a FOCUS_START line, then keeps quiet (no chatter) and shows the time left over his head. At the end: a FOCUS_DONE line and hearts, and the 50-minute break timer starts over. Click it again to stop early (FOCUS_STOP)
- **pause...:** see [Pausing](#pausing)
- **size** (small, medium, large) and **text size** (small, medium, large, for his speech bubbles and this menu): click to change
- **app modes:** on, he changes what he does with the window in front ([App modes on Windows](#app-modes-on-windows)). Off, he acts as if you're on the desktop all the time
- **walk on minecraft's hotbar**
- **walk on discord's message bar**
- **chatty:** off, he doesn't talk on his own (no chatter, activity lines, mode lines or cursor lines). He still answers when you click, pet or feed him, and still does reminders
- **break reminders and check-ins:** the 50-minute BREAK reminder and the SCROLL_LONG check-ins
- **click-through:** clicks go straight through him, like he isn't there. Use the tray icon to get his menu back
- **start with windows:** starts him when you sign in (a normal startup entry, also listed in Task Manager's startup apps)
- **lines:** where his lines came from and when (built in, saved copy, or updated at a time). Click to check GitHub now
- **goodbye:** a GOODBYE line, a wave, and he fades out and quits

### Tray icon

His face in the notification area. Left-click: his menu (or unpause him). Right-click: squib's menu, feed a shrimp, talk, do a trick, pause for 15 minutes / unpause, click-through, goodbye.

### When you're away

No mouse or keyboard for 3 minutes and he lies down for a nap. Coming back counts like turning the phone's screen back on: he gets up, after 10+ minutes away he may say a WAKE line (at bedtime, a WOKE_NAG line), and after 5+ minutes away the 50-minute break timer starts over. Bedtime (midnight to 5am) works the same as on the phone.

### Minecraft on Windows

When Minecraft (Java Edition, or Bedrock from the Microsoft Store) is open and not minimised, and no other window is in front of its hotbar, he walks along the top of the hotbar instead of the taskbar. He works out where the hotbar is the same way Minecraft does, from the window's size and the GUI scale in the game's `options.txt` (from its `--gameDir`, or the usual `.minecraft` folder), so nothing reads the screen. `Squib.exe --find-hotbar` writes what he works out to `%TEMP%\squib-hotbar.txt`, for checking.

There he gets his diamond gear and does everything in [Minecraft](#minecraft-1) (needs **app modes** on). With **walk on minecraft's hotbar** off he stays on the taskbar, still with his gear when Minecraft is the window in front.

### Discord on Windows

Since 1.21.0, while Discord (or Discord PTB or Canary) is the window in front, on the main screen and not minimised, he walks along the top of its message bar, the rounded box you type in, instead of the taskbar. When the bar grows with a longer message he steps up with it, and when you switch to another window he falls back down to the taskbar. Getting up there from further away is a poof.

He finds the bar through UI Automation, the accessibility API screen readers use, so it follows Discord's real layout whatever the theme, zoom or window size. Nothing reads the screen. The first time he asks, Discord switches its accessibility support on, which can make it use a bit more CPU; turn **walk on discord's message bar** off in his menu and he stops asking. `Squib.exe --find-chatbar` writes what he finds in the open Discord window to `%TEMP%\squib-chatbar.txt`, for checking.

### App modes on Windows

Once a second he looks at the window in front of everything:

1. In a **browser** (Chrome, Edge, Firefox, Brave, Opera, Vivaldi, Arc, Chromium, LibreWolf, Waterfox, Floorp, Zen), the page's title decides, so X, Reddit or YouTube in a tab count like their apps on the phone. A fullscreen browser on any other page counts as WATCH.
2. Other **programs** go by their name.
3. Anything else **fullscreen** counts as a game.
4. The desktop, the taskbar and everything else: HOME.

Minecraft is GAME, with his gear. Each mode does what it does on the phone (see [Modes](#modes)), except that on Windows **modes never move him**. On the phone he goes to a spot for some modes (a cloud up top for chat, maps, reading and games, the corner for videos), but the window in front of a computer changes all the time, so there he'd jump around the screen every few seconds. On Windows he stays where he is on the taskbar, and the mode only changes what he does there (popcorn, texting, staying quiet, bopping along, and so on). A trick that needs a cloud (fishing, from **pick a trick...**) puts the cloud just above him, and he comes back down after.

| mode | browser tab titles with | programs |
|---|---|---|
| MUSIC | YouTube Music, Spotify, SoundCloud, Apple Music, Deezer, TIDAL, Bandcamp, Pandora | Spotify, iTunes, Apple Music, foobar2000, MusicBee, AIMP, Winamp, TIDAL, Deezer, Amazon Music, Groove Music, Audacious, Plexamp |
| HIDE | Google Meet, "Meet - ", Zoom Meeting | Zoom, Zoom screen sharing (CptHost), Camera, PowerPoint, the lock screen, the Snipping Tool |
| WATCH | YouTube, Netflix, Twitch, Disney+, Prime Video, Hulu, Crunchyroll, Plex, Paramount+, Peacock, BBC iPlayer, Channel 4, Pluto TV, Tubi, Apple TV, Vimeo, Dailymotion | VLC, MPC-HC, MPC-BE, PotPlayer, Windows Media Player, Media Player, Films & TV, Plex, mpv, Netflix, Disney+, Prime Video, Stremio, Kodi |
| SCROLL | X (" / X", "on X:"), Twitter, Reddit (": r/"), Instagram, TikTok, Facebook, Threads, Bluesky, Tumblr, Pinterest, LinkedIn, 9GAG, Imgur, Lemon8 | |
| CHAT | WhatsApp, Discord, Messenger, Telegram, Gmail, Outlook, Slack, Microsoft Teams | Discord, Slack, Teams, Telegram, WhatsApp, Signal, Outlook (new and classic), Thunderbird, Element, Messenger, Skype, Viber, LINE |
| NAV | Google Maps, Bing Maps, OpenStreetMap, Citymapper | |
| READ | Kindle, Wattpad, Duolingo, Wikipedia, Google Books, AO3 / Archive of Our Own | Kindle, Adobe Reader and Acrobat, SumatraPDF, calibre |
| GAME | | Minecraft, Roblox, Fortnite, League of Legends, Valorant, Counter-Strike 2, Dota 2, GTA V, Rocket League, Overwatch, Genshin Impact, Honkai: Star Rail, Elden Ring, Terraria, Among Us, Rainbow Six, Apex Legends, Fall Guys, Palworld, and anything fullscreen |

Titles are checked in the order of the table, so "YouTube Music" is music, not watching. Add your own in `squib.json` ([desktopApps](#desktopapps)).

### Files

All in `%APPDATA%\Squib`:

- `prefs.txt`: his stats and settings. The first time this version runs, it copies the stats and settings from `memory.txt` (the first Windows Squib's file), which it leaves as it was.
- `squib.json`: the last lines downloaded from GitHub.
- `squib-log.txt`: his log, like the phone's: what he noticed and decided, and why (app modes, clicks, activities, bedtime). The last 1 MB or so; the one before is `squib-log.old.txt`. Programs are logged by name, never whole page titles.

### Command line

| | |
|---|---|
| `Squib.exe` | starts him. If he's already running: brings him back from a pause (or he says hi) |
| `Squib.exe --quit` | closes the running Squib, saving his stats |
| `Squib.exe --find-hotbar` | see [Minecraft on Windows](#minecraft-on-windows) |
| `Squib.exe --find-chatbar` | see [Discord on Windows](#discord-on-windows) |
| `Squib.exe --demo` | goes through talk, feed, a fidget spinner and his menu in the first 30 seconds, for checking how they look |
| `Squib.exe --sim ...` | runs him with a fake clock and no window: see [Building](#building) |
