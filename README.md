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
| SCROLL, SCROLL_LONG | scrolling apps. SCROLL_LONG needs at least 3 lines: they're said after 20, 40 and 60 minutes, in order |
| CHAT_ENTER | chat apps |
| GAME_ENTER, GAME, MINECRAFT_ENTER | games |
| NAV_ENTER, NAV | map apps |
| READ_ENTER, READ | reading apps |

These get filled in when he says the line:
`{time}` the current time, `{day}` how many days he's lived on your phone, `{fed}` shrimp eaten, `{boops}`, `{pets}`.

Stick to plain keyboard characters. The pixel font has no emoji or accented letters, so those show up as `?`.

### apps

Adds apps to a type. These get added on top of the built-in list. Types: `WATCH`, `MUSIC`, `SCROLL`, `CHAT`, `GAME`, `NAV`, `READ`, `HIDE`. To find an app's name, open its Play Store page: it's the part after `id=` in the link.
