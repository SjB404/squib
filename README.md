# squib

Voice lines and app lists for squib, the pixel buddy that lives on your phone.

The app downloads `squib.json` from this repo when it starts and every 6 hours after that. It keeps a copy on the phone, so it still works offline. To check right away, open squib's settings and tap "check now". GitHub can take up to about 5 minutes to serve a new version of the file.

## squib.json

```json
{
  "version": 1,
  "lines": { "BOOP": ["boop!", "hey!"], ... },
  "apps":  { "WATCH": ["com.netflix.mediaclient", ...], ... }
}
```

### settings

| setting | default | what it does |
|---|---|---|
| `scrollCheckMinutes` | 15 | minutes between scroll check-ins |
| `activityGapMin` / `activityGapMax` | 25 / 70 | seconds between his activities (a random time in between) |

### lines

Each group **replaces** the built-in lines for that group. A group that's missing or empty keeps the built-in lines.

| group | when he says it |
|---|---|
| THOUGHTS, MUTTERS, JOKES, FACTS, STATS | random chatter |
| MORNING, AFTERNOON, EVENING, NIGHT | random chatter, depending on time of day |
| OPENERS | sometimes stuck in front of chatter ("hm. ", "ok so, ") |
| BOOP | when tapped |
| SPAM | when tapped 5 times fast |
| GRABBED | when picked up |
| PET | when petted |
| WAKE | when you turn the screen back on after a while |
| FED, HUNGRY | after eating, and when he hasn't eaten in hours |
| BREAK | after 50 minutes of phone use |
| LATE | 11pm to 1am bedtime nagging |
| LATE_NIGHT | 1am to 5am bedtime nagging |
| WATCH_ENTER, WATCH, WATCH_LONG | video apps (WATCH_LONG: after 2 hours) |
| MUSIC_ENTER, MUSIC | music apps |
| SCROLL, SCROLL_LONG | scrolling apps. SCROLL_LONG is said every `scrollCheckMinutes` (15) of scrolling, going through the lines in order and starting over at the end. `{mins}` becomes how long you've been scrolling |
| CHAT_ENTER | chat apps |
| GAME_ENTER, GAME, MINECRAFT_ENTER | games |
| NAV_ENTER, NAV | map apps |
| READ_ENTER, READ | reading apps |
| MINECRAFT | in Minecraft, when he switches items or starts mining |
| BALL, PLANE, BUBBLES, BOOK, YOYO, DANCE, WAVE, BUTTERFLY, NAP, TEA | when he starts that activity (not every time) |

These get filled in when he says the line:
`{time}` the current time, `{mins}` minutes scrolled (SCROLL_LONG only), `{day}` how many days he's lived on your phone, `{fed}` shrimp eaten, `{boops}`, `{pets}`.

Stick to plain keyboard characters. The pixel font has no emoji or accented letters, so those show up as `?`.

### minecraft

Apps where he gets his Minecraft gear. Anything listed here also counts as a game.

### apps

Adds apps to a type. These get added on top of the built-in list. Types: `WATCH`, `MUSIC`, `SCROLL`, `CHAT`, `GAME`, `NAV`, `READ`, `HIDE`. To find an app's name, open its Play Store page: it's the part after `id=` in the link.

## Modes

He switches mode based on the app you have open. This needs usage access turned on, and the "app modes" switch in his settings. Switching into a mode (other than HIDE) has a 30% chance of a `<MODE>_ENTER` line, at most once per 10 minutes per mode.

Bedtime nagging and the 50-minute break reminder happen in every mode except HIDE.

### everything else (HOME)

Your home screen and any app not listed below. Walks along the bottom of the screen, wanders, hops, looks around.

- Talks every 35 seconds to 3 minutes: thoughts, mutters, jokes, facts, stats, time-of-day lines, and HUNGRY when he hasn't eaten in 2 hours.

### watch (WATCH)

Walks to the bottom-right corner and sits down with popcorn. Kernels pop out now and then.

- Says a WATCH line every 3 to 6 minutes.
- After 2 hours: one WATCH_LONG line.
- After 90 minutes he may fall asleep. Tap to wake him.

**Apps:** Netflix `com.netflix.mediaclient`, YouTube `com.google.android.youtube`, YouTube Kids `com.google.android.apps.youtube.kids`, Disney+ `com.disney.disneyplus`, Prime Video `com.amazon.avod.thirdpartyclient`, Max `com.wbd.stream`, HBO `com.hbo.hbonow`, Hulu `com.hulu.plus`, Twitch `tv.twitch.android.app`, Crunchyroll `com.crunchyroll.crunchyroid`, Plex `com.plexapp.android`, VLC `org.videolan.vlc`, Paramount+ `com.cbs.app`, Peacock `com.peacocktv.peacockandroid`, Google TV `com.google.android.videos`, MX Player `com.mxtech.videoplayer.ad`, Apple TV `com.apple.atve.androidtv.appletv`, BBC iPlayer `com.bbc.iplayer.android`, Channel 4 `com.channel4.ondemand`, Pluto TV `tv.pluto.android`, Tubi `com.tubitv`, Kick `com.kick.mobile`

