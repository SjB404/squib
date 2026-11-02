package com.squib.buddy;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.SystemClock;
import android.text.format.DateFormat;
import java.util.Date;

/**
 * The "pause" button in squib's notification. Pausing stops him completely (no overlay at all, so apps
 * that block screens drawn on top of them work again) and leaves a small notification with a "start" button.
 * If "come back on his own" is on, an alarm starts him again after the chosen number of minutes.
 * Any start of SquibService (this alarm, the start button, the app, boot) ends the pause.
 */
public final class Pause extends BroadcastReceiver {
    static final String ACTION_WAKE = "com.squib.buddy.PAUSE_WAKE";
    static final String ACTION_CANCEL = "com.squib.buddy.PAUSE_CANCEL";
    static final int NOTIF_ID = 2;
    /** Choices for the "pause for" setting, in minutes. */
    static final int[] MINUTES = {1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180};
    static final int FLAGS = 201326592; // FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT

    static boolean auto(Context c) {
        return Prefs.get(c).getBoolean("pauseAuto", true);
    }

    static int minutes(Context c) {
        return Prefs.get(c).getInt("pauseMinutes", 5);
    }

    static boolean paused(Context c) {
        return Prefs.get(c).getBoolean("paused", false);
    }

    /** Wall-clock time he comes back, or 0 if he waits for the start button. */
    static long until(Context c) {
        return Prefs.get(c).getLong("pauseUntil", 0L);
    }

    static String clock(Context c, long when) {
        return DateFormat.getTimeFormat(c).format(new Date(when));
    }

    /** Called by the service just before it stops itself. */
    static void begin(Context c) {
        long until = 0L;
        if (auto(c)) {
            long delay = minutes(c) * 60000L;
            until = System.currentTimeMillis() + delay;
            schedule(c, SystemClock.elapsedRealtime() + delay);
        } else {
            cancelAlarm(c);
        }
        Prefs.get(c).edit().putBoolean("paused", true).putLong("pauseUntil", until).apply();
        post(c, until > 0 ? "back at " + clock(c, until) + ", or press start" : "press start to bring him back");
    }

    /** Ends the pause without starting him (the service calls this when it starts, and the "stop" button). */
    static void clear(Context c) {
        cancelAlarm(c);
        SharedPreferences p = Prefs.get(c);
        if (p.getBoolean("paused", false)) {
            p.edit().putBoolean("paused", false).putLong("pauseUntil", 0L).apply();
        }
        ((NotificationManager) c.getSystemService(NotificationManager.class)).cancel(NOTIF_ID);
    }

    static void schedule(Context c, long at) {
        AlarmManager am = (AlarmManager) c.getSystemService(AlarmManager.class);
        PendingIntent pi = wakeIntent(c);
        // An exact alarm is one of the few things Android lets start a foreground service from the background.
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
                return;
            } catch (SecurityException e) {
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
    }

    static void cancelAlarm(Context c) {
        ((AlarmManager) c.getSystemService(AlarmManager.class)).cancel(wakeIntent(c));
    }

    static PendingIntent wakeIntent(Context c) {
        return PendingIntent.getBroadcast(c, 10, new Intent(c, Pause.class).setAction(ACTION_WAKE), FLAGS);
    }

    static void post(Context c, String text) {
        PendingIntent start = PendingIntent.getForegroundService(c, 11, new Intent(c, SquibService.class), FLAGS);
        PendingIntent stop = PendingIntent.getBroadcast(c, 12, new Intent(c, Pause.class).setAction(ACTION_CANCEL), FLAGS);
        Notification n = new Notification.Builder(c, SquibService.CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("squib is paused")
                .setContentText(text)
                .setContentIntent(PendingIntent.getActivity(c, 13, new Intent(c, MainActivity.class), FLAGS))
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_notif), "start", start).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_notif), "stop", stop).build())
                .build();
        ((NotificationManager) c.getSystemService(NotificationManager.class)).notify(NOTIF_ID, n);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (ACTION_CANCEL.equals(action)) {
            clear(c);
        } else if (ACTION_WAKE.equals(action) && paused(c)) {
            if (SquibService.running) {
                clear(c);
                return;
            }
            try {
                c.startForegroundService(new Intent(c, SquibService.class));
            } catch (RuntimeException e) {
                // Android refused to start him from the background. Leave the start button.
                Prefs.get(c).edit().putLong("pauseUntil", 0L).apply();
                post(c, "couldn't come back on his own, press start");
            }
        }
    }
}
