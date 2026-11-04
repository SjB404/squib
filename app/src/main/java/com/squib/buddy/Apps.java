package com.squib.buddy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.HashMap;
import java.util.Map;

final class Apps {
    private static final Map<String, Kind> KNOWN = new HashMap<String, Kind>();
    static final String SUMMARY = "watch: netflix, youtube, disney+, prime video, max, hulu, twitch, crunchyroll, plex, vlc and other video apps. sits in the corner with popcorn.\n\n"
            + "music: spotify, youtube music, apple music, soundcloud, deezer, tidal, podcasts. bops along, dances and plays guitar more. when music is actually playing he puts headphones on and listens along, and keeps them on in other apps while it plays (they come off for video).\n\n"
            + "scrolling: tiktok, instagram, snapchat, reddit, x, facebook, pinterest, threads. hangs out, and checks in every 15 minutes.\n\n"
            + "chat: whatsapp, discord, telegram, messenger, signal, beeper, messages, slack, teams, gmail. sits on a cloud near the top and texts on his own phone, message bubbles going back and forth over his head.\n\n"
            + "games: minecraft, roblox, clash royale, brawl stars, genshin, pubg, cod, among us, pokemon go and any other game. perches in the top corner and cheers. in minecraft he gets diamond gear.\n\n"
            + "maps: google maps, waze, uber. perches up top.\n\n"
            + "reading: kindle, play books, wattpad, duolingo. perches up top, very quiet.\n\n"
            + "hides: camera, phone calls, zoom.\n\n"
            + "everything else: walks along the bottom, chats, and gets up to things.";
    private static String home;

    enum Kind {
        HOME,
        WATCH,
        MUSIC,
        SCROLL,
        CHAT,
        GAME,
        NAV,
        READ,
        HIDE
    }

    static {
        add(Kind.WATCH, "com.netflix.mediaclient", "com.google.android.youtube", "com.google.android.apps.youtube.kids", "com.disney.disneyplus", "com.amazon.avod.thirdpartyclient", "com.wbd.stream", "com.hbo.hbonow", "com.hulu.plus", "tv.twitch.android.app", "com.crunchyroll.crunchyroid", "com.plexapp.android", "org.videolan.vlc", "com.cbs.app", "com.peacocktv.peacockandroid", "com.google.android.videos", "com.mxtech.videoplayer.ad", "com.apple.atve.androidtv.appletv", "com.bbc.iplayer.android", "com.channel4.ondemand", "tv.pluto.android", "com.tubitv", "com.kick.mobile");
        add(Kind.MUSIC, "com.spotify.music", "com.google.android.apps.youtube.music", "com.apple.android.music", "com.soundcloud.android", "deezer.android.app", "com.aspiro.tidal", "com.amazon.mp3", "com.pandora.android", "com.shazam.android", "com.audible.application", "com.bambuna.podcastaddict", "au.com.shiftyjelly.pocketcasts");
        add(Kind.SCROLL, "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.instagram.android", "com.instagram.barcelona", "com.snapchat.android", "com.reddit.frontpage", "com.twitter.android", "com.facebook.katana", "com.tumblr", "com.pinterest", "com.bereal.ft", "com.linkedin.android", "com.vsco.cam", "bsky.app", "com.lemon8.android");
        add(Kind.CHAT, "com.whatsapp", "com.whatsapp.w4b", "com.discord", "org.telegram.messenger", "com.facebook.orca", "org.thoughtcrime.securesms", "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.Slack", "com.microsoft.teams", "com.viber.voip", "jp.naver.line.android", "com.kakao.talk", "com.google.android.gm", "com.microsoft.office.outlook", "com.beeper.android", "com.beeper.chat");
        add(Kind.GAME, "com.mojang.minecraftpe", "com.roblox.client", "com.supercell.clashroyale", "com.supercell.brawlstars", "com.supercell.clashofclans", "com.miHoYo.GenshinImpact", "com.HoYoverse.hkrpgoversea", "com.tencent.ig", "com.activision.callofduty.shooter", "com.innersloth.spacemafia", "com.kiloo.subwaysurf", "com.king.candycrushsaga", "com.nianticlabs.pokemongo", "com.chess", "com.dts.freefireth", "com.ea.gp.fifamobile", "com.mobile.legends", "com.epicgames.fortnite", "com.YoStarEN.Arknights");
        add(Kind.NAV, "com.google.android.apps.maps", "com.waze", "com.citymapper.app.release", "com.ubercab", "me.lyft.android");
        add(Kind.READ, "com.amazon.kindle", "com.google.android.apps.books", "com.wattpad.wattpad", "com.medium.reader", "com.duolingo", "com.google.android.apps.magazines");
        add(Kind.HIDE, "com.google.android.GoogleCamera", "com.sec.android.app.camera", "com.android.camera", "com.android.camera2", "com.oneplus.camera", "com.motorola.camera3", "com.google.android.dialer", "com.samsung.android.incallui", "com.android.incallui", "com.android.server.telecom", "us.zoom.videomeetings");
    }

    static void put(String pkg, Kind kind) {
        KNOWN.put(pkg, kind);
    }

    private static void add(Kind kind, String... strArr) {
        for (String str : strArr) {
            KNOWN.put(str, kind);
        }
    }

    static Kind kindOf(Context context, String str) {
        Kind kind = KNOWN.get(str);
        if (kind != null) {
            return kind;
        }
        if (str.contains("camera") || str.contains("incallui")) {
            return Kind.HIDE;
        }
        if (str.equals(homePackage(context))) {
            return Kind.HOME;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(str, 0);
            switch (applicationInfo.category) {
                case 0:
                    return Kind.GAME;
                case 1:
                    return Kind.MUSIC;
                case 2:
                    return Kind.WATCH;
                case 3:
                default:
                    if ((applicationInfo.flags & 33554432) != 0) {
                        return Kind.GAME;
                    }
                    break;
                case 4:
                    return Kind.SCROLL;
                case 5:
                    return Kind.READ;
                case 6:
                    return Kind.NAV;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return Kind.HOME;
    }

    private static String homePackage(Context context) {
        if (home == null) {
            ResolveInfo resolveActivity = context.getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 65536);
            home = (resolveActivity == null || resolveActivity.activityInfo == null) ? "" : resolveActivity.activityInfo.packageName;
        }
        return home;
    }

    private Apps() {
    }
}