**Also:** apps not on any list that Android files under: Android category video.

### music (MUSIC)

Walks along the bottom. When standing still he bops to a beat and music notes float up.

- Says a MUSIC line every 4 to 8 minutes.

**Apps:** Spotify `com.spotify.music`, YouTube Music `com.google.android.apps.youtube.music`, Apple Music `com.apple.android.music`, SoundCloud `com.soundcloud.android`, Deezer `deezer.android.app`, Tidal `com.aspiro.tidal`, Amazon Music `com.amazon.mp3`, Pandora `com.pandora.android`, Shazam `com.shazam.android`, Audible `com.audible.application`, Podcast Addict `com.bambuna.podcastaddict`, Pocket Casts `au.com.shiftyjelly.pocketcasts`

**Also:** apps not on any list that Android files under: Android category audio.

### scrolling (SCROLL)

Walks along the bottom like normal.

- Talks every 35 seconds to 3 minutes: 30% of the time a SCROLL line, otherwise normal chatter.
- Every 15 minutes of scrolling (`scrollCheckMinutes`), no limit: a SCROLL_LONG check-in (needs break reminders on). The count resets after 5 minutes away from scrolling apps.

**Apps:** TikTok `com.zhiliaoapp.musically`, TikTok (Asia) `com.ss.android.ugc.trill`, Instagram `com.instagram.android`, Threads `com.instagram.barcelona`, Snapchat `com.snapchat.android`, Reddit `com.reddit.frontpage`, X `com.twitter.android`, Facebook `com.facebook.katana`, Tumblr `com.tumblr`, Pinterest `com.pinterest`, BeReal `com.bereal.ft`, LinkedIn `com.linkedin.android`, VSCO `com.vsco.cam`, Bluesky `bsky.app`, Lemon8 `com.lemon8.android`

**Also:** apps not on any list that Android files under: Android category social.

### chat (CHAT)

Sits on a cloud in the top-right corner, out of the way.

- Never chatters on his own here.

**Apps:** WhatsApp `com.whatsapp`, WhatsApp Business `com.whatsapp.w4b`, Discord `com.discord`, Telegram `org.telegram.messenger`, Messenger `com.facebook.orca`, Signal `org.thoughtcrime.securesms`, Google Messages `com.google.android.apps.messaging`, Samsung Messages `com.samsung.android.messaging`, Slack `com.Slack`, Teams `com.microsoft.teams`, Viber `com.viber.voip`, LINE `jp.naver.line.android`, KakaoTalk `com.kakao.talk`, Gmail `com.google.android.gm`, Outlook `com.microsoft.office.outlook`

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

Every 25 to 70 seconds (`activityGapMin` / `activityGapMax`), when he's standing around, he picks something to do. About half the time he says a line from that activity's group when he starts.

| activity | what happens | where |
|---|---|---|
| BALL | a ball drops in, he chases it and kicks it around 3 to 6 times | bottom of the screen only |
| PLANE | winds up and throws a paper plane that glides off screen. His eyes follow it | anywhere |
| BUBBLES | blows bubbles with a wand. They float up and fade | anywhere |
| BOOK | holds up a book and reads, turning pages now and then | anywhere, more often in reading apps |
| YOYO | plays with a yo-yo | anywhere |
| DANCE | dances: arms swapping, hopping on the beat, music notes | anywhere, much more often in music apps |
| WAVE | waves at you | anywhere |
| BUTTERFLY | a butterfly flies in and circles his head. He hops trying to catch it, then it flies away | anywhere |
| NAP | takes a 20 to 45 second nap. Tap him to wake him | anywhere |
| TEA | drinks a mug of tea. Steam rises, and he takes sips | anywhere |

He doesn't do activities while watching videos (he has popcorn), in hidden apps, or in Minecraft. Picking him up, petting him, or feeding him stops whatever he's doing.

## Minecraft

In Minecraft (and the launchers in the `minecraft` list) he gets diamond gear. Only diamond, never netherite.

- Holds one of: diamond sword, diamond pickaxe, diamond axe, diamond shovel, or a diamond. He switches to a different one every 20 to 50 seconds.
- Wears a diamond helmet most of the time (decided each time you open Minecraft).
- Every few seconds he does something with it:
  - **swing:** raises the item and strikes, with a little swoosh
  - **mine:** chops repeatedly, with bits of dirt and stone flying (pickaxe, axe, shovel)
  - **show off:** holds it up high, with sparkles (always for the diamond)
- Says MINECRAFT lines sometimes when switching items or starting to mine.
