// App modes on Windows. The phone goes by the app in front; on a computer the same things happen
// in different places (X and YouTube are usually browser tabs, Spotify and Discord are programs),
// so this goes by the window in front: its program, and for browsers the page title.
// squib.json's "desktopApps" adds to these lists.

using System;
using System.Collections.Generic;
using Squib.Engine;

namespace Squib
{
    static class Modes
    {
        public static readonly string[] Browsers = {
            "chrome", "msedge", "firefox", "brave", "opera", "vivaldi", "arc", "chromium", "librewolf", "waterfox", "floorp", "zen", "iexplore",
        };

        // Program names (without .exe), any case.
        static readonly Dictionary<string, Apps.Kind> programs = new Dictionary<string, Apps.Kind>(StringComparer.OrdinalIgnoreCase);
        // Bits of a browser tab's title, checked in order (so "YouTube Music" comes before "YouTube").
        static readonly List<KeyValuePair<string, Apps.Kind>> titles = new List<KeyValuePair<string, Apps.Kind>>();

        static Modes()
        {
            // titles: the most specific first
            Title(Apps.Kind.MUSIC, "YouTube Music", "Spotify", "SoundCloud", "Apple Music", "Deezer", "TIDAL", "Bandcamp", "Pandora");
            Title(Apps.Kind.HIDE, "Google Meet", "Meet - ", "Zoom Meeting");
            Title(Apps.Kind.WATCH, "YouTube", "Netflix", "Twitch", "Disney+", "Prime Video", "Hulu", "Crunchyroll", "Plex",
                  "Paramount+", "Peacock", "BBC iPlayer", "Channel 4", "Pluto TV", "Tubi", "Apple TV", "Vimeo", "Dailymotion");
            Title(Apps.Kind.SCROLL, " / X", "on X:", "Twitter", "Reddit", ": r/", "Instagram", "TikTok", "Facebook", "Threads", "Bluesky",
                  "Tumblr", "Pinterest", "LinkedIn", "9GAG", "Imgur", "Lemon8");
            Title(Apps.Kind.CHAT, "WhatsApp", "Discord", "Messenger", "Telegram", "Gmail", "Outlook", "Slack", "Microsoft Teams");
            Title(Apps.Kind.NAV, "Google Maps", "Bing Maps", "OpenStreetMap", "Citymapper");
            Title(Apps.Kind.READ, "Kindle", "Wattpad", "Duolingo", "Wikipedia", "Google Books", "AO3", "Archive of Our Own");

            Program(Apps.Kind.WATCH, "vlc", "mpc-hc", "mpc-hc64", "mpc-be", "mpc-be64", "PotPlayerMini", "PotPlayerMini64", "wmplayer",
                    "Microsoft.Media.Player", "Video.UI", "Plex", "Plex HTPC", "mpv", "Netflix", "Disney+", "PrimeVideo", "stremio", "kodi");
            Program(Apps.Kind.MUSIC, "Spotify", "iTunes", "AppleMusic", "foobar2000", "MusicBee", "AIMP", "winamp", "TIDAL", "Deezer",
                    "Amazon Music", "Music.UI", "Audacious", "Plexamp");
            Program(Apps.Kind.CHAT, "Discord", "DiscordPTB", "DiscordCanary", "slack", "Teams", "ms-teams", "Telegram", "WhatsApp", "Signal",
                    "olk", "OUTLOOK", "thunderbird", "Element", "Messenger", "Skype", "Viber", "LINE");
            Program(Apps.Kind.GAME, "RobloxPlayerBeta", "FortniteClient-Win64-Shipping", "League of Legends", "VALORANT-Win64-Shipping",
                    "cs2", "dota2", "GTA5", "RocketLeague", "Overwatch", "GenshinImpact", "StarRail", "eldenring", "Terraria",
                    "Among Us", "Minecraft.Windows", "Rainbow6", "r5apex", "FallGuys_client_game", "Palworld-Win64-Shipping");
            Program(Apps.Kind.READ, "Kindle", "AcroRd32", "Acrobat", "SumatraPDF", "calibre", "ebook-viewer");
            Program(Apps.Kind.HIDE, "Zoom", "CptHost", "WindowsCamera", "POWERPNT", "LockApp", "ScreenClippingHost", "SnippingTool");
        }

        static void Title(Apps.Kind kind, params string[] bits) { foreach (string b in bits) titles.Add(new KeyValuePair<string, Apps.Kind>(b, kind)); }
        static void Program(Apps.Kind kind, params string[] names) { foreach (string n in names) programs[n] = kind; }

        // From squib.json "desktopApps": { "WATCH": ["vlc", "title:Netflix"], ... }
        public static void Add(Apps.Kind kind, string rule)
        {
            if (rule.StartsWith("title:", StringComparison.OrdinalIgnoreCase)) titles.Insert(0, new KeyValuePair<string, Apps.Kind>(rule.Substring(6), kind));
            else programs[rule.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) ? rule.Substring(0, rule.Length - 4) : rule] = kind;
        }

        public static bool IsBrowser(string program)
        {
            foreach (string b in Browsers) if (string.Equals(b, program, StringComparison.OrdinalIgnoreCase)) return true;
            return false;
        }

        // The id SquibView.setApp gets: "KIND|program" (see Apps.kindOf).
        public static string Classify(string program, string title, bool fullscreen)
        {
            if (IsBrowser(program))
            {
                foreach (var t in titles)
                    if (title.IndexOf(t.Key, StringComparison.OrdinalIgnoreCase) >= 0) return t.Value + "|" + program + "|" + t.Key;
                return (fullscreen ? Apps.Kind.WATCH : Apps.Kind.HOME) + "|" + program;
            }
            Apps.Kind k;
            if (programs.TryGetValue(program, out k)) return k + "|" + program;
            if (fullscreen) return Apps.Kind.GAME + "|" + program;
            return Apps.Kind.HOME + "|" + program;
        }
    }
}
